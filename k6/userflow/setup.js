/**
 * Token Pool Setup (VU 사용자 토큰 분배)
 *
 * 실행 예시:
 * - k6 run k6/userflow/practiceflow.js --env BASE_URL=http://localhost:8080 --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv --env PRACTICE_RATE=6
 * - k6 run k6/userflow/interview_flow.js --env BASE_URL=http://localhost:8080 --env USER_TOKENS_JSON='[{"user_id":1,"access_token":"...","refresh_token":"..."}]' --env PRACTICE_RATE=2 --env REAL_RATE=1
 *
 * 토큰 소스 우선순위:
 * 1) USER_TOKENS_JSON: [{"user_id","access_token","refresh_token","access_expires_at"}] 형태
 * 2) ACCESS_TOKEN(+선택: REFRESH_TOKEN/SINGLE_USER_ID/ACCESS_EXPIRES_AT)
 * 3) USER_TOKEN_CSV (기본 탐색 경로 포함)
 *
 * 관련 환경변수:
 * - USER_TOKEN_CSV: CSV 파일 경로
 * - USER_TOKENS_JSON: JSON 문자열 토큰 목록
 * - ACCESS_TOKEN: 단일 유저 모드 Access Token
 * - REFRESH_TOKEN: 단일 유저 모드 Refresh Token(선택)
 * - SINGLE_USER_ID: 단일 유저 모드 user_id (기본: 1)
 * - ACCESS_EXPIRES_AT: Access Token 만료 시각 (선택, ISO 혹은 SQL timestamp)
 */
import exec from "k6/execution";
import { SharedArray } from "k6/data";
import { fail } from "k6";

const USER_TOKEN_CSV = __ENV.USER_TOKEN_CSV || "";
const USER_TOKENS_JSON = __ENV.USER_TOKENS_JSON || "";
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || "";
const REFRESH_TOKEN = __ENV.REFRESH_TOKEN || "";
const SINGLE_USER_ID = Number(__ENV.SINGLE_USER_ID || 1);
const ACCESS_EXPIRES_AT = __ENV.ACCESS_EXPIRES_AT || "";

const tokenPoolDescriptor = resolveTokenPoolDescriptor();

const tokenPoolRows = new SharedArray("vu-token-pool-rows", () => {
  const rows = buildTokenPoolRows(tokenPoolDescriptor);
  if (!Array.isArray(rows) || rows.length === 0) {
    throw new Error("token pool is empty");
  }
  return rows;
});

export function setupTokenPool({ requiredUserCount, contextLabel, baseUrl, duration }) {
  if (tokenPoolRows.length < requiredUserCount) {
    throw new Error(
      `not enough users in token pool: pool=${tokenPoolRows.length}, required=${requiredUserCount}`
    );
  }

  const prefix = contextLabel ? `[setup] ${contextLabel}` : "[setup]";
  console.log(
    `${prefix} baseUrl=${baseUrl || ""}, duration=${duration || ""}, tokenSource=${tokenPoolDescriptor.source}, tokenRef=${tokenPoolDescriptor.detail}, poolSize=${tokenPoolRows.length}`
  );

  return {
    tokenPoolSize: tokenPoolRows.length,
    tokenSource: tokenPoolDescriptor.source,
    tokenRef: tokenPoolDescriptor.detail,
  };
}

export function getOrInitVuState(store, scenarioName, userSpan, userOffset) {
  if (store[scenarioName]) {
    return store[scenarioName];
  }

  const poolSize = tokenPoolRows.length;
  const vuId = resolveVuId();
  let userIndex = resolveUserIndex(vuId, userSpan, userOffset, poolSize);
  if (!Number.isInteger(userIndex)) {
    userIndex = resolveFallbackUserIndex(userSpan, userOffset, poolSize);
  }

  if (!Number.isInteger(userIndex) || userIndex < 0 || userIndex >= poolSize) {
    fail(
      `user index out of range: scenario=${scenarioName}, userIndex=${userIndex}, vuId=${vuId}, tokenPool=${poolSize}`
    );
  }

  const user = tokenPoolRows[userIndex];
  if (!user || !user.access_token) {
    fail(`invalid token row: scenario=${scenarioName}, userIndex=${userIndex}`);
  }

  const state = {
    userId: Number(user.user_id),
    accessToken: String(user.access_token),
    refreshToken: user.refresh_token ? String(user.refresh_token) : "",
    accessExpiresAtMs: parseAccessExpiresAt(user.access_expires_at),
  };
  store[scenarioName] = state;
  return state;
}

function resolveTokenPoolDescriptor() {
  if (USER_TOKENS_JSON && USER_TOKENS_JSON.trim().length > 0) {
    return {
      source: "json-env",
      detail: "USER_TOKENS_JSON",
    };
  }

  if (ACCESS_TOKEN && ACCESS_TOKEN.trim().length > 0) {
    return {
      source: "single-token-env",
      detail: "ACCESS_TOKEN",
    };
  }

  const csvPath = resolveTokenCsvPath(USER_TOKEN_CSV);
  return {
    source: "csv-file",
    detail: csvPath,
  };
}

