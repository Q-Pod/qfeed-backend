/**
 * 인증된 사용자 루프 시나리오 (재사용 가능 모듈)
 *
 * stress-authenticated-loop.js, soak-authenticated-loop.js 에서 import 하여 사용.
 *
 * 흐름:
 *   홈(categories + recommendation + questions list)
 *   → 연습(2-1): 질문 상세 → 텍스트 답변 제출
 *   → 홈 복귀: questions list
 *   → 실전(2-2): 세션 생성 → 답변 루프 → AI 피드백 요청
 *   → 프로필(3-1): 내 답변 목록 → 제출 건 검증 → 최근 상세 → 학습 통계
 *   → 반복
 *
 * 환경변수:
 *   ACCESS_TOKEN         JWT Bearer 토큰 (LOAD_TEST_USER_ID 미사용 시 필수)
 *   LOAD_TEST_USER_ID    로드테스트 전용 헤더 인증 (ACCESS_TOKEN 대안)
 *   TEST_QUESTION_ID     연습용 질문 ID 폴백 (기본: 1)
 *   MAX_REAL_TURNS       실전 답변 루프 최대 횟수 (기본: 3)
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';
import {
    BASE_URL,
    ACCESS_TOKEN,
    LOAD_TEST_USER_ID,
    getHeaders,
    getMultipartHeaders,
    parseJsonResponse,
    randomInt,
    randomString,
} from '../config.js';

// ===== Fail-fast: 인증 미설정 시 테스트 시작 전에 중단 =====
if (!ACCESS_TOKEN && !LOAD_TEST_USER_ID) {
    throw new Error(
        '\n[FAIL-FAST] 인증 설정이 없습니다. 다음 중 하나를 설정하세요:\n' +
        '  k6 run --env ACCESS_TOKEN=<JWT토큰> ...\n' +
        '  k6 run --env LOAD_TEST_USER_ID=<사용자ID> ...\n'
    );
}

// ===== 환경변수 =====
const TEST_QUESTION_ID = parseInt(__ENV.TEST_QUESTION_ID || '1', 10);
const MAX_REAL_TURNS = parseInt(__ENV.MAX_REAL_TURNS || '3', 10);

// ===== 커스텀 메트릭 =====
// stress/soak 스크립트에서 threshold 설정에 사용
export const flowSuccessRate = new Rate('flow_success_rate');
export const flowDuration = new Trend('flow_duration');
export const profileVerificationSuccessRate = new Rate('profile_verification_success_rate');

// ===== 내부 헬퍼 =====

/** 응답에서 data 필드 안전 추출 */
function extractData(res) {
    const body = parseJsonResponse(res);
    return body ? body.data : null;
}

/**
 * 다양한 응답 구조에서 배열 추출
 * - data: [...] (단순 배열)
 * - data: { answers: [...] }
 * - data: { content: [...] } (Spring Page)
 */
function toItemArray(data) {
    if (Array.isArray(data)) { return data; }
    if (data && Array.isArray(data.answers)) { return data.answers; }
    if (data && Array.isArray(data.content)) { return data.content; }
    return null;
}

/** 답변 ID 추출 (필드명 변형 대응) */
function toAnswerId(item) {
    return item.answerId || item.id || item.answer_id || null;
}

/** 질문 ID 추출 (필드명 변형 대응) */
function toQuestionId(item) {
    return item.questionId || item.id || item.question_id || null;
}

// ===== 단계 함수들 =====

/**
 * [홈] 카테고리 + 추천 + 질문 목록 조회
 * @returns {number} 다음 연습에 사용할 questionId
 */
