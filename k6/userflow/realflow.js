/**
 * Real Flow (실전모드 단독)
 *
 * 실행 예시:
 * - k6 run k6/userflow/realflow.js --env BASE_URL=http://localhost:8080 --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv --env REAL_RATE=2 --env REAL_TURN_LIMIT=4
 *
 * 주요 환경변수:
 * - BASE_URL: API 서버 주소 (기본: http://localhost:8080)
 * - USER_TOKEN_CSV: 유저 토큰 CSV 경로 (user_id/access_token/refresh_token 컬럼)
 * - DURATION: 시나리오 실행 시간 (기본: 5m)
 * - REAL_RATE: 초당 실전 사이클 시작 수 (기본: 2)
 * - REAL_PRE_ALLOCATED_VUS: 초기 VU 수 (기본: 10)
 * - REAL_MAX_VUS: 최대 VU 수 (기본: 40)
 * - REAL_TURN_LIMIT: 실전 질문/답변 최대 턴 수 (기본: 4)
 * - REAL_SESSION_STATE_RATE: 세션 상태 조회 호출 확률 0~1 (기본: 0.2)
 * - REAL_VIDEO_PARTS: 턴당 비디오 multipart part 수 (기본: 2)
 * - REAL_THINK_SEC: 턴 간 사용자 think time(초) (기본: 0.2)
 * - ENABLE_HOME_CALLS: 홈 공통 호출(주간통계/카테고리/추천) 포함 여부 (기본: true)
 * - ENABLE_FEEDBACK_POLL: 최종 피드백 폴링 여부 (기본: false)
 * - MOCK_AI: AI 호출(STT/TTS) mock 여부 (기본: true)
 * - MOCK_S3_UPLOAD: S3 PUT 외부 호출 mock 사용 여부 (기본: true)
 * - MOCK_S3_LATENCY_SEC: mock S3 업로드 지연(초) (기본: 0.03)
 * - ALLOW_STT_FALLBACK: STT 실패 시 텍스트 fallback 허용 여부 (기본: true)
 * - ALLOW_TTS_FAILURE: TTS 실패를 사이클 실패로 처리하지 않을지 여부 (기본: false)
 */
import exec from "k6/execution";
import { sleep } from "k6";
import { Rate, Trend } from "k6/metrics";
import {
  DURATION,
  DEFAULT_ANSWER_TEXT,
  DEFAULT_FINAL_TTS_TEXT,
  chooseQuestionType,
  callHomeCommon,
  callTts,
  envBoolean,
  envNumber,
  getOrInitState,
  isBadCaseDetected,
  isFinalTurn,
  parseJson,
  pickBadCaseGuidance,
  pickNextQuestionText,
  pickQuestionTextFromSessionCreate,
  pickSessionId,
  pollCompletedFeedback,
  randomChance,
  requestAudioUploadFlow,
  requestVideoMultipartUploadFlow,
  requestWithAuth,
  setupTokenRows,
} from "./flow_utils.js";

const SCENARIO_REAL = "real_cycle";

const REAL_RATE = envNumber("REAL_RATE", 2);
const REAL_PRE_ALLOCATED_VUS = envNumber("REAL_PRE_ALLOCATED_VUS", 10);
const REAL_MAX_VUS = envNumber("REAL_MAX_VUS", 40);

const REAL_TURN_LIMIT = Math.max(1, Math.floor(envNumber("REAL_TURN_LIMIT", 4)));
const REAL_SESSION_STATE_RATE = envNumber("REAL_SESSION_STATE_RATE", 0.2);
const REAL_VIDEO_PARTS = Math.max(1, Math.floor(envNumber("REAL_VIDEO_PARTS", 2)));
const REAL_THINK_SEC = envNumber("REAL_THINK_SEC", 0.2);

const ENABLE_HOME_CALLS = envBoolean("ENABLE_HOME_CALLS", true);
const ENABLE_FEEDBACK_POLL = envBoolean("ENABLE_FEEDBACK_POLL", false);

const MOCK_S3_UPLOAD = envBoolean("MOCK_S3_UPLOAD", true);
const MOCK_S3_LATENCY_SEC = envNumber("MOCK_S3_LATENCY_SEC", 0.03);
const ALLOW_STT_FALLBACK = envBoolean("ALLOW_STT_FALLBACK", true);

const ALLOW_TTS_FAILURE = envBoolean("ALLOW_TTS_FAILURE", false);

const realCycleFailed = new Rate("real_cycle_failed");
const realCycleDuration = new Trend("real_cycle_duration_ms");

