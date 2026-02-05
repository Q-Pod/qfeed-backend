/**
 * User Learning API 부하 테스트
 *
 * 대상 엔드포인트:
 * - GET /api/users/me/stats (Auth 필요)
 * - GET /api/users/me/stats/weekly (Auth 필요)
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { BASE_URL, defaultOptions } from './config.js';

export const options = defaultOptions;

// Custom metrics
const userErrorRate = new Rate('user_error_rate');
const userDuration = new Trend('user_duration');

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
    group('User Learning API', function () {
        // GET /api/users/me/stats
        group('Get Learning Stats', function () {
            const res = http.get(
                `${BASE_URL}/api/users/me/stats`,
                { headers: getAuthHeaders() }
            );

            let success;
            if (ACCESS_TOKEN) {
                success = check(res, {
                    'stats returns 200': (r) => r.status === 200,
                    'stats has data': (r) => {
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
                    'stats returns 401 without auth': (r) => r.status === 401,
                });
            }

            userErrorRate.add(!success);
            userDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // GET /api/users/me/stats/weekly
        group('Get Weekly Stats', function () {
            const res = http.get(
                `${BASE_URL}/api/users/me/stats/weekly`,
                { headers: getAuthHeaders() }
            );

            let success;
            if (ACCESS_TOKEN) {
                success = check(res, {
                    'weekly stats returns 200': (r) => r.status === 200,
                    'weekly stats has data': (r) => {
                        try {
                            const body = JSON.parse(r.body);
                            return body.data !== undefined;
                        } catch (e) {
                            return false;
                        }
                    },
                });
            } else {
                success = check(res, {
                    'weekly stats returns 401 without auth': (r) => r.status === 401,
                });
            }

            userErrorRate.add(!success);
            userDuration.add(res.timings.duration);
        });

        sleep(1);
    });
}

/**
 * 인증된 사용자로 전체 통계 조회 테스트
 */
export function authenticatedStatsTest() {
    if (!ACCESS_TOKEN) {
        console.log('Skipping authenticated tests: ACCESS_TOKEN not set');
        return;
    }

    const authHeaders = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': `Bearer ${ACCESS_TOKEN}`,
    };

    group('Authenticated User Stats', function () {
        // 통계 요약 조회
        group('Get Stats Summary', function () {
            const res = http.get(
                `${BASE_URL}/api/users/me/stats`,
                { headers: authHeaders }
            );

            const success = check(res, {
                'stats returns 200': (r) => r.status === 200,
                'has totalAnswers': (r) => {
                    try {
                        const body = JSON.parse(r.body);
                        return body.data && body.data.totalAnswers !== undefined;
                    } catch (e) {
                        return false;
                    }
                },
            });

            userErrorRate.add(!success);
            userDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // 주간 통계 조회
        group('Get Weekly Stats Detail', function () {
            const res = http.get(
                `${BASE_URL}/api/users/me/stats/weekly`,
                { headers: authHeaders }
            );

            const success = check(res, {
                'weekly returns 200': (r) => r.status === 200,
                'has daily data': (r) => {
                    try {
                        const body = JSON.parse(r.body);
                        return body.data && body.data.dailyStats;
                    } catch (e) {
                        return false;
                    }
                },
            });

            userErrorRate.add(!success);
            userDuration.add(res.timings.duration);
        });

        sleep(1);
    });
}

/**
 * 반복 조회 스트레스 테스트
 */
export function stressTest() {
    if (!ACCESS_TOKEN) {
        console.log('Skipping stress tests: ACCESS_TOKEN not set');
        return;
    }

    const authHeaders = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
        'Authorization': `Bearer ${ACCESS_TOKEN}`,
    };

    group('User Stats Stress Test', function () {
        // 짧은 간격으로 여러 번 조회
        for (let i = 0; i < 5; i++) {
            const res = http.get(
                `${BASE_URL}/api/users/me/stats`,
                { headers: authHeaders }
            );

            check(res, {
                [`iteration ${i + 1} returns 200`]: (r) => r.status === 200,
            });

            userDuration.add(res.timings.duration);
            sleep(0.1);
        }

        sleep(0.5);

        // 주간 통계도 반복 조회
        for (let i = 0; i < 5; i++) {
            const res = http.get(
                `${BASE_URL}/api/users/me/stats/weekly`,
                { headers: authHeaders }
            );

            check(res, {
                [`weekly iteration ${i + 1} returns 200`]: (r) => r.status === 200,
            });

            userDuration.add(res.timings.duration);
            sleep(0.1);
        }
    });
}
