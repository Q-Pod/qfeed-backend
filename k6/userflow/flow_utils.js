/**
 * Flow Utils (유저 시나리오 공통 유틸)
 *
 * 실행 예시:
 * - k6 run k6/userflow/practiceflow.js --env BASE_URL=http://localhost:8080 --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv --env PRACTICE_RATE=6
 * - k6 run k6/userflow/realflow.js --env BASE_URL=http://localhost:8080 --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv --env REAL_RATE=2 --env REAL_TURN_LIMIT=4
 * - k6 run k6/userflow/interview_flow.js --env BASE_URL=http://localhost:8080 --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv --env PRACTICE_RATE=6 --env REAL_RATE=2 --env REAL_TURN_LIMIT=4
 *
 * 공통 환경변수:
 * - BASE_URL: API 서버 주소 (기본: http://localhost:8080)
 * - USER_TOKEN_CSV: 토큰 CSV 파일 경로
 * - DURATION: 시나리오 실행 시간 (기본: 5m)
 * - MOCK_AI: AI 호출(STT/TTS) mock 여부 (기본: true)
 * - MOCK_STT: STT 호출 mock 여부 (기본: MOCK_AI 값)
 * - MOCK_TTS: TTS 호출 mock 여부 (기본: MOCK_AI 값)
 * - MOCK_STT_LATENCY_SEC: STT mock 지연(초) (기본: 0.05)
 * - MOCK_TTS_LATENCY_SEC: TTS mock 지연(초) (기본: 0.03)
 * - TOKEN_MARGIN_SEC: 액세스 토큰 만료 선갱신 여유(초) (기본: 30)
 * - FEEDBACK_POLL_MAX: 피드백 폴링 최대 횟수 (기본: 15)
 * - FEEDBACK_POLL_INTERVAL_SEC: 피드백 폴링 간격(초) (기본: 0.3)
 * - DEFAULT_ANSWER_TEXT: STT 실패 fallback 답변 텍스트
 * - DEFAULT_FINAL_TTS_TEXT: 실전 종료 TTS 기본 멘트
 */
import http from "k6/http";
import { check, sleep } from "k6";
import { Counter } from "k6/metrics";
import { getOrInitVuState, setupTokenPool } from "./setup.js";

export const BASE_URL = (__ENV.BASE_URL || "http://localhost:8080").replace(/\/+$/, "");
export const DURATION = __ENV.DURATION || "5m";
export const FEEDBACK_POLL_MAX = envNumber("FEEDBACK_POLL_MAX", 15);
export const FEEDBACK_POLL_INTERVAL_SEC = envNumber("FEEDBACK_POLL_INTERVAL_SEC", 0.3);
export const TOKEN_MARGIN_SEC = envNumber("TOKEN_MARGIN_SEC", 30);
export const DEFAULT_ACCESS_EXPIRES_SEC = 600;
export const QUESTION_TYPES = ["CS", "SYSTEM_DESIGN"];
export const MOCK_AI = envBoolean("MOCK_AI", true);
export const MOCK_STT = envBoolean("MOCK_STT", MOCK_AI);
export const MOCK_TTS = envBoolean("MOCK_TTS", MOCK_AI);
export const MOCK_STT_LATENCY_SEC = envNumber("MOCK_STT_LATENCY_SEC", 0.05);
export const MOCK_TTS_LATENCY_SEC = envNumber("MOCK_TTS_LATENCY_SEC", 0.03);
export const DEFAULT_ANSWER_TEXT =
  __ENV.DEFAULT_ANSWER_TEXT ||
  "핵심 개념부터 정리하고, 실제 사례를 함께 설명하겠습니다.";
export const DEFAULT_FINAL_TTS_TEXT =
  __ENV.DEFAULT_FINAL_TTS_TEXT || "수고하셨습니다. 면접이 종료되었습니다.";

export const tokenRefreshTotal = new Counter("token_refresh_total");
export const authRetryTotal = new Counter("auth_retry_total");
export const s3MockUploadTotal = new Counter("s3_mock_upload_total");
export const sttFallbackTotal = new Counter("stt_fallback_total");
export const aiSttMockTotal = new Counter("ai_stt_mock_total");
export const aiTtsMockTotal = new Counter("ai_tts_mock_total");

