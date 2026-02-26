/**
 * Q-Feed interview load test (practice + real cycle)
 *
 * Required data:
 * - loadtest/generated/user-access-token-list.csv
 *
 * Run example:
 * k6 run loadtest/test.js \
 *   --env BASE_URL=http://localhost:8080 \
 *   --env USER_TOKEN_CSV=generated/user-access-token-list.csv \
 *   --env DURATION=5m \
 *   --env PRACTICE_RATE=6 \
 *   --env REAL_RATE=2
 */

import http from "k6/http";
import exec from "k6/execution";
import { SharedArray } from "k6/data";
import { check, fail, sleep } from "k6";
import { Counter, Rate, Trend } from "k6/metrics";

const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/+$/, "");
const USER_TOKEN_CSV = __ENV.USER_TOKEN_CSV || "";

const DURATION = __ENV.DURATION || "5m";

const PRACTICE_RATE = Number(__ENV.PRACTICE_RATE || 6);
const PRACTICE_PRE_ALLOCATED_VUS = Number(__ENV.PRACTICE_PRE_ALLOCATED_VUS || 20);
const PRACTICE_MAX_VUS = Number(__ENV.PRACTICE_MAX_VUS || 60);

const REAL_RATE = Number(__ENV.REAL_RATE || 2);
const REAL_PRE_ALLOCATED_VUS = Number(__ENV.REAL_PRE_ALLOCATED_VUS || 10);
const REAL_MAX_VUS = Number(__ENV.REAL_MAX_VUS || 40);

const PRACTICE_USER_SPAN = PRACTICE_RATE > 0 ? Math.max(1, PRACTICE_MAX_VUS) : 0;
const REAL_USER_SPAN = REAL_RATE > 0 ? Math.max(1, REAL_MAX_VUS) : 0;
const REAL_USER_OFFSET = PRACTICE_USER_SPAN;
const REQUIRED_USER_COUNT = PRACTICE_USER_SPAN + REAL_USER_SPAN;

const TOKEN_MARGIN_SEC = Number(__ENV.TOKEN_MARGIN_SEC || 30);
const FEEDBACK_POLL_MAX = Number(__ENV.FEEDBACK_POLL_MAX || 15);
const FEEDBACK_POLL_INTERVAL_SEC = Number(__ENV.FEEDBACK_POLL_INTERVAL_SEC || 0.3);
const REAL_TURN_LIMIT = Number(__ENV.REAL_TURN_LIMIT || 8);
const REAL_THINK_SEC = Number(__ENV.REAL_THINK_SEC || 0.2);
const ANSWER_MIN_TEXT = "핵심 개념을 정의하고 원리와 예시를 함께 설명하겠습니다.";

const DEFAULT_ACCESS_EXPIRES_SEC = 600;
const SCENARIO_PRACTICE = "practice_cycle";
const SCENARIO_REAL = "real_cycle";

const practiceCycleFailed = new Rate("practice_cycle_failed");
const realCycleFailed = new Rate("real_cycle_failed");
const practiceCycleDuration = new Trend("practice_cycle_duration_ms");
const realCycleDuration = new Trend("real_cycle_duration_ms");
const tokenRefreshTotal = new Counter("token_refresh_total");
const authRetryTotal = new Counter("auth_retry_total");

const resolvedTokenCsvPath = resolveTokenCsvPath(USER_TOKEN_CSV);
const userTokenRows = new SharedArray("user-token-rows", () => {
  const rows = parseCsv(open(resolvedTokenCsvPath));
  if (rows.length === 0) {
    throw new Error(`token csv is empty: ${resolvedTokenCsvPath}`);
  }
  return rows;
});

const scenarios = {};
if (PRACTICE_RATE > 0) {
  scenarios[SCENARIO_PRACTICE] = {
    executor: "constant-arrival-rate",
    exec: "practiceCycle",
    rate: PRACTICE_RATE,
    timeUnit: "1s",
    duration: DURATION,
    preAllocatedVUs: PRACTICE_PRE_ALLOCATED_VUS,
    maxVUs: PRACTICE_MAX_VUS,
  };
}
if (REAL_RATE > 0) {
  scenarios[SCENARIO_REAL] = {
    executor: "constant-arrival-rate",
    exec: "realCycle",
    rate: REAL_RATE,
    timeUnit: "1s",
    duration: DURATION,
    preAllocatedVUs: REAL_PRE_ALLOCATED_VUS,
    maxVUs: REAL_MAX_VUS,
  };
}

