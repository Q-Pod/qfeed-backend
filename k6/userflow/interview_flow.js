/**
 * Interview Flow (연습+실전 혼합)
 *
 * 실행 예시:
 * - k6 run k6/userflow/interview_flow.js --env BASE_URL=http://localhost:8080 --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv --env PRACTICE_RATE=6 --env REAL_RATE=2 --env REAL_TURN_LIMIT=4
 *
 * 주요 환경변수:
 * - BASE_URL: API 서버 주소 (기본: http://localhost:8080)
 * - USER_TOKEN_CSV: 유저 토큰 CSV 경로 (user_id/access_token/refresh_token 컬럼)
 * - DURATION: 시나리오 실행 시간 (기본: 5m)
 * - PRACTICE_RATE: 초당 연습 사이클 시작 수 (기본: 6)
 * - REAL_RATE: 초당 실전 사이클 시작 수 (기본: 2)
 * - MOCK_AI: AI 호출(STT/TTS) mock 여부 (기본: true)
 * - PRACTICE_PRE_ALLOCATED_VUS / PRACTICE_MAX_VUS: 연습 VU 설정
 * - REAL_PRE_ALLOCATED_VUS / REAL_MAX_VUS: 실전 VU 설정
 * - REAL_TURN_LIMIT: 실전 턴 수(실제 값은 realflow.js에서 사용, 기본 4)
 *
 * 참고:
 * - 혼합 실행 시 사용자 인덱스 충돌 방지를 위해
 *   practice 사용자 구간과 real 사용자 구간을 분리해 할당합니다.
 */
import { DURATION, envNumber, setupTokenRows } from "./flow_utils.js";
import { practiceCycle } from "./practiceflow.js";
import { realCycle, configureRealFlow } from "./realflow.js";

const SCENARIO_PRACTICE = "practice_cycle";
const SCENARIO_REAL = "real_cycle";

const PRACTICE_RATE = envNumber("PRACTICE_RATE", 6);
const PRACTICE_PRE_ALLOCATED_VUS = envNumber("PRACTICE_PRE_ALLOCATED_VUS", 20);
const PRACTICE_MAX_VUS = envNumber("PRACTICE_MAX_VUS", 60);

const REAL_RATE = envNumber("REAL_RATE", 2);
const REAL_PRE_ALLOCATED_VUS = envNumber("REAL_PRE_ALLOCATED_VUS", 10);
const REAL_MAX_VUS = envNumber("REAL_MAX_VUS", 40);

const PRACTICE_USER_SPAN = PRACTICE_RATE > 0 ? Math.max(1, PRACTICE_MAX_VUS) : 0;
const REAL_USER_SPAN = REAL_RATE > 0 ? Math.max(1, REAL_MAX_VUS) : 0;
const REAL_USER_OFFSET = PRACTICE_USER_SPAN;
const REQUIRED_USER_COUNT = PRACTICE_USER_SPAN + REAL_USER_SPAN;

configureRealFlow({
  scenarioName: SCENARIO_REAL,
  userSpan: REAL_USER_SPAN || Math.max(1, REAL_MAX_VUS),
  userOffset: REAL_USER_OFFSET,
});

const mixedScenarios = {};
if (PRACTICE_RATE > 0) {
  mixedScenarios[SCENARIO_PRACTICE] = {
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
  mixedScenarios[SCENARIO_REAL] = {
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
  scenarios: mixedScenarios,
  thresholds: {
    http_req_failed: ["rate<0.10"],
    practice_cycle_failed: ["rate<0.20"],
    real_cycle_failed: ["rate<0.25"],
    practice_cycle_duration_ms: ["p(95)<15000"],
    real_cycle_duration_ms: ["p(95)<20000"],
  },
};

export function setup() {
  if (PRACTICE_RATE <= 0 && REAL_RATE <= 0) {
    throw new Error("set PRACTICE_RATE or REAL_RATE greater than zero");
  }

  const required = REQUIRED_USER_COUNT > 0 ? REQUIRED_USER_COUNT : 1;
  setupTokenRows(
    required,
    `interview_flow practiceRate=${PRACTICE_RATE}, realRate=${REAL_RATE}, practiceMaxVUs=${PRACTICE_MAX_VUS}, realMaxVUs=${REAL_MAX_VUS}`
  );
}

export { practiceCycle, realCycle };

export function teardown() {
  console.log("[teardown] interview mixed flow test finished");
}

export default function () {
  practiceCycle();
}
