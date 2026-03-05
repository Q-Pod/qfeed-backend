/**
 * Practice Flow (연습모드 단독)
 *
 * 실행 예시:
 * - k6 run k6/userflow/practiceflow.js --env BASE_URL=http://localhost:8080 --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv --env PRACTICE_RATE=6
 *
 * 주요 환경변수:
 * - BASE_URL: API 서버 주소 (기본: http://localhost:8080)
 * - USER_TOKEN_CSV: 유저 토큰 CSV 경로 (user_id/access_token/refresh_token 컬럼)
 * - DURATION: 시나리오 실행 시간 (기본: 5m)
 * - PRACTICE_RATE: 초당 연습 사이클 시작 수 (기본: 6)
 * - PRACTICE_PRE_ALLOCATED_VUS: 초기 VU 수 (기본: 20)
 * - PRACTICE_MAX_VUS: 최대 VU 수 (기본: 60)
 * - ENABLE_HOME_CALLS: 홈 공통 호출(주간통계/카테고리/추천) 포함 여부 (기본: true)
 * - ENABLE_FEEDBACK_POLL: 최종 피드백 폴링 여부 (기본: false)
 * - MOCK_AI: AI 호출(STT/TTS) mock 여부 (기본: true)
 * - PRACTICE_USE_CURSOR_RATE: 다음 페이지 조회 수행 확률 0~1 (기본: 0.2)
 * - PRACTICE_SEARCH_RATE: 검색 호출 수행 확률 0~1 (기본: 0.2)
 * - PRACTICE_RECOMMEND_ENTRY_RATE: 추천 질문 상세 진입 확률 0~1 (기본: 0)
 * - PRACTICE_CONTEXT_MISS_RATE: 질문 상세 재조회(컨텍스트 미스) 수행 확률 0~1 (기본: 0.1)
 * - PRACTICE_VOICE_RATE: 음성 경로 수행 확률 0~1 (기본: 1.0)
 * - MOCK_S3_UPLOAD: S3 PUT 외부 호출 mock 사용 여부 (기본: true)
 * - MOCK_S3_LATENCY_SEC: mock S3 업로드 지연(초) (기본: 0.03)
 * - ALLOW_STT_FALLBACK: STT 실패 시 텍스트 fallback 허용 여부 (기본: true)
 */
import exec from "k6/execution";
import { Rate, Trend } from "k6/metrics";
import {
  DURATION,
  DEFAULT_ANSWER_TEXT,
  chooseQuestionType,
  callHomeCommon,
  envBoolean,
  envNumber,
  getOrInitState,
  parseJson,
  pickQuestionFromList,
  pickQuestionIdFromDetail,
  pickQuestionTypeFromDetail,
  pickSessionId,
  pollCompletedFeedback,
  randomChance,
  requestAudioUploadFlow,
  requestWithAuth,
  setupTokenRows,
} from "./flow_utils.js";

const SCENARIO_PRACTICE = "practice_cycle";

const PRACTICE_RATE = envNumber("PRACTICE_RATE", 6);
const PRACTICE_PRE_ALLOCATED_VUS = envNumber("PRACTICE_PRE_ALLOCATED_VUS", 20);
const PRACTICE_MAX_VUS = envNumber("PRACTICE_MAX_VUS", 60);
const PRACTICE_USER_SPAN = PRACTICE_RATE > 0 ? Math.max(1, PRACTICE_MAX_VUS) : 0;

const ENABLE_HOME_CALLS = envBoolean("ENABLE_HOME_CALLS", true);
const ENABLE_FEEDBACK_POLL = envBoolean("ENABLE_FEEDBACK_POLL", false);

const PRACTICE_USE_CURSOR_RATE = envNumber("PRACTICE_USE_CURSOR_RATE", 0.2);
const PRACTICE_SEARCH_RATE = envNumber("PRACTICE_SEARCH_RATE", 0.2);
const PRACTICE_RECOMMEND_ENTRY_RATE = envNumber("PRACTICE_RECOMMEND_ENTRY_RATE", 0);
const PRACTICE_CONTEXT_MISS_RATE = envNumber("PRACTICE_CONTEXT_MISS_RATE", 0.1);
const PRACTICE_VOICE_RATE = envNumber("PRACTICE_VOICE_RATE", 1);

const MOCK_S3_UPLOAD = envBoolean("MOCK_S3_UPLOAD", true);
const MOCK_S3_LATENCY_SEC = envNumber("MOCK_S3_LATENCY_SEC", 0.03);
const ALLOW_STT_FALLBACK = envBoolean("ALLOW_STT_FALLBACK", true);

const SEARCH_KEYWORDS = [
  "운영체제",
  "네트워크",
  "데이터베이스",
  "시스템 디자인",
  "트랜잭션",
];