export const options = {
  scenarios,
  thresholds: {
    http_req_failed: ["rate<0.05"],
    practice_cycle_failed: ["rate<0.10"],
    real_cycle_failed: ["rate<0.20"],
    practice_cycle_duration_ms: ["p(95)<5000"],
    real_cycle_duration_ms: ["p(95)<12000"],
  },
};

const vuStateByScenario = {};

export function setup() {
  if (userTokenRows.length < REQUIRED_USER_COUNT) {
    throw new Error(
      `not enough users in csv: rows=${userTokenRows.length}, required=${REQUIRED_USER_COUNT} (practiceSpan + realSpan)`
    );
  }

  console.log(
    `[setup] baseUrl=${BASE_URL}, duration=${DURATION}, tokenCsv=${resolvedTokenCsvPath}, csvRows=${userTokenRows.length}, practiceRate=${PRACTICE_RATE}, realRate=${REAL_RATE}`
  );
}

export function practiceCycle() {
  const startedAt = Date.now();
  let success = true;

  try {
    const state = getOrInitState(SCENARIO_PRACTICE);
    const questionType = exec.vu.iterationInScenario % 2 === 0 ? "CS" : "SYSTEM_DESIGN";

    const qRes = requestWithAuth({
      state,
      method: "GET",
      path: `/api/questions?type=${encodeURIComponent(questionType)}&size=10`,
      tags: { flow: SCENARIO_PRACTICE, step: "questions" },
    });
    if (qRes.status !== 200) {
      success = false;
      return;
    }
    const qJson = parseJson(qRes);
    const question = pickQuestion(qJson);
    if (!question) {
      success = false;
      return;
    }

    const createRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/interview/sessions",
      body: { interviewType: "PRACTICE_INTERVIEW", questionType },
      tags: { flow: SCENARIO_PRACTICE, step: "create_session" },
    });
    if (createRes.status !== 201) {
      success = false;
      return;
    }
    const createJson = parseJson(createRes);
    const sessionId = pickSessionId(createJson);
    if (!sessionId) {
      success = false;
      return;
    }

    const submitRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/answers/practice",
      body: {
        sessionId,
        questionId: question.questionId,
        answerText: `${ANSWER_MIN_TEXT} [practice][q=${question.questionId}]`,
      },
      tags: { flow: SCENARIO_PRACTICE, step: "submit_answer" },
    });
    if (submitRes.status !== 201) {
      success = false;
      return;
    }

    const finalReqRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/interview/sessions/feedback/request",
      body: { sessionId },
      tags: { flow: SCENARIO_PRACTICE, step: "request_feedback" },
    });
    if (finalReqRes.status !== 201) {
      success = false;
      return;
    }

    if (!pollCompletedFeedback(state, sessionId, SCENARIO_PRACTICE)) {
      success = false;
    }
  } catch (e) {
    success = false;
    console.error(`[${SCENARIO_PRACTICE}] unexpected error: ${String(e)}`);
  } finally {
    practiceCycleFailed.add(!success);
    practiceCycleDuration.add(Date.now() - startedAt);
  }
}