export function envNumber(name, defaultValue) {
  const raw = __ENV[name];
  if (raw == null || String(raw).trim() === "") {
    return defaultValue;
  }
  const parsed = Number(raw);
  return Number.isFinite(parsed) ? parsed : defaultValue;
}

export function envBoolean(name, defaultValue) {
  const raw = __ENV[name];
  if (raw == null || String(raw).trim() === "") {
    return defaultValue;
  }
  const normalized = String(raw).trim().toLowerCase();
  return normalized === "1" || normalized === "true" || normalized === "yes" || normalized === "on";
}

export function randomChance(probability) {
  if (!Number.isFinite(probability)) {
    return false;
  }
  if (probability <= 0) {
    return false;
  }
  if (probability >= 1) {
    return true;
  }
  return Math.random() < probability;
}

export function chooseQuestionType(seedValue) {
  const safeSeed = Number.isFinite(seedValue) ? Math.floor(seedValue) : 0;
  return QUESTION_TYPES[Math.abs(safeSeed) % QUESTION_TYPES.length];
}

export function setupTokenRows(requiredUserCount, contextLabel) {
  setupTokenPool({
    requiredUserCount,
    contextLabel,
    baseUrl: BASE_URL,
    duration: DURATION,
  });
}

export function getOrInitState(store, scenarioName, userSpan, userOffset) {
  return getOrInitVuState(store, scenarioName, userSpan, userOffset);
}

export function requestWithAuth({ state, method, path, body, tags, headers }) {
  if (!ensureAccessToken(state, tags)) {
    return fakeUnauthorizedResponse();
  }

  let response = doRequest(method, path, state, body, tags, headers);
  if (response.status !== 401) {
    return response;
  }

  authRetryTotal.add(1);
  if (!refreshAccessToken(state, tags)) {
    return response;
  }
  response = doRequest(method, path, state, body, tags, headers);
  return response;
}

export function callHomeCommon(state, flowName) {
  const weeklyRes = requestWithAuth({
    state,
    method: "GET",
    path: "/api/users/me/stats/weekly",
    tags: { flow: flowName, step: "home_weekly" },
  });
  const weeklyOk = check(weeklyRes, { "home weekly 200": (r) => r.status === 200 });
  if (!weeklyOk) {
    return { ok: false, recommendationQuestionId: null };
  }

  const categoriesRes = requestWithAuth({
    state,
    method: "GET",
    path: "/api/questions/categories",
    tags: { flow: flowName, step: "home_categories" },
  });
  const categoriesOk = check(categoriesRes, { "home categories 200": (r) => r.status === 200 });
  if (!categoriesOk) {
    return { ok: false, recommendationQuestionId: null };
  }

  const recommendationRes = requestWithAuth({
    state,
    method: "GET",
    path: "/api/questions/recommendation",
    tags: { flow: flowName, step: "home_recommendation" },
  });
  const recommendationOk = check(recommendationRes, {
    "home recommendation 200/404": (r) => r.status === 200 || r.status === 404,
  });
  if (!recommendationOk) {
    return { ok: false, recommendationQuestionId: null };
  }

  const recommendationJson = parseJson(recommendationRes);
  const recommendationQuestionId = pickQuestionIdFromDetail(recommendationJson);
  return { ok: true, recommendationQuestionId };
}

export function pollCompletedFeedback(state, sessionId, flowName) {
  const encodedSessionId = encodeURIComponent(sessionId);

  for (let i = 0; i < FEEDBACK_POLL_MAX; i += 1) {
    const pollRes = requestWithAuth({
      state,
      method: "GET",
      path: `/api/interview/sessions/feedback?sessionId=${encodedSessionId}`,
      tags: { flow: flowName, step: "poll_feedback" },
    });

    if (pollRes.status === 202) {
      sleep(FEEDBACK_POLL_INTERVAL_SEC);
      continue;
    }

    if (pollRes.status !== 200) {
      return false;
    }

    const json = parseJson(pollRes);
    const status = String(json?.data?.status || "").toUpperCase();
    if (status === "COMPLETED") {
      return true;
    }
    if (status === "FAILED") {
      return false;
    }
    sleep(FEEDBACK_POLL_INTERVAL_SEC);
  }

  return false;
}