function homePhase() {
    let questionId = TEST_QUESTION_ID;

    group('home', function () {
        // 1. 카테고리 목록
        const catRes = http.get(`${BASE_URL}/api/questions/categories`, {
            headers: getHeaders(),
            tags: { phase: 'home' },
        });
        check(catRes, {
            '[home] categories 200': (r) => r.status === 200,
        });
        flowDuration.add(catRes.timings.duration, { phase: 'home' });
        sleep(0.3);

        // 2. 오늘의 추천 질문
        const recRes = http.get(`${BASE_URL}/api/questions/recommendation`, {
            headers: getHeaders(),
            tags: { phase: 'home' },
        });
        check(recRes, {
            '[home] recommendation 200/404': (r) => r.status === 200 || r.status === 404,
        });
        flowDuration.add(recRes.timings.duration, { phase: 'home' });
        sleep(0.3);

        // 3. 질문 목록 — 첫 번째 질문 ID를 다음 연습에 사용
        const cats = ['OS', 'NETWORK', 'DB', 'COMPUTER_ARCHITECTURE', 'DATA_STRUCTURE_ALGORITHM'];
        const cat = cats[randomInt(0, cats.length - 1)];
        const listRes = http.get(
            `${BASE_URL}/api/questions?category=${cat}&size=10`,
            { headers: getHeaders(), tags: { phase: 'home' } }
        );
        const listOk = check(listRes, {
            '[home] questions list 200': (r) => r.status === 200,
        });
        flowDuration.add(listRes.timings.duration, { phase: 'home' });

        if (listOk) {
            const items = toItemArray(extractData(listRes));
            if (items && items.length > 0) {
                const id = toQuestionId(items[0]);
                if (id) { questionId = id; }
            }
        }
    });

    return questionId;
}

/**
 * [연습(2-1)] 질문 상세 확인 → 텍스트 답변 제출
 * @param {number} questionId
 * @returns {{ submittedAnswerId: number|null, startedAt: number }}
 */
function practicePhase(questionId) {
    let submittedAnswerId = null;
    const startedAt = Date.now();

    group('practice', function () {
        // 1. 질문 상세 조회 (연습 화면 진입)
        const detailRes = http.get(`${BASE_URL}/api/questions/${questionId}`, {
            headers: getHeaders(),
            tags: { phase: 'practice' },
        });
        check(detailRes, {
            '[practice] question detail 200/404': (r) => r.status === 200 || r.status === 404,
        });
        flowDuration.add(detailRes.timings.duration, { phase: 'practice' });
        sleep(0.5);

        // 2. 텍스트 답변 제출 (multipart/form-data)
        const fd = new FormData();
        fd.append('questionId', String(questionId));
        fd.append('answerText', `[K6-practice] ${randomString(30)}`);
        fd.append('answerType', 'TEXT');

        const submitRes = http.post(
            `${BASE_URL}/api/interview/answers`,
            fd.body(),
            {
                headers: Object.assign({}, getMultipartHeaders(true), {
                    'Content-Type': `multipart/form-data; boundary=${fd.boundary}`,
                }),
                tags: { phase: 'practice' },
            }
        );
        const submitOk = check(submitRes, {
            '[practice] answer submit 201': (r) => r.status === 201,
        });
        flowDuration.add(submitRes.timings.duration, { phase: 'practice' });
        flowSuccessRate.add(submitOk, { phase: 'practice' });

        if (!submitOk) {
            console.warn(
                `[practice] 답변 제출 실패 | ` +
                `status=${submitRes.status} | body=${submitRes.body.substring(0, 200)}`
            );
            return;
        }

        // 제출된 answerId 추출 (profile 검증에 사용)
        const d = extractData(submitRes);
        if (d) {
            submittedAnswerId = toAnswerId(d);
        }
    });

    return { submittedAnswerId, startedAt };
}

/**
 * [홈 복귀] 질문 목록 재조회
 */
function homeReturnPhase() {
    group('home', function () {
        const types = ['CS', 'SYSTEM_DESIGN', 'PORTFOLIO'];
        const listRes = http.get(
            `${BASE_URL}/api/questions?type=${types[randomInt(0, types.length - 1)]}&size=10`,
            { headers: getHeaders(), tags: { phase: 'home' } }
        );
        check(listRes, {
            '[home-return] questions 200': (r) => r.status === 200,
        });
        flowDuration.add(listRes.timings.duration, { phase: 'home' });
    });
    sleep(0.3);
}

/**
 * [실전(2-2)] 인터뷰 세션 생성 → 답변 루프 → AI 피드백 요청
 * @returns {string|null} sessionId (실패 시 null)
 */