export function realCycle() {
  const startedAt = Date.now();
  let success = true;

  try {
    const state = getOrInitState(SCENARIO_REAL);
    const questionType = exec.vu.iterationInScenario % 2 === 0 ? "CS" : "SYSTEM_DESIGN";

    const createRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/interview/sessions",
      body: { interviewType: "REAL_INTERVIEW", questionType },
      tags: { flow: SCENARIO_REAL, step: "create_session" },
    });
    if (createRes.status !== 201) {
      success = false;
      return;
    }
    const createJson = parseJson(createRes);
    const sessionId = pickSessionId(createJson);
    let currentQuestion = pickQuestionText(createJson);
    if (!sessionId || !currentQuestion) {
      success = false;
      return;
    }

    let isFinal = false;
    for (let turn = 0; turn < REAL_TURN_LIMIT; turn += 1) {
      const submitRes = requestWithAuth({
        state,
        method: "POST",
        path: "/api/answers/real",
        body: {
          session_id: sessionId,
          answer_text: `${ANSWER_MIN_TEXT} [real][turn=${turn + 1}]`,
          question_type: questionType,
          question: currentQuestion,
        },
        tags: { flow: SCENARIO_REAL, step: "submit_turn" },
      });
      if (submitRes.status !== 201) {
        success = false;
        return;
      }

      const submitJson = parseJson(submitRes);
      const data = submitJson.data || {};
      isFinal = Boolean(data.is_final ?? data.isFinal);
      if (isFinal) {
        break;
      }

      const nextQuestion =
        (data.next_question && data.next_question.content) ||
        (data.nextQuestion && data.nextQuestion.content);

      if (nextQuestion && nextQuestion.length > 0) {
        currentQuestion = nextQuestion;
      } else if (data.bad_case_feedback || data.badCaseFeedback) {
        // bad-case 응답은 다음 질문이 null일 수 있으므로 현재 질문 유지 후 재답변
      } else {
        success = false;
        return;
      }

      sleep(REAL_THINK_SEC);
    }

    if (!isFinal) {
      success = false;
      return;
    }

    const finalReqRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/interview/sessions/feedback/request",
      body: { sessionId },
      tags: { flow: SCENARIO_REAL, step: "request_feedback" },
    });
    if (finalReqRes.status !== 201) {
      success = false;
      return;
    }

    if (!pollCompletedFeedback(state, sessionId, SCENARIO_REAL)) {
      success = false;
    }
  } catch (e) {
    success = false;
    console.error(`[${SCENARIO_REAL}] unexpected error: ${String(e)}`);
  } finally {
    realCycleFailed.add(!success);
    realCycleDuration.add(Date.now() - startedAt);
  }
}

export function teardown() {
  console.log("[teardown] interview load test finished");
}

function getOrInitState(scenarioName) {
  if (vuStateByScenario[scenarioName]) {
    return vuStateByScenario[scenarioName];
  }

  const vuId = resolveVuId();
  let userIndex = resolveUserIndex(scenarioName, vuId);
  if (!Number.isInteger(userIndex)) {
    userIndex = resolveFallbackUserIndex(scenarioName);
  }

  if (!Number.isInteger(userIndex) || userIndex < 0 || userIndex >= userTokenRows.length) {
    fail(
      `user index out of range. scenario=${scenarioName}, userIndex=${userIndex}, vuId=${vuId}, tokenRows=${userTokenRows.length}`
    );
  }

  const user = userTokenRows[userIndex];
  if (!user || !user.user_id) {
    fail(
      `user row missing. scenario=${scenarioName}, userIndex=${userIndex}, vuId=${vuId}, tokenRows=${userTokenRows.length}`
    );
  }
  vuStateByScenario[scenarioName] = {
    userId: Number(user.user_id),
    accessToken: user.access_token,
    refreshToken: user.refresh_token,
    accessExpiresAtMs: parseSqlTimestamp(user.access_expires_at),
  };
  return vuStateByScenario[scenarioName];
}

function resolveUserIndex(scenarioName, vuIdInScenario) {
  const vuId = Number(vuIdInScenario);
  if (!Number.isInteger(vuId) || vuId <= 0) {
    return Number.NaN;
  }

  if (scenarioName === SCENARIO_PRACTICE) {
    const span = PRACTICE_USER_SPAN || Math.max(1, userTokenRows.length);
    return (vuId - 1) % span;
  }
  if (scenarioName === SCENARIO_REAL) {
    const span = REAL_USER_SPAN || Math.max(1, userTokenRows.length);
    return REAL_USER_OFFSET + ((vuId - 1) % span);
  }
  return (vuId - 1) % Math.max(1, userTokenRows.length);
}

function resolveVuId() {
  const candidates = [
    exec.vu?.idInScenario,
    exec.vu?.idInTest,
    exec.vu?.idInInstance,
  ];
  for (let i = 0; i < candidates.length; i += 1) {
    const value = Number(candidates[i]);
    if (Number.isInteger(value) && value > 0) {
      return value;
    }
  }
  return null;
}

