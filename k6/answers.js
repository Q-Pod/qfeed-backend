/**
 * Answer API 부하 테스트 (AI 호출 제외)
 *
 * 대상 엔드포인트:
 * - GET  /api/answers (Auth 필요)
 * - GET  /api/answers/{answerId} (Auth 필요)
 * - POST /api/interview/answers (Auth 필요)
 * - POST /api/interview/sessions (Auth 필요)
 * - POST /api/answers/real (Auth 필요, 반복 루프)
 * - POST /api/interview/sessions/feedback/request (Auth 필요)
 * - GET  /api/interviews/answers/{answerId}/feedback (Auth 필요)
 *
 * 참고: 모든 엔드포인트가 인증 필요
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { FormData } from 'https://jslib.k6.io/formdata/0.0.2/index.js';
import { BASE_URL, getHeaders, getMultipartHeaders, defaultOptions, randomString } from './config.js';

export const options = defaultOptions;

// Custom metrics
const answerErrorRate = new Rate('answer_error_rate');
const answerDuration = new Trend('answer_duration');

// 테스트용 ID (실제 환경에서는 환경 변수로 설정)
const TEST_ANSWER_ID = __ENV.TEST_ANSWER_ID || 1;
const TEST_QUESTION_ID = __ENV.TEST_QUESTION_ID || 1;

// ACCESS_TOKEN 환경 변수
const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';

function getAuthHeaders() {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };
    if (ACCESS_TOKEN) {
        headers['Authorization'] = `Bearer ${ACCESS_TOKEN}`;
    }
    return headers;
}

export default function () {
    group('Answer API', function () {
        // GET /api/answers (목록 조회)
        group('Get Answers List', function () {
            const res = http.get(
                `${BASE_URL}/api/answers?limit=10`,
                { headers: getAuthHeaders() }
            );

            let success;
            if (ACCESS_TOKEN) {
                success = check(res, {
                    'answers list returns 200': (r) => r.status === 200,
                    'answers list has data': (r) => {
                        try {
                            const body = JSON.parse(r.body);
                            return body.data !== undefined;
                        } catch (e) {
                            return false;
                        }
                    },
                });
            } else {
                // 인증 없이 요청 시 401 예상
                success = check(res, {
                    'answers list returns 401 without auth': (r) => r.status === 401,
                });
            }

            answerErrorRate.add(!success);
            answerDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // GET /api/answers/{answerId} (상세 조회)
        group('Get Answer Detail', function () {
            const res = http.get(
                `${BASE_URL}/api/answers/${TEST_ANSWER_ID}`,
                { headers: getAuthHeaders() }
            );

            let success;
            if (ACCESS_TOKEN) {
                success = check(res, {
                    'answer detail returns 200 or 404': (r) => r.status === 200 || r.status === 404,
                });
            } else {
                success = check(res, {
                    'answer detail returns 401 without auth': (r) => r.status === 401,
                });
            }

            answerErrorRate.add(!success);
            answerDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // GET /api/interviews/answers/{answerId}/feedback
        group('Get Answer Feedback', function () {
            const res = http.get(
                `${BASE_URL}/api/interviews/answers/${TEST_ANSWER_ID}/feedback`,
                { headers: getAuthHeaders() }
            );

            let success;
            if (ACCESS_TOKEN) {
                success = check(res, {
                    'feedback returns 200, 202, or 404': (r) =>
                        r.status === 200 || r.status === 202 || r.status === 404,
                });
            } else {
                success = check(res, {
                    'feedback returns 401 without auth': (r) => r.status === 401,
                });
            }

            answerErrorRate.add(!success);
            answerDuration.add(res.timings.duration);
        });

        sleep(1);
    });
}

export function submitTests() {
    const authHeaders = {
        'Accept': 'application/json',
        'Authorization': `Bearer ${ACCESS_TOKEN}`,
    };
    const jsonHeaders = Object.assign({}, authHeaders, { 'Content-Type': 'application/json' });

    group('Answer Submit Operations', function () {
        // POST /api/interview/answers (텍스트 답변 제출 — 비세션)
        group('Submit Text Answer', function () {
            const fd = new FormData();
            fd.append('questionId', TEST_QUESTION_ID.toString());
            fd.append('answerText', `K6 부하 테스트 답변입니다. ${randomString(50)}`);
            fd.append('answerType', 'TEXT');

            const res = http.post(
                `${BASE_URL}/api/interview/answers`,
                fd.body(),
                {
                    headers: Object.assign({}, authHeaders, {
                        'Content-Type': `multipart/form-data; boundary=${fd.boundary}`,
                    }),
                }
            );

            const success = check(res, {
                'submit answer returns 201': (r) => r.status === 201,
            });

            answerErrorRate.add(!success);
            answerDuration.add(res.timings.duration);
        });

        sleep(1);

        // REAL 인터뷰 세션 반복 플로우: 세션 생성 → 답변/질문 루프 → AI 최종 피드백
        group('Submit Session Answer', function () {
            // 1. 세션 생성 (REAL_INTERVIEW) — 첫 질문 포함
            const sessionRes = http.post(
                `${BASE_URL}/api/interview/sessions`,
                JSON.stringify({ interviewType: 'REAL_INTERVIEW', questionType: 'CS' }),
                { headers: jsonHeaders }
            );

            const sessionCreated = check(sessionRes, {
                'session created returns 200 or 201': (r) => r.status === 200 || r.status === 201,
            });

            answerErrorRate.add(!sessionCreated);
            answerDuration.add(sessionRes.timings.duration);

            if (!sessionCreated) {
                return;
            }

            const sessionData = JSON.parse(sessionRes.body).data;
            const sessionId = sessionData.session_id;
            let currentQuestion = sessionData.question_text;

            // 2. 답변-질문 반복 루프 (최대 MAX_TURNS번 또는 is_final 시 종료)
            const MAX_TURNS = 9;
            for (let turn = 0; turn < MAX_TURNS; turn++) {
                sleep(1);

                const answerRes = http.post(
                    `${BASE_URL}/api/answers/real`,
                    JSON.stringify({
                        sessionId: sessionId,
                        answerText: `K6 부하 테스트 ${turn + 1}번 답변 ${randomString(30)}`,
                        question: currentQuestion,
                        questionType: 'CS',
                    }),
                    { headers: jsonHeaders }
                );

                const answerSuccess = check(answerRes, {
                    'session answer returns 200 or 201': (r) => r.status === 200 || r.status === 201,
                });

                answerErrorRate.add(!answerSuccess);
                answerDuration.add(answerRes.timings.duration);

                if (!answerSuccess) {
                    break;
                }

                const answerBody = JSON.parse(answerRes.body).data;

                if (answerBody.is_final || !answerBody.next_question) {
                    break;
                }

                currentQuestion = answerBody.next_question.content;
            }

            // 3. AI 최종 피드백 요청
            sleep(1);

            const feedbackRes = http.post(
                `${BASE_URL}/api/interview/sessions/feedback/request`,
                JSON.stringify({ sessionId: sessionId }),
                { headers: jsonHeaders }
            );

            const feedbackSuccess = check(feedbackRes, {
                'final feedback returns 200 or 202': (r) => r.status === 200 || r.status === 202,
            });

            answerErrorRate.add(!feedbackSuccess);
            answerDuration.add(feedbackRes.timings.duration);
        });
    });
}