function realInterviewPhase() {
    let sessionId = null;

    group('real', function () {
        // 1. 세션 생성
        const sessionRes = http.post(
            `${BASE_URL}/api/interview/sessions`,
            JSON.stringify({ interviewType: 'REAL_INTERVIEW', questionType: 'CS' }),
            { headers: getHeaders(true), tags: { phase: 'real' } }
        );
        const sessionOk = check(sessionRes, {
            '[real] session created 200/201': (r) => r.status === 200 || r.status === 201,
        });
        flowDuration.add(sessionRes.timings.duration, { phase: 'real' });
        flowSuccessRate.add(sessionOk, { phase: 'real' });

        if (!sessionOk) {
            console.warn(
                `[real] 세션 생성 실패 | ` +
                `status=${sessionRes.status} | body=${sessionRes.body.substring(0, 200)}`
            );
            return;
        }

        const sd = extractData(sessionRes);
        if (!sd) {
            console.warn('[real] 세션 응답 파싱 실패');
            return;
        }

        sessionId = sd.session_id || sd.sessionId || sd.id;
        let currentQuestion = sd.question_text || sd.questionText || '';

        // 2. 답변-질문 반복 루프 (최대 MAX_REAL_TURNS)
        for (let turn = 0; turn < MAX_REAL_TURNS; turn++) {
            sleep(0.5);

            const ansRes = http.post(
                `${BASE_URL}/api/answers/real`,
                JSON.stringify({
                    sessionId: sessionId,
                    answerText: `[K6-real-turn${turn + 1}] ${randomString(20)}`,
                    question: currentQuestion,
                    questionType: 'CS',
                }),
                { headers: getHeaders(true), tags: { phase: 'real' } }
            );
            const ansOk = check(ansRes, {
                [`[real] answer turn${turn + 1} 200/201`]: (r) => r.status === 200 || r.status === 201,
            });
            flowDuration.add(ansRes.timings.duration, { phase: 'real' });

            if (!ansOk) {
                console.warn(`[real] 답변 실패 | turn=${turn + 1} | status=${ansRes.status}`);
                break;
            }

            const ab = extractData(ansRes);
            if (!ab || ab.is_final || !ab.next_question) { break; }

            currentQuestion = (typeof ab.next_question === 'object')
                ? (ab.next_question.content || ab.next_question.text || '')
                : String(ab.next_question);
        }

        // 3. AI 최종 피드백 요청
        sleep(0.5);
        const fbRes = http.post(
            `${BASE_URL}/api/interview/sessions/feedback/request`,
            JSON.stringify({ sessionId: sessionId }),
            { headers: getHeaders(true), tags: { phase: 'real' } }
        );
        const fbOk = check(fbRes, {
            '[real] feedback request 200/202': (r) => r.status === 200 || r.status === 202,
        });
        flowDuration.add(fbRes.timings.duration, { phase: 'real' });
        flowSuccessRate.add(fbOk, { phase: 'real' });

        if (!fbOk) {
            console.warn(`[real] 피드백 요청 실패 | status=${fbRes.status}`);
        }
    });

    return sessionId;
}

/**
 * [프로필(3-1)] 내 답변 목록 → 제출 건 검증 → 최근 상세 → 학습 통계
 *
 * 검증 전략:
 *   1) submittedAnswerId 있으면: 목록에 해당 ID 포함 여부로 검증
 *   2) submittedAnswerId 없으면: 최신 답변 createdAt > 제출 시각으로 검증
 *   3) createdAt도 없으면: 목록 존재 여부만 확인 + 경고 로그
 *
 * @param {{ submittedAnswerId: number|null, startedAt: number }} practiceResult
 */
