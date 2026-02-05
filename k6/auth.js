/**
 * Authentication API 부하 테스트
 *
 * 대상 엔드포인트:
 * - GET  /api/auth/oauth/authorization-url?provider=kakao
 * - POST /api/auth/oauth/exchange
 * - POST /api/auth/tokens
 * - POST /api/auth/logout (Auth 필요)
 * - POST /api/auth/logout/all (Auth 필요)
 *
 * 참고: OAuth 콜백은 외부 서비스 의존성으로 제외
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, getHeaders, defaultOptions, checkResponse } from './config.js';

export const options = defaultOptions;

// Custom metrics
const authErrorRate = new Rate('auth_error_rate');
const authDuration = new Trend('auth_duration');

export default function () {
    group('Auth API', function () {
        // GET /api/auth/oauth/authorization-url
        group('Get Authorization URL', function () {
            const res = http.get(
                `${BASE_URL}/api/auth/oauth/authorization-url?provider=kakao`,
                {
                    headers: getHeaders(),
                    redirects: 0,  // 리다이렉트 따라가지 않음
                }
            );

            const success = check(res, {
                'authorization-url returns 302': (r) => r.status === 302,
                'has Location header': (r) => r.headers['Location'] !== undefined,
            });

            authErrorRate.add(!success);
            authDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // POST /api/auth/oauth/exchange (잘못된 코드로 테스트)
        group('Exchange Code (Invalid)', function () {
            const payload = JSON.stringify({
                exchangeCode: 'invalid-exchange-code-for-load-test',
            });

            const res = http.post(
                `${BASE_URL}/api/auth/oauth/exchange`,
                payload,
                { headers: getHeaders() }
            );

            // 잘못된 코드이므로 400 또는 401 예상
            const success = check(res, {
                'exchange returns 4xx for invalid code': (r) => r.status >= 400 && r.status < 500,
            });

            authErrorRate.add(!success);
            authDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // POST /api/auth/tokens (Refresh Token 없이 테스트)
        group('Refresh Tokens (No Cookie)', function () {
            const res = http.post(
                `${BASE_URL}/api/auth/tokens`,
                null,
                { headers: getHeaders() }
            );

            // Refresh Token 쿠키가 없으므로 400 예상
            const success = check(res, {
                'tokens returns 400 without cookie': (r) => r.status === 400,
            });

            authErrorRate.add(!success);
            authDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // POST /api/auth/logout (인증 없이 테스트)
        group('Logout (Unauthorized)', function () {
            const res = http.post(
                `${BASE_URL}/api/auth/logout`,
                null,
                { headers: getHeaders() }
            );

            // 인증이 없으므로 401 예상
            const success = check(res, {
                'logout returns 401 without auth': (r) => r.status === 401,
            });

            authErrorRate.add(!success);
            authDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // POST /api/auth/logout/all (인증 없이 테스트)
        group('Logout All (Unauthorized)', function () {
            const res = http.post(
                `${BASE_URL}/api/auth/logout/all`,
                null,
                { headers: getHeaders() }
            );

            // 인증이 없으므로 401 예상
            const success = check(res, {
                'logout-all returns 401 without auth': (r) => r.status === 401,
            });

            authErrorRate.add(!success);
            authDuration.add(res.timings.duration);
        });

        sleep(1);
    });
}

/**
 * 인증된 사용자 테스트 (별도 실행)
 * ACCESS_TOKEN 환경 변수가 설정된 경우에만 의미 있음
 */
export function authenticatedTests() {
    const ACCESS_TOKEN = __ENV.ACCESS_TOKEN;
    if (!ACCESS_TOKEN) {
        console.log('Skipping authenticated tests: ACCESS_TOKEN not set');
        return;
    }

    const authHeaders = {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${ACCESS_TOKEN}`,
    };

    group('Auth API (Authenticated)', function () {
        // POST /api/auth/logout
        group('Logout', function () {
            const res = http.post(
                `${BASE_URL}/api/auth/logout`,
                null,
                { headers: authHeaders }
            );

            check(res, {
                'logout returns 200': (r) => r.status === 200,
            });
        });
    });
}