const practiceCycleFailed = new Rate("practice_cycle_failed");
const practiceCycleDuration = new Trend("practice_cycle_duration_ms");

const standaloneScenarios = {};
if (PRACTICE_RATE > 0) {
  standaloneScenarios[SCENARIO_PRACTICE] = {
    executor: "constant-arrival-rate",
    exec: "practiceCycle",
    rate: PRACTICE_RATE,
    timeUnit: "1s",
    duration: DURATION,
    preAllocatedVUs: PRACTICE_PRE_ALLOCATED_VUS,
    maxVUs: PRACTICE_MAX_VUS,
  };
}

export const options = {
  scenarios: standaloneScenarios,
  thresholds: {
    http_req_failed: ["rate<0.10"],
    practice_cycle_failed: ["rate<0.20"],
    practice_cycle_duration_ms: ["p(95)<15000"],
  },
};

const vuStateByScenario = {};

export function setup() {
  if (PRACTICE_RATE <= 0) {
    throw new Error("set PRACTICE_RATE greater than zero");
  }

  const requiredUsers = PRACTICE_USER_SPAN || Math.max(1, PRACTICE_MAX_VUS);
  setupTokenRows(
    requiredUsers,
    `practiceflow practiceRate=${PRACTICE_RATE}, practiceMaxVUs=${PRACTICE_MAX_VUS}`
  );
}

export function practiceCycle() {
  const startedAt = Date.now();
  let success = true;

  try {
    const flowName = SCENARIO_PRACTICE;
    const state = getOrInitState(vuStateByScenario, flowName, PRACTICE_USER_SPAN || 1, 0);
    const questionType = chooseQuestionType(exec.vu.iterationInScenario);

    if (ENABLE_HOME_CALLS) {
      const home = callHomeCommon(state, flowName);
      if (!home.ok) {
        success = false;
        return;
      }
    }

    const categoriesRes = requestWithAuth({
      state,
      method: "GET",
      path: "/api/questions/categories",
      tags: { flow: flowName, step: "practice_categories" },
    });
    if (categoriesRes.status !== 200) {
      success = false;
      return;
    }
    const categoriesJson = parseJson(categoriesRes);
    const selectedCategory = pickCategoryForType(categoriesJson, questionType);

    const typesRes = requestWithAuth({
      state,
      method: "GET",
      path: "/api/questions/types",
      tags: { flow: flowName, step: "practice_types" },
    });
    if (typesRes.status !== 200) {
      success = false;
      return;
    }

    const questionListRes = requestWithAuth({
      state,
      method: "GET",
      path: buildQuestionListPath({
        type: questionType,
        category: selectedCategory,
        size: 10,
      }),
      tags: { flow: flowName, step: "practice_questions" },
    });
    if (questionListRes.status !== 200) {
      success = false;
      return;
    }

    const questionListJson = parseJson(questionListRes);
    let selectedQuestion = pickQuestionFromList(questionListJson, exec.vu.iterationInScenario);
    if (!selectedQuestion) {
      success = false;
      return;
    }

    if (randomChance(PRACTICE_USE_CURSOR_RATE)) {
      const pagination = questionListJson?.data?.pagination || {};
      const hasNext = Boolean(pagination.hasNext ?? pagination.has_next);
      const nextCursor = pagination.nextCursor ?? pagination.next_cursor;
      if (hasNext && nextCursor != null) {
        requestWithAuth({
          state,
          method: "GET",
          path: buildQuestionListPath({
            type: questionType,
            category: selectedCategory,
            size: 10,
            cursor: nextCursor,
          }),
          tags: { flow: flowName, step: "practice_questions_next" },
        });
      }
    }

    if (randomChance(PRACTICE_SEARCH_RATE)) {
      const keyword = SEARCH_KEYWORDS[Math.abs(exec.vu.iterationInScenario) % SEARCH_KEYWORDS.length];
      const searchRes = requestWithAuth({
        state,
        method: "GET",
        path: buildQuestionSearchPath({
          q: keyword,
          type: questionType,
          category: selectedCategory,
          size: 10,
        }),
        tags: { flow: flowName, step: "practice_search" },
      });
      if (searchRes.status === 200) {
        const searchJson = parseJson(searchRes);
        const searched = pickQuestionFromList(searchJson, exec.vu.iterationInScenario + 1);
        if (searched) {
          selectedQuestion = searched;
        }
      }
    }

    if (randomChance(PRACTICE_RECOMMEND_ENTRY_RATE)) {
      const recommendationRes = requestWithAuth({
        state,
        method: "GET",
        path: "/api/questions/recommendation",
        tags: { flow: flowName, step: "practice_recommendation_entry" },
      });

      if (recommendationRes.status === 200) {
        const recommendationJson = parseJson(recommendationRes);
        const recommendationQuestionId = pickQuestionIdFromDetail(recommendationJson);
        const recommendationType = pickQuestionTypeFromDetail(recommendationJson);

        if (
          recommendationQuestionId &&
          (!recommendationType || String(recommendationType) === String(questionType))
        ) {
          const detailRes = requestWithAuth({
            state,
            method: "GET",
            path: `/api/questions/${recommendationQuestionId}`,
            tags: { flow: flowName, step: "practice_question_detail" },
          });
          if (detailRes.status === 200) {
            selectedQuestion = {
              questionId: recommendationQuestionId,
              questionType: recommendationType || questionType,
            };
          }
        }
      }
    }

    if (randomChance(PRACTICE_CONTEXT_MISS_RATE)) {
      requestWithAuth({
        state,
        method: "GET",
        path: `/api/questions/${selectedQuestion.questionId}`,
        tags: { flow: flowName, step: "practice_context_miss_detail" },
      });
    }

    let answerText = `${DEFAULT_ANSWER_TEXT} [practice][q=${selectedQuestion.questionId}]`;
    const useVoicePath = randomChance(PRACTICE_VOICE_RATE);
    if (useVoicePath) {
      const answerResult = requestAudioUploadFlow({
        state,
        flowName,
        sessionId: null,
        mockS3Upload: MOCK_S3_UPLOAD,
        mockS3LatencySec: MOCK_S3_LATENCY_SEC,
        allowSttFallback: ALLOW_STT_FALLBACK,
        fallbackAnswerText: answerText,
      });
      if (!answerResult.ok) {
        success = false;
        return;
      }
      answerText = answerResult.answerText || answerText;
    }

    const createSessionRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/interview/sessions",
      body: {
        interviewType: "PRACTICE_INTERVIEW",
        questionType,
      },
      tags: { flow: flowName, step: "create_session" },
    });
    if (createSessionRes.status !== 201) {
      success = false;
      return;
    }

    const createSessionJson = parseJson(createSessionRes);
    const sessionId = pickSessionId(createSessionJson);
    if (!sessionId) {
      success = false;
      return;
    }

    const submitPracticeRes = requestWithAuth({
      state,
      method: "POST",
      path: "/api/answers/practice",
      body: {
        sessionId,
        questionId: selectedQuestion.questionId,
        answerText,
      },
      tags: { flow: flowName, step: "submit_practice" },
    });
    if (submitPracticeRes.status !== 201) {
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
    console.error(`[${SCENARIO_PRACTICE}] unexpected error: ${String(e)}`);
  } finally {
    practiceCycleFailed.add(!success);
    practiceCycleDuration.add(Date.now() - startedAt);
  }
}

