/**
 * K6 부하 테스트 공통 설정
 *
 * 환경 변수:
 * - BASE_URL: API 서버 주소 (기본값: http://localhost:8080)
 * - ACCESS_TOKEN: 인증이 필요한 API용 JWT 토큰
 */

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
export const ACCESS_TOKEN = __ENV.ACCESS_TOKEN || '';
export const LOAD_TEST_USER_ID = __ENV.LOAD_TEST_USER_ID || '';

/**
 * 공통 헤더 생성
 */
export function getHeaders(withAuth = false) {
    const headers = {
        'Content-Type': 'application/json',
        'Accept': 'application/json',
    };

    if (withAuth) {
        if (LOAD_TEST_USER_ID) {
            headers['X-Load-Test-User-Id'] = LOAD_TEST_USER_ID;
        } else if (ACCESS_TOKEN) {
            headers['Authorization'] = `Bearer ${ACCESS_TOKEN}`;
        }
    }

    return headers;
}

/**
 * Multipart 헤더 생성
 */
export function getMultipartHeaders(withAuth = false) {
    const headers = {
        'Accept': 'application/json',
    };

    if (withAuth) {
        if (LOAD_TEST_USER_ID) {
            headers['X-Load-Test-User-Id'] = LOAD_TEST_USER_ID;
        } else if (ACCESS_TOKEN) {
            headers['Authorization'] = `Bearer ${ACCESS_TOKEN}`;
        }
    }

    return headers;
}

/**
 * 기본 부하 테스트 옵션
 */
export const defaultOptions = {
    stages: [
        { duration: '30s', target: 10 },   // Ramp-up: 30초 동안 10 VU까지 증가
        { duration: '1m', target: 50 },    // Load: 1분 동안 50 VU 유지
        { duration: '30s', target: 100 },  // Spike: 30초 동안 100 VU까지 증가
        { duration: '1m', target: 50 },    // Sustained: 1분 동안 50 VU 유지
        { duration: '30s', target: 0 },    // Ramp-down: 30초 동안 0 VU로 감소
    ],
    thresholds: {
        http_req_duration: ['p(95)<500'],  // 95% 요청이 500ms 이내
        http_req_failed: ['rate<0.01'],    // 에러율 1% 미만
    },
};

/**
 * 경량 부하 테스트 옵션 (개발/테스트용)
 */
export const lightOptions = {
    stages: [
        { duration: '10s', target: 5 },
        { duration: '30s', target: 10 },
        { duration: '10s', target: 0 },
    ],
    thresholds: {
        http_req_duration: ['p(95)<1000'],
        http_req_failed: ['rate<0.05'],
    },
};

/**
 * 스모크 테스트 옵션 (빠른 검증용)
 */
export const smokeOptions = {
    vus: 1,
    duration: '10s',
    thresholds: {
        http_req_failed: ['rate==0'],
    },
};

/**
 * 응답 검증 유틸리티
 */
export function checkResponse(res, expectedStatus = 200) {
    return res.status === expectedStatus;
}

/**
 * JSON 응답 파싱
 */
export function parseJsonResponse(res) {
    try {
        return JSON.parse(res.body);
    } catch (e) {
        return null;
    }
}

/**
 * 랜덤 정수 생성
 */
export function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * 랜덤 문자열 생성
 */
export function randomString(length) {
    const chars = 'abcdefghijklmnopqrstuvwxyz0123456789';
    let result = '';
    for (let i = 0; i < length; i++) {
        result += chars.charAt(Math.floor(Math.random() * chars.length));
    }
    return result;
}