export function requestAudioUploadFlow({
  state,
  flowName,
  sessionId,
  mockS3Upload,
  mockS3LatencySec,
  allowSttFallback,
  fallbackAnswerText,
}) {
  const fileName = `${flowName}-${state.userId}-${Date.now()}.webm`;
  const presignedRes = requestWithAuth({
    state,
    method: "POST",
    path: "/api/files/presigned-url",
    body: {
      file_name: fileName,
      file_size: 16000,
      mime_type: "audio/webm",
      category: "AUDIO",
      method: "PUT",
    },
    tags: { flow: flowName, step: "presigned" },
  });

  const presignedOk = check(presignedRes, { "presigned 200": (r) => r.status === 200 });
  if (!presignedOk) {
    return { ok: false, answerText: null, fileId: null };
  }

  const presignedJson = parseJson(presignedRes);
  const fileId = pickFileId(presignedJson);
  const presignedUrl = pickPresignedUrl(presignedJson);
  if (!fileId || !presignedUrl) {
    return { ok: false, answerText: null, fileId: null };
  }

  if (mockS3Upload) {
    s3MockUploadTotal.add(1);
    sleep(mockS3LatencySec);
  } else {
    const uploadRes = http.put(presignedUrl, "RIFF....WEBM_MOCK_BYTES", {
      headers: { "Content-Type": "audio/webm" },
      tags: { flow: flowName, step: "s3_put", external: "s3" },
    });
    const uploadOk = check(uploadRes, {
      "s3 upload 200/204": (r) => r.status === 200 || r.status === 204,
    });
    if (!uploadOk) {
      return { ok: false, answerText: null, fileId: null };
    }
  }

  const confirmRes = requestWithAuth({
    state,
    method: "POST",
    path: `/api/files/${fileId}/confirm`,
    tags: { flow: flowName, step: "confirm_upload" },
  });

  const confirmStrictOk = check(confirmRes, {
    "confirm 200": (r) => r.status === 200,
  });
  if (!confirmStrictOk && !(mockS3Upload && (confirmRes.status === 404 || confirmRes.status === 422))) {
    return { ok: false, answerText: null, fileId };
  }

  const confirmJson = parseJson(confirmRes);
  const audioUrl = pickFileUrl(confirmJson) || presignedUrl;

  if (MOCK_STT) {
    aiSttMockTotal.add(1);
    sleep(MOCK_STT_LATENCY_SEC);
    return { ok: true, answerText: fallbackAnswerText || DEFAULT_ANSWER_TEXT, fileId };
  }

  const sttRes = requestWithAuth({
    state,
    method: "POST",
    path: "/api/ai/stt",
    body: {
      user_id: state.userId,
      session_id: sessionId,
      audio_url: audioUrl,
    },
    tags: { flow: flowName, step: "stt" },
  });

  const sttJson = parseJson(sttRes);
  const sttText = pickSttText(sttJson);
  if (sttRes.status === 200 && sttText) {
    return { ok: true, answerText: sttText, fileId };
  }

  if (!allowSttFallback) {
    return { ok: false, answerText: null, fileId };
  }

  sttFallbackTotal.add(1);
  return { ok: true, answerText: fallbackAnswerText || DEFAULT_ANSWER_TEXT, fileId };
}