export function teardown() {
  console.log("[teardown] practice flow test finished");
}

export default function () {
  practiceCycle();
}

function buildQuestionListPath({ type, category, size, cursor }) {
  const parts = [`type=${encodeURIComponent(type)}`, `size=${encodeURIComponent(size)}`];
  if (category) {
    parts.push(`category=${encodeURIComponent(category)}`);
  }
  if (cursor != null && String(cursor).length > 0) {
    parts.push(`cursor=${encodeURIComponent(cursor)}`);
  }
  return `/api/questions?${parts.join("&")}`;
}

function buildQuestionSearchPath({ q, type, category, size, cursor }) {
  const parts = [
    `q=${encodeURIComponent(q)}`,
    `type=${encodeURIComponent(type)}`,
    `size=${encodeURIComponent(size)}`,
  ];
  if (category) {
    parts.push(`category=${encodeURIComponent(category)}`);
  }
  if (cursor != null && String(cursor).length > 0) {
    parts.push(`cursor=${encodeURIComponent(cursor)}`);
  }
  return `/api/questions/search?${parts.join("&")}`;
}

function pickCategoryForType(categoriesJson, questionType) {
  const categories = categoriesJson?.data?.categories;
  if (!categories || typeof categories !== "object") {
    return null;
  }

  const byType = categories?.[String(questionType)] || categories?.[String(questionType).toUpperCase()];
  if (byType && typeof byType === "object") {
    const keys = Object.keys(byType);
    if (keys.length > 0) {
      return keys[0];
    }
  }

  const topLevelKeys = Object.keys(categories);
  for (let i = 0; i < topLevelKeys.length; i += 1) {
    const nested = categories[topLevelKeys[i]];
    if (nested && typeof nested === "object") {
      const nestedKeys = Object.keys(nested);
      if (nestedKeys.length > 0) {
        return nestedKeys[0];
      }
    }
  }

  return null;
}
