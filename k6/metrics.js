/**
 * Metric API 부하 테스트
 *
 * 대상 엔드포인트:
 * - GET    /api/metrics
 * - GET    /api/metrics/{metricId}
 * - POST   /api/metrics
 * - PATCH  /api/metrics/{metricId}
 * - DELETE /api/metrics/{metricId}
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, getHeaders, defaultOptions, randomString, randomInt } from './config.js';

export const options = defaultOptions;

// Custom metrics
const metricErrorRate = new Rate('metric_error_rate');
const metricDuration = new Trend('metric_duration');
const metricsCreated = new Counter('metrics_created');

// 테스트용 메트릭 ID (실제 환경에서는 환경 변수로 설정)
const TEST_METRIC_ID = __ENV.TEST_METRIC_ID || 1;

export default function () {
    group('Metric API', function () {
        // GET /api/metrics (목록 조회)
        group('Get Metrics List', function () {
            const res = http.get(
                `${BASE_URL}/api/metrics?size=10`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'metrics list returns 200': (r) => r.status === 200,
                'metrics list has data': (r) => {
                    try {
                        const body = JSON.parse(r.body);
                        return body.data !== undefined;
                    } catch (e) {
                        return false;
                    }
                },
            });

            metricErrorRate.add(!success);
            metricDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // GET /api/metrics (활성 필터)
        group('Get Active Metrics', function () {
            const res = http.get(
                `${BASE_URL}/api/metrics?useYn=true&size=10`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'active metrics returns 200': (r) => r.status === 200,
            });

            metricErrorRate.add(!success);
            metricDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // GET /api/metrics/{metricId} (상세 조회)
        group('Get Metric Detail', function () {
            const res = http.get(
                `${BASE_URL}/api/metrics/${TEST_METRIC_ID}`,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'metric detail returns 200 or 404': (r) => r.status === 200 || r.status === 404,
            });

            metricErrorRate.add(!success);
            metricDuration.add(res.timings.duration);
        });

        sleep(1);
    });
}

/**
 * 쓰기 작업 테스트 (별도 실행 권장)
 * 데이터 변경이 발생하므로 주의 필요
 */
export function writeTests() {
    let createdMetricId = null;

    group('Metric Write Operations', function () {
        // POST /api/metrics (생성)
        group('Create Metric', function () {
            const payload = JSON.stringify({
                name: `K6 테스트 메트릭 ${randomString(8)}`,
                description: `K6 부하 테스트용 메트릭입니다. ${randomString(20)}`,
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
                'create metric returns 201': (r) => r.status === 201,
            });

            if (res.status === 201) {
                metricsCreated.add(1);
                try {
                    const body = JSON.parse(res.body);
                    createdMetricId = body.data?.id;
                } catch (e) {
                    console.log('Failed to parse create response');
                }
            }

            metricErrorRate.add(!success);
            metricDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // PATCH /api/metrics/{metricId} (수정)
        group('Update Metric', function () {
            const targetId = createdMetricId || TEST_METRIC_ID;
            const payload = JSON.stringify({
                name: `K6 수정된 메트릭 ${randomString(8)}`,
                description: `K6 수정된 설명 ${randomString(20)}`,
            });

            const res = http.patch(
                `${BASE_URL}/api/metrics/${targetId}`,
                payload,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'update metric returns 200 or 404': (r) => r.status === 200 || r.status === 404,
            });

            metricErrorRate.add(!success);
            metricDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // DELETE /api/metrics/{metricId} (삭제 - 생성한 메트릭만)
        if (createdMetricId) {
            group('Delete Metric', function () {
                const res = http.del(
                    `${BASE_URL}/api/metrics/${createdMetricId}`,
                    null,
                    { headers: getHeaders() }
                );

                const success = check(res, {
                    'delete metric returns 200': (r) => r.status === 200,
                });

                metricErrorRate.add(!success);
                metricDuration.add(res.timings.duration);
            });
        }
    });
}

/**
 * CRUD 전체 플로우 테스트
 */
export function crudFlowTest() {
    let metricId = null;

    group('Metric CRUD Flow', function () {
        // Create
        group('Step 1: Create', function () {
            const payload = JSON.stringify({
                name: `Flow Test Metric ${randomString(6)}`,
                description: 'CRUD 플로우 테스트용',
                minScore: 0,
                maxScore: 10,
                useYn: true,
            });

            const res = http.post(
                `${BASE_URL}/api/metrics`,
                payload,
                { headers: getHeaders() }
            );

            check(res, {
                'create returns 201': (r) => r.status === 201,
            });

            if (res.status === 201) {
                try {
                    const body = JSON.parse(res.body);
                    metricId = body.data?.id;
                } catch (e) {
                    // ignore
                }
            }

            metricDuration.add(res.timings.duration);
        });

        sleep(0.3);

        // Read
        if (metricId) {
            group('Step 2: Read', function () {
                const res = http.get(
                    `${BASE_URL}/api/metrics/${metricId}`,
                    { headers: getHeaders() }
                );

                check(res, {
                    'read returns 200': (r) => r.status === 200,
                });

                metricDuration.add(res.timings.duration);
            });

            sleep(0.3);

            // Update
            group('Step 3: Update', function () {
                const payload = JSON.stringify({
                    description: 'Updated by CRUD flow test',
                });

                const res = http.patch(
                    `${BASE_URL}/api/metrics/${metricId}`,
                    payload,
                    { headers: getHeaders() }
                );

                check(res, {
                    'update returns 200': (r) => r.status === 200,
                });

                metricDuration.add(res.timings.duration);
            });

            sleep(0.3);

            // Delete
            group('Step 4: Delete', function () {
                const res = http.del(
                    `${BASE_URL}/api/metrics/${metricId}`,
                    null,
                    { headers: getHeaders() }
                );

                check(res, {
                    'delete returns 200': (r) => r.status === 200,
                });

                metricDuration.add(res.timings.duration);
            });
        }
    });
}