export function requestVideoMultipartUploadFlow({
  state,
  flowName,
  mockS3Upload,
  mockS3LatencySec,
  partCount,
}) {
  const safePartCount = Math.max(1, Number.isFinite(partCount) ? Math.floor(partCount) : 1);
  const fileName = `${flowName}-${state.userId}-${Date.now()}.webm`;
  const startRes = requestWithAuth({
    state,
    method: "POST",
    path: "/api/files/presigned-url",
    body: {
      file_name: fileName,
      file_size: 24 * 1024 * 1024,
      mime_type: "video/webm",
      category: "VIDEO",
      method: "PUT",
    },
    tags: { flow: flowName, step: "video_presigned_start" },
  });

  const startOk = check(startRes, { "video presigned 200": (r) => r.status === 200 });
  if (!startOk) {
    return { ok: false, fileId: null };
  }

  const startJson = parseJson(startRes);
  const videoFileId = pickFileId(startJson);
  if (!videoFileId) {
    return { ok: false, fileId: null };
  }

  const uploadedParts = [];

  for (let partNumber = 1; partNumber <= safePartCount; partNumber += 1) {
    const partRes = requestWithAuth({
      state,
      method: "POST",
      path: `/api/files/${videoFileId}/multipart/parts`,
      body: { part_number: partNumber },
      tags: { flow: flowName, step: "video_part_presigned" },
    });

    const partOk = check(partRes, { "video part presigned 200": (r) => r.status === 200 });
    if (!partOk) {
      return { ok: false, fileId: videoFileId };
    }

    const partJson = parseJson(partRes);
    const partPresignedUrl = pickMultipartPartPresignedUrl(partJson);
    if (!partPresignedUrl) {
      return { ok: false, fileId: videoFileId };
    }

    if (mockS3Upload) {
      s3MockUploadTotal.add(1);
      sleep(mockS3LatencySec);
      uploadedParts.push({
        part_number: partNumber,
        etag: `"mock-etag-${videoFileId}-${partNumber}"`,
      });
      continue;
    }

    const uploadRes = http.put(partPresignedUrl, "MOCK_VIDEO_PART_BYTES", {
      headers: { "Content-Type": "video/webm" },
      tags: { flow: flowName, step: "video_s3_part_put", external: "s3" },
    });
    const uploadOk = check(uploadRes, {
      "video part upload 200/204": (r) => r.status === 200 || r.status === 204,
    });
    if (!uploadOk) {
      return { ok: false, fileId: videoFileId };
    }

    const etagHeader = getHeaderValue(uploadRes.headers, "etag");
    const resolvedEtag =
      etagHeader && String(etagHeader).trim().length > 0
        ? String(etagHeader).trim()
        : `"mock-etag-${videoFileId}-${partNumber}"`;
    uploadedParts.push({
      part_number: partNumber,
      etag: resolvedEtag,
    });
  }

  const completeRes = requestWithAuth({
    state,
    method: "POST",
    path: `/api/files/${videoFileId}/multipart/complete`,
    body: { parts: uploadedParts },
    tags: { flow: flowName, step: "video_multipart_complete" },
  });

  const completeStrictOk = check(completeRes, {
    "video multipart complete 200": (r) => r.status === 200,
  });
  if (!completeStrictOk && !(mockS3Upload && (completeRes.status === 404 || completeRes.status === 422))) {
    return { ok: false, fileId: videoFileId };
  }

  return { ok: true, fileId: videoFileId };
}

export function callTts(state, flowName, sessionId, text, stepName) {
  if (MOCK_TTS) {
    aiTtsMockTotal.add(1);
    sleep(MOCK_TTS_LATENCY_SEC);
    return true;
  }

  const ttsRes = requestWithAuth({
    state,
    method: "POST",
    path: "/api/ai/tts",
    body: {
      user_id: state.userId,
      session_id: sessionId,
      text,
    },
    headers: { Accept: "audio/mpeg" },
    tags: { flow: flowName, step: stepName || "tts" },
  });

  return check(ttsRes, { "tts 200": (r) => r.status === 200 });
}

export function pickQuestionFromList(questionListJson, seed) {
  const questions = questionListJson?.data?.questions;
  if (!Array.isArray(questions) || questions.length === 0) {
    return null;
  }
  const index = Math.abs(Number.isFinite(seed) ? Math.floor(seed) : 0) % questions.length;
  const picked = questions[index];
  const questionId = picked?.questionId ?? picked?.question_id;
  const questionType = picked?.type ?? picked?.question_type;
  return questionId ? { questionId: Number(questionId), questionType } : null;
}

export function pickQuestionIdFromDetail(questionJson) {
  const questionId = questionJson?.data?.questionId ?? questionJson?.data?.question_id;
  return questionId == null ? null : Number(questionId);
}

export function pickQuestionTypeFromDetail(questionJson) {
  return questionJson?.data?.type ?? questionJson?.data?.question_type ?? null;
}

export function pickQuestionTextFromSessionCreate(sessionCreateJson) {
  return sessionCreateJson?.data?.question_text || sessionCreateJson?.data?.questionText || null;
}

