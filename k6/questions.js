/**
 * Question API 부하 테스트
 *
 * 대상 엔드포인트:
 * - GET    /api/questions
 * - GET    /api/questions/categories
 * - GET    /api/questions/types
 * - GET    /api/questions/{questionId}
 * - GET    /api/questions/search?q=keyword
 * - GET    /api/questions/recommendation
 * - POST   /api/questions
 * - PATCH  /api/questions/{questionId}
 * - DELETE /api/questions/{questionId}
 * - GET    /api/questions/{questionId}/keywords
 * - POST   /api/questions/{questionId}/keyword-checks
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, getHeaders, defaultOptions, randomInt, randomString } from './config.js';

export const options = defaultOptions;

// Custom metrics
const questionErrorRate = new Rate('question_error_rate');
const questionDuration = new Trend('question_duration');
const questionsCreated = new Counter('questions_created');

// 테스트용 질문 ID (실제 환경에서는 환경 변수로 설정)
const TEST_QUESTION_ID = __ENV.TEST_QUESTION_ID || 1;

// 카테고리 및 타입 enum 값
const CATEGORIES = ['OS', 'NETWORK', 'DB', 'COMPUTER_ARCHITECTURE', 'DATA_STRUCTURE_ALGORITHM'];
const TYPES = ['CS', 'SYSTEM_DESIGN', 'PORTFOLIO'];

export default function () {
    group('Question API', function () {
        // GET /api/questions/categories
        group('Get Categories', function () {
            const res = http.get(
                `${BASE_URL}/api/questions/categories`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'categories returns 200': (r) => r.status === 200,
                'categories has data': (r) => {
                    const body = JSON.parse(r.body);
                    return body.data && body.data.categories;
                },
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // GET /api/questions/types
        group('Get Types', function () {
            const res = http.get(
                `${BASE_URL}/api/questions/types`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'types returns 200': (r) => r.status === 200,
                'types has data': (r) => {
                    const body = JSON.parse(r.body);
                    return body.data && body.data.types;
                },
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // GET /api/questions (목록 조회)
        group('Get Questions List', function () {
            const category = CATEGORIES[randomInt(0, CATEGORIES.length - 1)];
            const res = http.get(
                `${BASE_URL}/api/questions?category=${category}&size=10`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'questions list returns 200': (r) => r.status === 200,
                'questions list has data': (r) => {
                    const body = JSON.parse(r.body);
                    return body.data !== undefined;
                },
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // GET /api/questions/{questionId} (상세 조회)
        group('Get Question Detail', function () {
            const res = http.get(
                `${BASE_URL}/api/questions/${TEST_QUESTION_ID}`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'question detail returns 200 or 404': (r) => r.status === 200 || r.status === 404,
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // GET /api/questions/search
        group('Search Questions', function () {
            const keywords = ['자바', 'Spring', 'REST', 'API', 'JPA', '객체지향'];
            const keyword = keywords[randomInt(0, keywords.length - 1)];

            const res = http.get(
                `${BASE_URL}/api/questions/search?q=${encodeURIComponent(keyword)}&size=10`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'search returns 200': (r) => r.status === 200,
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // GET /api/questions/recommendation
        group('Get Daily Recommendation', function () {
            const res = http.get(
                `${BASE_URL}/api/questions/recommendation`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'recommendation returns 200 or 404': (r) => r.status === 200 || r.status === 404,
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // GET /api/questions/{questionId}/keywords
        group('Get Question Keywords', function () {
            const res = http.get(
                `${BASE_URL}/api/questions/${TEST_QUESTION_ID}/keywords`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'keywords returns 200 or 404': (r) => r.status === 200 || r.status === 404,
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // POST /api/questions/{questionId}/keyword-checks
        group('Check Question Keywords', function () {
            const payload = JSON.stringify({
                keywords: ['자바', 'Spring', 'JPA'],
            });

            const res = http.post(
                `${BASE_URL}/api/questions/${TEST_QUESTION_ID}/keyword-checks`,
                payload,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'keyword-checks returns 200 or 404': (r) => r.status === 200 || r.status === 404,
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(1);
    });
}

/**
 * 쓰기 작업 테스트 (별도 실행 권장)
 * 데이터 변경이 발생하므로 주의 필요
 */
export function writeTests() {
    group('Question Write Operations', function () {
        // POST /api/questions (생성)
        group('Create Question', function () {
            const payload = JSON.stringify({
                content: `K6 부하 테스트 질문 ${randomString(8)}`,
                category: CATEGORIES[randomInt(0, CATEGORIES.length - 1)],
                type: TYPES[randomInt(0, TYPES.length - 1)],
                keywords: ['테스트', 'K6'],
            });

            const res = http.post(
                `${BASE_URL}/api/questions`,
                payload,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'create question returns 201': (r) => r.status === 201,
            });

            if (success) {
                questionsCreated.add(1);
            }
            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);

            // 생성된 질문 ID 반환 (다음 테스트에서 사용)
            if (res.status === 201) {
                try {
                    const body = JSON.parse(res.body);
                    return body.data?.id;
                } catch (e) {
                    return null;
                }
            }
        });

        sleep(0.5);

        // PATCH /api/questions/{questionId} (수정)
        group('Update Question', function () {
            const payload = JSON.stringify({
                content: `K6 수정된 질문 ${randomString(8)}`,
            });

            const res = http.patch(
                `${BASE_URL}/api/questions/${TEST_QUESTION_ID}`,
                payload,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'update question returns 200 or 404': (r) => r.status === 200 || r.status === 404,
            });

            questionErrorRate.add(!success);
            questionDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // DELETE /api/questions/{questionId} (삭제 - 주의!)
        // 실제 데이터 삭제를 방지하기 위해 기본적으로 비활성화
        if (__ENV.ENABLE_DELETE_TEST === 'true') {
            group('Delete Question', function () {
                const res = http.del(
                    `${BASE_URL}/api/questions/${TEST_QUESTION_ID}`,
                    null,
                    { headers: getHeaders() }
                );

                const success = check(res, {
                    'delete question returns 200 or 404': (r) => r.status === 200 || r.status === 404,
                });

                questionErrorRate.add(!success);
                questionDuration.add(res.timings.duration);
            });
        }
    });
}
