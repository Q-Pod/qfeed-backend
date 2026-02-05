/**
 * Answer API 부하 테스트 (AI 호출 제외)
 *
 * 대상 엔드포인트:
 * - GET  /api/answers (Auth 필요)
 * - GET  /api/answers/{answerId} (Auth 필요)
 * - POST /api/interview/answers (Auth 필요)
 * - POST /api/interview/sessions/{sessionId}/answers (Auth 필요)
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
const TEST_SESSION_ID = __ENV.TEST_SESSION_ID || 'test-session-id';

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

/**
 * 답변 제출 테스트 (별도 실행 권장)
 * 인증 토큰 필요, 데이터 생성이 발생함
 */
export function submitTests() {
    if (!ACCESS_TOKEN) {
        console.log('Skipping submit tests: ACCESS_TOKEN not set');
        return;
    }

    const authHeaders = {
        'Accept': 'application/json',
        'Authorization': `Bearer ${ACCESS_TOKEN}`,
    };

    group('Answer Submit Operations', function () {
        // POST /api/interview/answers (텍스트 답변 제출)
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

        // POST /api/interview/sessions/{sessionId}/answers (세션 기반 답변)
        group('Submit Session Answer', function () {
            const fd = new FormData();
            fd.append('questionId', TEST_QUESTION_ID.toString());
            fd.append('answerText', `K6 세션 답변 테스트 ${randomString(50)}`);

            const res = http.post(
                `${BASE_URL}/api/interview/sessions/${TEST_SESSION_ID}/answers`,
                fd.body(),
                {
                    headers: Object.assign({}, authHeaders, {
                        'Content-Type': `multipart/form-data; boundary=${fd.boundary}`,
                    }),
                }
            );

            // 세션이 없거나 구현이 완료되지 않은 경우 다양한 응답 가능
            const success = check(res, {
                'session answer returns expected status': (r) =>
                    r.status === 201 || r.status === 404 || r.status === 409,
            });

            answerErrorRate.add(!success);
            answerDuration.add(res.timings.duration);
        });
    });
}