function buildTokenPoolRows(descriptor) {
  if (descriptor.source === "json-env") {
    return normalizeRows(parseTokenRowsFromJson(USER_TOKENS_JSON));
  }

  if (descriptor.source === "single-token-env") {
    return normalizeRows([
      {
        user_id: Number.isInteger(SINGLE_USER_ID) && SINGLE_USER_ID > 0 ? SINGLE_USER_ID : 1,
        access_token: ACCESS_TOKEN,
        refresh_token: REFRESH_TOKEN,
        access_expires_at: ACCESS_EXPIRES_AT,
      },
    ]);
  }

  return normalizeRows(parseCsv(open(descriptor.detail)));
}

function parseTokenRowsFromJson(jsonText) {
  let parsed;
  try {
    parsed = JSON.parse(jsonText);
  } catch (e) {
    throw new Error(`USER_TOKENS_JSON parse failed: ${String(e)}`);
  }

  if (Array.isArray(parsed)) {
    return parsed;
  }

  if (parsed && Array.isArray(parsed.tokens)) {
    return parsed.tokens;
  }

  throw new Error("USER_TOKENS_JSON must be an array or object with tokens array");
}

function normalizeRows(rows) {
  if (!Array.isArray(rows)) {
    return [];
  }

  return rows.map((row, index) => {
    const userIdRaw = row?.user_id ?? row?.userId ?? index + 1;
    const accessToken = row?.access_token ?? row?.accessToken;
    const refreshToken = row?.refresh_token ?? row?.refreshToken ?? "";
    const accessExpiresAt = row?.access_expires_at ?? row?.accessExpiresAt ?? "";

    if (!accessToken || String(accessToken).trim().length === 0) {
      throw new Error(`token row missing access_token at index=${index}`);
    }

    const parsedUserId = Number(userIdRaw);
    const safeUserId = Number.isInteger(parsedUserId) && parsedUserId > 0 ? parsedUserId : index + 1;

    return {
      user_id: safeUserId,
      access_token: String(accessToken),
      refresh_token: refreshToken ? String(refreshToken) : "",
      access_expires_at: accessExpiresAt ? String(accessExpiresAt) : "",
    };
  });
}

function resolveVuId() {
  const candidates = [exec.vu?.idInScenario, exec.vu?.idInTest, exec.vu?.idInInstance];
  for (let i = 0; i < candidates.length; i += 1) {
    const value = Number(candidates[i]);
    if (Number.isInteger(value) && value > 0) {
      return value;
    }
  }
  return null;
}

function resolveUserIndex(vuIdInScenario, userSpan, userOffset, poolSize) {
  const vuId = Number(vuIdInScenario);
  if (!Number.isInteger(vuId) || vuId <= 0) {
    return Number.NaN;
  }

  const span = Math.max(1, Number(userSpan) || poolSize);
  const offset = Math.max(0, Number(userOffset) || 0);
  return offset + ((vuId - 1) % span);
}

function resolveFallbackUserIndex(userSpan, userOffset, poolSize) {
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

  const span = Math.max(1, Number(userSpan) || poolSize);
  const offset = Math.max(0, Number(userOffset) || 0);
  return offset + (iteration % span);
}

function parseAccessExpiresAt(value) {
  if (!value) {
    return 0;
  }
  const raw = String(value).trim();
  const direct = Date.parse(raw);
  if (Number.isFinite(direct)) {
    return direct;
  }
  const sqlStyle = raw.includes("T") ? raw : `${raw.replace(" ", "T")}Z`;
  const parsed = Date.parse(sqlStyle);
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
    if (ch === "\"") {
      if (inQuotes && i + 1 < line.length && line[i + 1] === "\"") {
        current += "\"";
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

function resolveTokenCsvPath(requestedPath) {
  const candidates = [];
  const seen = {};

  function pushCandidate(value) {
    if (!value || String(value).trim().length === 0) {
      return;
    }
    const normalized = String(value).trim();
    if (seen[normalized]) {
      return;
    }
    seen[normalized] = true;
    candidates.push(normalized);
  }

  if (requestedPath && String(requestedPath).trim().length > 0) {
    const raw = String(requestedPath).trim();
    pushCandidate(raw);
    pushCandidate(`./${raw}`);
    if (raw.startsWith("loadtest/")) {
      pushCandidate(raw.slice("loadtest/".length));
    } else {
      pushCandidate(`loadtest/${raw}`);
    }
    if (raw.startsWith("k6/")) {
      pushCandidate(raw.slice("k6/".length));
    } else {
      pushCandidate(`k6/${raw}`);
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
      // try next
    }
  }

  throw new Error(
    `cannot build token pool: provide USER_TOKEN_CSV or USER_TOKENS_JSON or ACCESS_TOKEN. tried csv paths=${candidates.join(
      ", "
    )}`
  );
}