function profilePhase(practiceResult) {
    const { submittedAnswerId, startedAt } = practiceResult;

    group('profile', function () {
        // 1. 내 답변 목록 조회
        const listRes = http.get(`${BASE_URL}/api/answers?limit=10`, {
            headers: getHeaders(true),
            tags: { phase: 'profile' },
        });
        const listOk = check(listRes, {
            '[profile] answers list 200': (r) => r.status === 200,
            '[profile] answers list has data': (r) => {
                const b = parseJsonResponse(r);
                return b && b.data !== undefined;
            },
        });
        flowDuration.add(listRes.timings.duration, { phase: 'profile' });

        if (!listOk) {
            console.warn(`[profile] 목록 조회 실패 | status=${listRes.status}`);
            profileVerificationSuccessRate.add(false);
            return;
        }

        // 2. 최근 제출 건 검증
        const answers = toItemArray(extractData(listRes));

        if (!answers || answers.length === 0) {
            console.warn('[profile] 답변 목록 비어있음 → 검증 불가 (false positive 방지: 실패 처리)');
            profileVerificationSuccessRate.add(false);
            check(false, { '[profile] 제출 내역 검증': (v) => v });
            return;
        }

        let verified = false;

        if (submittedAnswerId) {
            // answerId 기반 검증 (가장 신뢰도 높음)
            verified = answers.some(a => String(toAnswerId(a)) === String(submittedAnswerId));
            if (!verified) {
                console.warn(
                    `[profile] 제출 answerId(${submittedAnswerId}) 미발견 → 검증 실패 ` +
                    '(false positive 방지: 명시적 실패 처리)'
                );
            }
        } else {
            // 시간 기반 검증 (answerId 미수집 시 폴백)
            const latest = answers[0];
            const createdAt = latest.createdAt || latest.created_at;
            if (createdAt) {
                verified = new Date(createdAt).getTime() >= startedAt;
                if (!verified) {
                    console.warn(
                        `[profile] 최신 답변(${createdAt}) < 제출 시각(${new Date(startedAt).toISOString()}) ` +
                        '→ 검증 실패 (answerId 미수집으로 시간 기반 폴백)'
                    );
                }
            } else {
                // createdAt 없으면 목록 존재만 확인하고 경고
                verified = true;
                console.warn('[profile] createdAt 필드 없음 → 목록 존재 여부만 확인 (부분 검증)');
            }
        }

        profileVerificationSuccessRate.add(verified);
        check(verified, {
            '[profile] 제출 내역 검증': (v) => v === true,
        });

        // 3. 최근 제출 상세 조회
        const latestId = toAnswerId(answers[0]);
        if (latestId) {
            sleep(0.3);
            const detailRes = http.get(`${BASE_URL}/api/answers/${latestId}`, {
                headers: getHeaders(true),
                tags: { phase: 'profile' },
            });
            check(detailRes, {
                '[profile] answer detail 200': (r) => r.status === 200 || r.status === 404,
            });
            flowDuration.add(detailRes.timings.duration, { phase: 'profile' });
        }

        // 4. 학습 통계
        sleep(0.3);
        const statsRes = http.get(`${BASE_URL}/api/users/me/stats`, {
            headers: getHeaders(true),
            tags: { phase: 'profile' },
        });
        check(statsRes, {
            '[profile] user stats 200': (r) => r.status === 200,
        });
        flowDuration.add(statsRes.timings.duration, { phase: 'profile' });
    });
}

// ===== 메인 루프 (export) =====

/**
 * 전체 사용자 루프 1회 실행
 *
 * stress/soak 스크립트의 default export 함수에서 이 함수를 호출한다.
 * 각 단계의 성공 여부를 종합하여 flow_success_rate를 기록한다.
 */
export function authenticatedUserLoop() {
    const loopStart = Date.now();
    let loopOk = true;

    // 홈
    const questionId = homePhase();
    sleep(0.5);

    // 연습(2-1)
    const practiceResult = practicePhase(questionId);
    if (!practiceResult.submittedAnswerId) {
        loopOk = false;
    }
    sleep(0.5);

    // 홈 복귀
    homeReturnPhase();
    sleep(0.3);

    // 실전(2-2)
    const sessionId = realInterviewPhase();
    if (!sessionId) {
        loopOk = false;
    }
    sleep(1);

    // 프로필(3-1)
    profilePhase(practiceResult);

    // 루프 전체 메트릭
    flowDuration.add(Date.now() - loopStart, { phase: 'loop' });
    flowSuccessRate.add(loopOk, { phase: 'loop' });

    if (!loopOk) {
        console.warn(`[loop] 루프 실패 | duration=${Date.now() - loopStart}ms`);
    }

    sleep(randomInt(1, 3));
}
