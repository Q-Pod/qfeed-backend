/**
 * 전체 API 통합 부하 테스트
 *
 * 모든 API를 시나리오 기반으로 테스트합니다.
 * AI 호출 관련 엔드포인트는 제외됩니다.
 *
 * 사용법:
 *   k6 run k6/all.js
 *   k6 run --env BASE_URL=http://localhost:8080 k6/all.js
 *   k6 run --env ACCESS_TOKEN=your_token k6/all.js
 *   k6 run --out json=results.json k6/all.js
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, getHeaders, randomInt, randomString } from './config.js';

// 시나리오 기반 부하 설정
export const options = {
    scenarios: {
        // 읽기 위주 시나리오 (대부분의 트래픽)
        read_heavy: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 20 },
                { duration: '1m', target: 50 },
                { duration: '30s', target: 100 },
                { duration: '1m', target: 50 },
                { duration: '30s', target: 0 },
            ],
            exec: 'readOperations',
        },
        // 쓰기 시나리오 (낮은 비율)
        write_operations: {
            executor: 'ramping-vus',
            startVUs: 0,
            stages: [
                { duration: '30s', target: 5 },
                { duration: '2m', target: 10 },
                { duration: '30s', target: 0 },
            ],
            exec: 'writeOperations',
            startTime: '30s',  // 읽기 테스트 시작 후 30초 뒤 시작
        },
        // 인증 시나리오
        // auth_flow: {
        //     executor: 'constant-vus',
        //     vus: 5,
        //     duration: '3m',
        //     exec: 'authOperations',
        // },
    },
    thresholds: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],
        http_req_failed: ['rate<0.01'],
        'http_req_duration{scenario:read_heavy}': ['p(95)<300'],
        'http_req_duration{scenario:write_operations}': ['p(95)<1000'],
    },
};

// Custom metrics
const errorRate = new Rate('overall_error_rate');
const duration = new Trend('overall_duration');
const requestCounter = new Counter('total_requests');

const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';
const TEST_QUESTION_ID = __ENV.TEST_QUESTION_ID || 1;
const TEST_METRIC_ID = __ENV.TEST_METRIC_ID || 1;

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

/**
 * 읽기 작업 시나리오
 */
export function readOperations() {
    const operations = [
        questionsList,
        questionDetail,
        questionCategories,
        questionTypes,
        questionSearch,
        questionRecommendation,
        questionKeywords,
        metricsList,
        metricDetail,
    ];

    // 랜덤하게 작업 선택
    const op = operations[randomInt(0, operations.length - 1)];
    op();

    // 인증이 필요한 작업
    if (ACCESS_TOKEN) {
        const authOps = [answersList, answerDetail, userStats, userWeeklyStats];
        const authOp = authOps[randomInt(0, authOps.length - 1)];
        authOp();
    }

    sleep(randomInt(1, 3) * 0.1);
}

/**
 * 쓰기 작업 시나리오
 */
export function writeOperations() {
    const operations = [
        createQuestion,
        createMetric,
        generatePresignedUrl,
    ];

    const op = operations[randomInt(0, operations.length - 1)];
    op();

    sleep(randomInt(5, 10) * 0.1);
}

/**
 * 인증 관련 시나리오
 */
export function authOperations() {
    group('Auth Flow', function () {
        // Authorization URL 조회
        authorizationUrl();
        sleep(0.5);

        // Token refresh (쿠키 없이)
        tokenRefresh();
        sleep(0.5);

        // Exchange (잘못된 코드)
        exchangeCode();
        sleep(1);
    });
}

// ========== Question Operations ==========

