/**
 * File API 부하 테스트
 *
 * 대상 엔드포인트:
 * - POST /api/files/presigned-url
 * - POST /api/files/{fileId}/confirm
 */

import http from 'k6/http';
import { check, sleep, group } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';
import { BASE_URL, getHeaders, defaultOptions, randomString } from './config.js';

export const options = defaultOptions;

// Custom metrics
const fileErrorRate = new Rate('file_error_rate');
const fileDuration = new Trend('file_duration');
const presignedUrlsGenerated = new Counter('presigned_urls_generated');

// 테스트용 파일 ID (실제 환경에서는 환경 변수로 설정)
const TEST_FILE_ID = __ENV.TEST_FILE_ID || 1;

// 파일 타입 및 크기 상수
const FILE_TYPES = ['video/mp4', 'video/webm', 'audio/mp3', 'audio/wav'];
const FILE_SIZES = [1024 * 1024, 5 * 1024 * 1024, 10 * 1024 * 1024]; // 1MB, 5MB, 10MB

function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

export default function () {
    group('File API', function () {
        // POST /api/files/presigned-url
        group('Generate Presigned URL', function () {
            const payload = JSON.stringify({
                fileName: `test-file-${randomString(8)}.mp4`,
                fileSize: FILE_SIZES[randomInt(0, FILE_SIZES.length - 1)],
                contentType: FILE_TYPES[randomInt(0, FILE_TYPES.length - 1)],
            });

            const res = http.post(
                `${BASE_URL}/api/files/presigned-url`,
                payload,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'presigned-url returns 200': (r) => r.status === 200,
                'presigned-url has uploadUrl': (r) => {
                    try {
                        const body = JSON.parse(r.body);
                        return body.data && body.data.uploadUrl;
                    } catch (e) {
                        return false;
                    }
                },
                'presigned-url has fileId': (r) => {
                    try {
                        const body = JSON.parse(r.body);
                        return body.data && body.data.fileId;
                    } catch (e) {
                        return false;
                    }
                },
            });

            if (success) {
                presignedUrlsGenerated.add(1);
            }

            fileErrorRate.add(!success);
            fileDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // POST /api/files/{fileId}/confirm
        group('Confirm Upload', function () {
            const res = http.post(
                `${BASE_URL}/api/files/${TEST_FILE_ID}/confirm`,
                null,
                { headers: getHeaders() }
            );

            // 파일이 없거나 S3에 업로드되지 않은 경우 다양한 응답 가능
            const success = check(res, {
                'confirm returns expected status': (r) =>
                    r.status === 200 || r.status === 404 || r.status === 422,
            });

            fileErrorRate.add(!success);
            fileDuration.add(res.timings.duration);
        });

        sleep(1);
    });
}

/**
 * Presigned URL 생성 후 확인 플로우 테스트
 */
export function flowTest() {
    group('File Upload Flow', function () {
        let fileId = null;

        // Step 1: Presigned URL 생성
        group('Step 1: Generate Presigned URL', function () {
            const payload = JSON.stringify({
                fileName: `flow-test-${randomString(8)}.mp4`,
                fileSize: 1024 * 1024, // 1MB
                contentType: 'video/mp4',
            });

            const res = http.post(
                `${BASE_URL}/api/files/presigned-url`,
                payload,
                { headers: getHeaders() }
            );

            const success = check(res, {
                'presigned-url returns 200': (r) => r.status === 200,
            });

            if (res.status === 200) {
                try {
                    const body = JSON.parse(res.body);
                    fileId = body.data?.fileId;
                } catch (e) {
                    console.log('Failed to parse presigned-url response');
                }
            }

            fileErrorRate.add(!success);
            fileDuration.add(res.timings.duration);
        });

        sleep(0.5);

        // Step 2: 업로드 확인 (실제로는 S3 업로드 후 호출해야 함)
        if (fileId) {
            group('Step 2: Confirm Upload', function () {
                const res = http.post(
                    `${BASE_URL}/api/files/${fileId}/confirm`,
                    null,
                    { headers: getHeaders() }
                );

                // S3에 실제 파일이 없으므로 422 예상
                const success = check(res, {
                    'confirm returns 422 without actual upload': (r) => r.status === 422,
                });

                fileErrorRate.add(!success);
                fileDuration.add(res.timings.duration);
            });
        }

        sleep(1);
    });
}

/**
 * 다양한 파일 타입으로 Presigned URL 테스트
 */
export function fileTypeTest() {
    const testCases = [
        { fileName: 'test.mp4', contentType: 'video/mp4', fileSize: 1024 * 1024 },
        { fileName: 'test.webm', contentType: 'video/webm', fileSize: 2 * 1024 * 1024 },
        { fileName: 'test.mp3', contentType: 'audio/mpeg', fileSize: 512 * 1024 },
        { fileName: 'test.wav', contentType: 'audio/wav', fileSize: 1024 * 1024 },
    ];

    group('File Type Tests', function () {
        testCases.forEach((tc, index) => {
            group(`Test ${index + 1}: ${tc.contentType}`, function () {
                const payload = JSON.stringify({
                    fileName: tc.fileName,
                    fileSize: tc.fileSize,
                    contentType: tc.contentType,
                });

                const res = http.post(
                    `${BASE_URL}/api/files/presigned-url`,
                    payload,
                    { headers: getHeaders() }
                );

                const success = check(res, {
                    'presigned-url returns 200 or 400': (r) => r.status === 200 || r.status === 400,
                });

                fileErrorRate.add(!success);
                fileDuration.add(res.timings.duration);
            });

            sleep(0.3);
        });
    });
}