export function pickSessionId(sessionCreateJson) {
  return sessionCreateJson?.data?.session_id || sessionCreateJson?.data?.sessionId || null;
}

export function pickNextQuestionText(realSubmitJson) {
  return (
    realSubmitJson?.data?.next_question?.content ||
    realSubmitJson?.data?.nextQuestion?.content ||
    null
  );
}

export function pickBadCaseGuidance(realSubmitJson) {
  return (
    realSubmitJson?.data?.bad_case_feedback?.guidance ||
    realSubmitJson?.data?.badCaseFeedback?.guidance ||
    null
  );
}

export function isBadCaseDetected(realSubmitJson) {
  const message = String(realSubmitJson?.message || "");
  return (
    message === "bad_case_detected" ||
    realSubmitJson?.data?.bad_case_feedback != null ||
    realSubmitJson?.data?.badCaseFeedback != null
  );
}

export function isFinalTurn(realSubmitJson) {
  return Boolean(realSubmitJson?.data?.is_final ?? realSubmitJson?.data?.isFinal);
}

export function parseJson(response) {
  try {
    return response.json();
  } catch (_) {
    return {};
  }
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

  const refreshRes = http.post(`${BASE_URL}/api/auth/tokens`, null, {
    headers: {
      Cookie: `refreshToken=${state.refreshToken}`,
      "Content-Type": "application/json",
    },
    tags: Object.assign({ endpoint: "auth_refresh" }, tags || {}),
  });

  const refreshOk = check(refreshRes, {
    "refresh status 200": (r) => r.status === 200,
    "refresh auth header": (r) => extractAccessTokenFromHeaders(r.headers) != null,
  });
  if (!refreshOk) {
    return false;
  }

  const refreshJson = parseJson(refreshRes);
  const expiresIn = Number(refreshJson?.data?.expiresIn || DEFAULT_ACCESS_EXPIRES_SEC);
  const refreshedAccessToken = extractAccessTokenFromHeaders(refreshRes.headers);
  if (!refreshedAccessToken) {
    return false;
  }

  const refreshedCookie = extractRefreshCookie(refreshRes);
  if (refreshedCookie) {
    state.refreshToken = refreshedCookie;
  }
  state.accessToken = refreshedAccessToken;
  state.accessExpiresAtMs = Date.now() + expiresIn * 1000;
  tokenRefreshTotal.add(1);
  return true;
}

function doRequest(method, path, state, body, tags, headers) {
  const mergedHeaders = Object.assign(
    {
      Authorization: `Bearer ${state.accessToken}`,
      Accept: "application/json",
    },
    headers || {}
  );

  let payload = null;
  if (body != null) {
    const hasExplicitContentType =
      mergedHeaders["Content-Type"] != null || mergedHeaders["content-type"] != null;
    if (typeof body === "string") {
      payload = body;
    } else {
      payload = JSON.stringify(body);
      if (!hasExplicitContentType) {
        mergedHeaders["Content-Type"] = "application/json";
      }
    }
  }

  const requestUrl = path.startsWith("http://") || path.startsWith("https://")
    ? path
    : `${BASE_URL}${path}`;

  return http.request(method, requestUrl, payload, {
    headers: mergedHeaders,
    tags,
  });
}

function pickFileId(presignedJson) {
  const fileId = presignedJson?.data?.file_id ?? presignedJson?.data?.fileId;
  return fileId == null ? null : Number(fileId);
}

function pickPresignedUrl(presignedJson) {
  return presignedJson?.data?.presigned_url || presignedJson?.data?.presignedUrl || null;
}

function pickMultipartPartPresignedUrl(partPresignedJson) {
  return partPresignedJson?.data?.presigned_url || partPresignedJson?.data?.presignedUrl || null;
}

function pickFileUrl(confirmJson) {
  return confirmJson?.data?.fileUrl || confirmJson?.data?.file_url || null;
}

function pickSttText(sttJson) {
  return sttJson?.data?.text || null;
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
    if (String(key).toLowerCase() === keyNameLower) {
      return Array.isArray(value) ? value[0] : value;
    }
  }
  return null;
}

function fakeUnauthorizedResponse() {
  return {
    status: 401,
    headers: {},
    cookies: {},
    json: () => ({}),
  };
}