const standaloneScenarios = {};
if (REAL_RATE > 0) {
  standaloneScenarios[SCENARIO_REAL] = {
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
  scenarios: standaloneScenarios,
  thresholds: {
    http_req_failed: ["rate<0.10"],
    real_cycle_failed: ["rate<0.25"],
    real_cycle_duration_ms: ["p(95)<20000"],
  },
};

const realFlowConfig = {
  scenarioName: SCENARIO_REAL,
  userSpan: Math.max(1, REAL_MAX_VUS),
  userOffset: Math.max(0, envNumber("REAL_USER_OFFSET", 0)),
};

const vuStateByScenario = {};

export function configureRealFlow(overrides) {
  if (!overrides) {
    return;
  }

  if (overrides.scenarioName) {
    realFlowConfig.scenarioName = overrides.scenarioName;
  }
  if (Number.isFinite(overrides.userSpan) && overrides.userSpan > 0) {
    realFlowConfig.userSpan = Math.floor(overrides.userSpan);
  }
  if (Number.isFinite(overrides.userOffset) && overrides.userOffset >= 0) {
    realFlowConfig.userOffset = Math.floor(overrides.userOffset);
  }
}

export function getRealFlowRequiredUserCount() {
  return realFlowConfig.userOffset + Math.max(1, realFlowConfig.userSpan);
}

export function setup() {
  setupTokenRows(
    getRealFlowRequiredUserCount(),
    `realflow realRate=${REAL_RATE}, realMaxVUs=${REAL_MAX_VUS}, turnLimit=${REAL_TURN_LIMIT}`
  );
}

export function realCycle() {
  const startedAt = Date.now();
  let success = true;

  try {
    const flowName = realFlowConfig.scenarioName;
    const state = getOrInitState(
      vuStateByScenario,
      flowName,
      realFlowConfig.userSpan,
      realFlowConfig.userOffset
    );
    const questionType = chooseQuestionType(exec.vu.iterationInScenario);

    if (ENABLE_HOME_CALLS) {
      const home = callHomeCommon(state, flowName);
      if (!home.ok) {
        success = false;
        return;
      }
    }

    const createRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/interview/sessions",
      body: {
        interviewType: "REAL_INTERVIEW",
        questionType,
      },
      tags: { flow: flowName, step: "create_session" },
    });
    if (createRes.status !== 201) {
      success = false;
      return;
    }

    const createJson = parseJson(createRes);
    const sessionId = pickSessionId(createJson);
    let currentQuestion = pickQuestionTextFromSessionCreate(createJson);
    if (!sessionId || !currentQuestion) {
      success = false;
      return;
    }

    if (randomChance(REAL_SESSION_STATE_RATE)) {
      const stateRes = requestWithAuth({
        state,
        method: "GET",
        path: `/api/interview/sessions?sessionId=${encodeURIComponent(sessionId)}`,
        tags: { flow: flowName, step: "session_state" },
      });
      if (stateRes.status !== 200) {
        success = false;
        return;
      }
    }

    const firstTtsOk = callTts(
      state,
      flowName,
      sessionId,
      currentQuestion,
      "tts_first_question"
    );
    if (!firstTtsOk && !ALLOW_TTS_FAILURE) {
      success = false;
      return;
    }

    let isFinal = false;
    for (let turn = 0; turn < REAL_TURN_LIMIT; turn += 1) {
      const answerResult = requestAudioUploadFlow({
        state,
        flowName,
        sessionId,
        mockS3Upload: MOCK_S3_UPLOAD,
        mockS3LatencySec: MOCK_S3_LATENCY_SEC,
        allowSttFallback: ALLOW_STT_FALLBACK,
        fallbackAnswerText: `${DEFAULT_ANSWER_TEXT} [real][turn=${turn + 1}]`,
      });
      if (!answerResult.ok) {
        success = false;
        return;
      }

      const videoResult = requestVideoMultipartUploadFlow({
        state,
        flowName,
        mockS3Upload: MOCK_S3_UPLOAD,
        mockS3LatencySec: MOCK_S3_LATENCY_SEC,
        partCount: REAL_VIDEO_PARTS,
      });
      if (!videoResult.ok) {
        success = false;
        return;
      }

      const submitRes = requestWithAuth({
        state,
        method: "POST",
        path: "/api/answers/real",
        body: {
          session_id: sessionId,
          question_type: questionType,
          question: currentQuestion,
          answer_text: answerResult.answerText || DEFAULT_ANSWER_TEXT,
          video_file_id: videoResult.fileId,
        },
        tags: { flow: flowName, step: "submit_real" },
      });
      if (submitRes.status !== 201) {
        success = false;
        return;
      }

      const submitJson = parseJson(submitRes);
      isFinal = isFinalTurn(submitJson);
      const badCaseDetected = isBadCaseDetected(submitJson);
      const nextQuestion = pickNextQuestionText(submitJson);

      if (isFinal) {
        const finalMessage = nextQuestion || DEFAULT_FINAL_TTS_TEXT;
        const finalTtsOk = callTts(state, flowName, sessionId, finalMessage, "tts_final");
        if (!finalTtsOk && !ALLOW_TTS_FAILURE) {
          success = false;
          return;
        }
        break;
      }

      if (badCaseDetected) {
        const guidance = pickBadCaseGuidance(submitJson) || "답변을 조금 더 구체적으로 설명해 주세요.";
        const badCaseTtsOk = callTts(state, flowName, sessionId, guidance, "tts_bad_case");
        if (!badCaseTtsOk && !ALLOW_TTS_FAILURE) {
          success = false;
          return;
        }
      } else {
        if (!nextQuestion) {
          success = false;
          return;
        }
        currentQuestion = nextQuestion;
        const nextTtsOk = callTts(state, flowName, sessionId, nextQuestion, "tts_next_question");
        if (!nextTtsOk && !ALLOW_TTS_FAILURE) {
          success = false;
          return;
        }
      }

      sleep(REAL_THINK_SEC);
    }

    if (!isFinal) {
      success = false;
      return;
    }

    const feedbackReqRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/interview/sessions/feedback/request",
      body: { sessionId },
      tags: { flow: flowName, step: "request_feedback" },
    });
    if (feedbackReqRes.status !== 201) {
      success = false;
      return;
    }

    if (ENABLE_FEEDBACK_POLL && !pollCompletedFeedback(state, sessionId, flowName)) {
      success = false;
    }
  } catch (e) {
    success = false;
    console.error(`[${realFlowConfig.scenarioName}] unexpected error: ${String(e)}`);
  } finally {
    realCycleFailed.add(!success);
    realCycleDuration.add(Date.now() - startedAt);
  }
}

export default function () {
  realCycle();
}