function resolveFallbackUserIndex(scenarioName) {
  const iterationCandidates = [
    exec.scenario?.iterationInTest,
    exec.scenario?.iterationInInstance,
    exec.vu?.iterationInScenario,
    exec.vu?.iterationInInstance,
  ];

  let iteration = 0;
  for (let i = 0; i < iterationCandidates.length; i += 1) {
    const value = Number(iterationCandidates[i]);
    if (Number.isFinite(value) && value >= 0) {
      iteration = Math.floor(value);
      break;
    }
  }

  if (scenarioName === SCENARIO_REAL) {
    const span = REAL_USER_SPAN || Math.max(1, userTokenRows.length);
    return REAL_USER_OFFSET + (iteration % span);
  }

  const span = PRACTICE_USER_SPAN || Math.max(1, userTokenRows.length);
  return iteration % span;
}

function requestWithAuth({ state, method, path, body, tags }) {
  if (!ensureAccessToken(state, tags)) {
    return fakeUnauthorizedResponse();
  }

  let response = doRequest(method, path, state, body, tags);
  if (response.status !== 401) {
    return response;
  }

  authRetryTotal.add(1);
  if (!refreshAccessToken(state, tags)) {
    return response;
  }
  response = doRequest(method, path, state, body, tags);
  return response;
}

function ensureAccessToken(state, tags) {
  const now = Date.now();
  const marginMs = TOKEN_MARGIN_SEC * 1000;
  if (state.accessToken && state.accessExpiresAtMs > now + marginMs) {
    return true;
  }
  return refreshAccessToken(state, tags);
}

function refreshAccessToken(state, tags) {
  if (!state.refreshToken) {
    return false;
  }

  const res = http.post(`${BASE_URL}/api/auth/tokens`, null, {
    headers: {
      Cookie: `refreshToken=${state.refreshToken}`,
      "Content-Type": "application/json",
    },
    tags: Object.assign({ endpoint: "auth_refresh" }, tags || {}),
  });

  const ok = check(res, {
    "refresh status=200": (r) => r.status === 200,
    "refresh has auth header": (r) => extractAccessTokenFromHeaders(r.headers) !== null,
  });
  if (!ok) {
    return false;
  }

  const body = parseJson(res);
  const expiresIn = Number(body?.data?.expiresIn || DEFAULT_ACCESS_EXPIRES_SEC);
  const refreshedAccessToken = extractAccessTokenFromHeaders(res.headers);
  if (!refreshedAccessToken) {
    return false;
  }

  const refreshedCookie = extractRefreshCookie(res);
  if (refreshedCookie) {
    state.refreshToken = refreshedCookie;
  }
  state.accessToken = refreshedAccessToken;
  state.accessExpiresAtMs = Date.now() + expiresIn * 1000;
  tokenRefreshTotal.add(1);
  return true;
}

function doRequest(method, path, state, body, tags) {
  const params = {
    headers: {
      Authorization: `Bearer ${state.accessToken}`,
      "Content-Type": "application/json",
    },
    tags,
  };

  const payload = body == null ? null : JSON.stringify(body);
  return http.request(method, `${BASE_URL}${path}`, payload, params);
}

function pollCompletedFeedback(state, sessionId, flowName) {
  const encoded = encodeURIComponent(sessionId);

  for (let i = 0; i < FEEDBACK_POLL_MAX; i += 1) {
    const res = requestWithAuth({
      state,
      method: "GET",
      path: `/api/interview/sessions/feedback?sessionId=${encoded}`,
      tags: { flow: flowName, step: "poll_feedback" },
    });

    if (res.status === 202) {
      sleep(FEEDBACK_POLL_INTERVAL_SEC);
      continue;
    }
    if (res.status !== 200) {
      return false;
    }

    const json = parseJson(res);
    const feedbackStatus = String(json?.data?.status || "").toUpperCase();
    if (feedbackStatus === "COMPLETED") {
      return true;
    }
    if (feedbackStatus === "FAILED") {
      return false;
    }

    sleep(FEEDBACK_POLL_INTERVAL_SEC);
  }
  return false;
}

function pickQuestion(questionListJson) {
  const questions = questionListJson?.data?.questions || [];
  if (!Array.isArray(questions) || questions.length === 0) {
    return null;
  }
  const idx = exec.vu.iterationInScenario % questions.length;
  const picked = questions[idx];
  const questionId = picked.questionId ?? picked.question_id;
  if (questionId == null) {
    return null;
  }
  return { questionId: Number(questionId) };
}