function questionsList() {
    group('Questions List', function () {
        const categories = ['OS', 'NETWORK', 'DB', 'COMPUTER_ARCHITECTURE', 'DATA_STRUCTURE_ALGORITHM'];
        const category = categories[randomInt(0, categories.length - 1)];

        const res = http.get(
            `${BASE_URL}/api/questions?category=${category}&size=10`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'questions list 200': (r) => r.status === 200,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function questionDetail() {
    group('Question Detail', function () {
        const res = http.get(
            `${BASE_URL}/api/questions/${TEST_QUESTION_ID}`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'question detail 200/404': (r) => r.status === 200 || r.status === 404,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function questionCategories() {
    group('Question Categories', function () {
        const res = http.get(
            `${BASE_URL}/api/questions/categories`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'categories 200': (r) => r.status === 200,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function questionTypes() {
    group('Question Types', function () {
        const res = http.get(
            `${BASE_URL}/api/questions/types`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'types 200': (r) => r.status === 200,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function questionSearch() {
    group('Question Search', function () {
        const keywords = ['자바', 'Spring', 'REST', 'API', 'JPA'];
        const keyword = keywords[randomInt(0, keywords.length - 1)];

        const res = http.get(
            `${BASE_URL}/api/questions/search?q=${encodeURIComponent(keyword)}&size=10`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'search 200': (r) => r.status === 200,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function questionRecommendation() {
    group('Question Recommendation', function () {
        const res = http.get(
            `${BASE_URL}/api/questions/recommendation`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'recommendation 200/404': (r) => r.status === 200 || r.status === 404,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function questionKeywords() {
    group('Question Keywords', function () {
        const res = http.get(
            `${BASE_URL}/api/questions/${TEST_QUESTION_ID}/keywords`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'keywords 200/404': (r) => r.status === 200 || r.status === 404,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

// ========== Metric Operations ==========

function metricsList() {
    group('Metrics List', function () {
        const res = http.get(
            `${BASE_URL}/api/metrics?size=10`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'metrics list 200': (r) => r.status === 200,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function metricDetail() {
    group('Metric Detail', function () {
        const res = http.get(
            `${BASE_URL}/api/metrics/${TEST_METRIC_ID}`,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'metric detail 200/404': (r) => r.status === 200 || r.status === 404,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

// ========== Answer Operations (Auth Required) ==========

function answersList() {
    group('Answers List', function () {
        const res = http.get(
            `${BASE_URL}/api/answers?limit=10`,
            { headers: getAuthHeaders() }
        );

        const success = check(res, {
            'answers list success': (r) => r.status === 200 || r.status === 401,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function answerDetail() {
    group('Answer Detail', function () {
        const res = http.get(
            `${BASE_URL}/api/answers/1`,
            { headers: getAuthHeaders() }
        );

        const success = check(res, {
            'answer detail success': (r) => r.status === 200 || r.status === 401 || r.status === 404,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

// ========== User Operations (Auth Required) ==========

function userStats() {
    group('User Stats', function () {
        const res = http.get(
            `${BASE_URL}/api/users/me/stats`,
            { headers: getAuthHeaders() }
        );

        const success = check(res, {
            'user stats success': (r) => r.status === 200 || r.status === 401,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function userWeeklyStats() {
    group('User Weekly Stats', function () {
        const res = http.get(
            `${BASE_URL}/api/users/me/stats/weekly`,
            { headers: getAuthHeaders() }
        );

        const success = check(res, {
            'weekly stats success': (r) => r.status === 200 || r.status === 401,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

// ========== Write Operations ==========

function createQuestion() {
    group('Create Question', function () {
        const categories = ['OS', 'NETWORK', 'DB', 'COMPUTER_ARCHITECTURE', 'DATA_STRUCTURE_ALGORITHM'];
        const types = ['CS', 'SYSTEM_DESIGN', 'PORTFOLIO'];

        const payload = JSON.stringify({
            content: `K6 테스트 질문 ${randomString(8)}`,
            category: categories[randomInt(0, categories.length - 1)],
            type: types[randomInt(0, types.length - 1)],
            keywords: ['테스트'],
        });

        const res = http.post(
            `${BASE_URL}/api/questions`,
            payload,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'create question 201': (r) => r.status === 201,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function createMetric() {
    group('Create Metric', function () {
        const payload = JSON.stringify({
            name: `K6 메트릭 ${randomString(6)}`,
            description: `테스트 메트릭`,
            minScore: 0,
            maxScore: 100,
            useYn: true,
        });

        const res = http.post(
            `${BASE_URL}/api/metrics`,
            payload,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'create metric 201': (r) => r.status === 201,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function generatePresignedUrl() {
    group('Generate Presigned URL', function () {
        const payload = JSON.stringify({
            fileName: `test-${randomString(8)}.mp4`,
            fileSize: 1024 * 1024,
            contentType: 'video/mp4',
        });

        const res = http.post(
            `${BASE_URL}/api/files/presigned-url`,
            payload,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'presigned url 200': (r) => r.status === 200,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

// ========== Auth Operations ==========

function authorizationUrl() {
    group('Authorization URL', function () {
        const res = http.get(
            `${BASE_URL}/api/auth/oauth/authorization-url?provider=kakao`,
            { headers: getHeaders(), redirects: 0 }
        );

        const success = check(res, {
            'auth url 302': (r) => r.status === 302,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function tokenRefresh() {
    group('Token Refresh', function () {
        const res = http.post(
            `${BASE_URL}/api/auth/tokens`,
            null,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'token refresh 400': (r) => r.status === 400,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

function exchangeCode() {
    group('Exchange Code', function () {
        const payload = JSON.stringify({
            exchangeCode: 'invalid-code',
        });

        const res = http.post(
            `${BASE_URL}/api/auth/oauth/exchange`,
            payload,
            { headers: getHeaders() }
        );

        const success = check(res, {
            'exchange 4xx': (r) => r.status >= 400 && r.status < 500,
        });

        errorRate.add(!success);
        duration.add(res.timings.duration);
        requestCounter.add(1);
    });
}

// 기본 실행 함수 (scenarios가 정의되면 무시됨)
export default function () {
    readOperations();
}