function pickSessionId(json) {
  return (
    (json && json.data && (json.data.session_id || json.data.sessionId)) ||
    null
  );
}

function pickQuestionText(json) {
  return (
    (json && json.data && (json.data.question_text || json.data.questionText)) ||
    null
  );
}

function extractAccessTokenFromHeaders(headers) {
  const authHeader = getHeaderValue(headers, "authorization");
  if (!authHeader) {
    return null;
  }
  const value = String(authHeader);
  if (!value.startsWith("Bearer ")) {
    return null;
  }
  return value.slice("Bearer ".length).trim();
}

function extractRefreshCookie(response) {
  const cookieEntries = response?.cookies?.refreshToken;
  if (Array.isArray(cookieEntries) && cookieEntries.length > 0) {
    return cookieEntries[0].value;
  }
  return null;
}

function getHeaderValue(headers, keyNameLower) {
  if (!headers) {
    return null;
  }
  const entries = Object.entries(headers);
  for (let i = 0; i < entries.length; i += 1) {
    const [key, value] = entries[i];
    if (key.toLowerCase() === keyNameLower) {
      return Array.isArray(value) ? value[0] : value;
    }
  }
  return null;
}

function parseJson(response) {
  try {
    return response.json();
  } catch (_) {
    return {};
  }
}

function parseSqlTimestamp(value) {
  if (!value) {
    return 0;
  }
  // csv timestamp format: yyyy-MM-dd HH:mm:ss (UTC 저장값)
  const iso = `${String(value).replace(" ", "T")}Z`;
  const parsed = Date.parse(iso);
  return Number.isFinite(parsed) ? parsed : 0;
}

function parseCsv(csvText) {
  const lines = csvText
    .split(/\r?\n/)
    .map((line) => line.trim())
    .filter((line) => line.length > 0);

  if (lines.length < 2) {
    return [];
  }

  const headers = parseCsvLine(lines[0]);
  const rows = [];
  for (let i = 1; i < lines.length; i += 1) {
    const cols = parseCsvLine(lines[i]);
    if (cols.length === 0) {
      continue;
    }
    const row = {};
    for (let c = 0; c < headers.length; c += 1) {
      row[headers[c]] = cols[c] ?? "";
    }
    rows.push(row);
  }
  return rows;
}

function parseCsvLine(line) {
  const result = [];
  let current = "";
  let inQuotes = false;

  for (let i = 0; i < line.length; i += 1) {
    const ch = line[i];

    if (ch === '"') {
      if (inQuotes && i + 1 < line.length && line[i + 1] === '"') {
        current += '"';
        i += 1;
      } else {
        inQuotes = !inQuotes;
      }
      continue;
    }

    if (ch === "," && !inQuotes) {
      result.push(current);
      current = "";
      continue;
    }

    current += ch;
  }
  result.push(current);
  return result;
}

function fakeUnauthorizedResponse() {
  return {
    status: 401,
    headers: {},
    cookies: {},
    json: () => ({}),
  };
}

function resolveTokenCsvPath(requestedPath) {
  const candidates = [];
  const seen = {};

  function pushCandidate(path) {
    if (!path || path.trim().length === 0) {
      return;
    }
    const normalized = path.trim();
    if (seen[normalized]) {
      return;
    }
    seen[normalized] = true;
    candidates.push(normalized);
  }

  if (requestedPath && requestedPath.trim().length > 0) {
    const raw = requestedPath.trim();
    pushCandidate(raw);
    pushCandidate(`./${raw}`);

    if (raw.startsWith("loadtest/")) {
      pushCandidate(raw.slice("loadtest/".length));
    } else {
      pushCandidate(`loadtest/${raw}`);
    }
  } else {
    pushCandidate("generated/user-access-token-list.csv");
    pushCandidate("./generated/user-access-token-list.csv");
    pushCandidate("loadtest/generated/user-access-token-list.csv");
    pushCandidate("k6/generated/user-access-token-list.csv");
  }

  for (let i = 0; i < candidates.length; i += 1) {
    const candidate = candidates[i];
    try {
      open(candidate);
      return candidate;
    } catch (_) {
      // try next path
    }
  }

  throw new Error(
    `cannot open user token csv. tried paths=${candidates.join(", ")}`
  );
}
