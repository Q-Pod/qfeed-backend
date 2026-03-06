/**
 * Soak Test — 인증된 사용자 루프
 *
 * 목적: 중간 부하를 장시간 유지하여 메모리 누수, 성능 저하, 커넥션 고갈 등 검증
 *   - 완만한 Ramp-up → 안정 부하 장시간 유지 → Ramp-down
 *   - 시간 경과에 따른 에러율·응답시간 변화 관찰
 *
 * 사용법:
 *   # 기본 실행 (ACCESS_TOKEN 필수)
 *   k6 run --env ACCESS_TOKEN=<JWT> k6/soak-authenticated-loop.js
 *
 *   # LOAD_TEST_USER_ID 사용 (loadtest 프로파일 서버)
 *   k6 run --env LOAD_TEST_USER_ID=1 k6/soak-authenticated-loop.js
 *
 *   # 부하 파라미터 오버라이드
 *   k6 run \
 *     --env ACCESS_TOKEN=<JWT> \
 *     --env SOAK_VUS=30 \
 *     --env SOAK_SUSTAIN_DURATION=1h \
 *     k6/soak-authenticated-loop.js
 *
 *   # 결과 저장
 *   k6 run --env ACCESS_TOKEN=<JWT> \
 *     --out json=results-soak.json \
 *     k6/soak-authenticated-loop.js
 *
 * ENV 오버라이드 가능 항목:
 *   SOAK_VUS               유지 VU 수          (기본: 20)
 *   SOAK_RAMP_UP_DURATION  ramp-up 시간        (기본: 2m)
 *   SOAK_SUSTAIN_DURATION  안정 부하 유지 시간  (기본: 30m)
 *   SOAK_RAMP_DOWN_DURATION ramp-down 시간     (기본: 2m)
 *   MAX_REAL_TURNS          실전 답변 루프 횟수  (기본: 3)
 *   TEST_QUESTION_ID        연습용 질문 ID 폴백  (기본: 1)
 */

import { authenticatedUserLoop } from './scenarios/authenticated-user-loop.js';

// ===== 부하 파라미터 (ENV 오버라이드 가능) =====
const SOAK_VUS = parseInt(__ENV.SOAK_VUS || '20', 10);
const SOAK_RAMP_UP_DURATION = __ENV.SOAK_RAMP_UP_DURATION || '2m';
const SOAK_SUSTAIN_DURATION = __ENV.SOAK_SUSTAIN_DURATION || '30m';
const SOAK_RAMP_DOWN_DURATION = __ENV.SOAK_RAMP_DOWN_DURATION || '2m';

export const options = {
    stages: [
        { duration: SOAK_RAMP_UP_DURATION, target: SOAK_VUS },     // 안정 부하까지 ramp-up
        { duration: SOAK_SUSTAIN_DURATION, target: SOAK_VUS },     // 안정 부하 유지
        { duration: SOAK_RAMP_DOWN_DURATION, target: 0 },          // ramp-down
    ],
    thresholds: {
        // ── 기본 HTTP 메트릭 ──────────────────────────────────────
        // 소크 환경: 안정적이어야 하므로 스트레스보다 엄격
        http_req_failed: ['rate<0.02'],           // 에러율 2% 미만
        http_req_duration: ['p(95)<2000'],        // 전체 95th < 2s

        // ── 단계별 응답시간 (phase 태그 기반) ──────────────────────
        'http_req_duration{phase:home}': ['p(95)<800'],       // 홈 읽기 < 800ms
        'http_req_duration{phase:practice}': ['p(95)<1500'],  // 연습 답변 < 1.5s
        'http_req_duration{phase:real}': ['p(95)<8000'],      // 실전+AI < 8s
        'http_req_duration{phase:profile}': ['p(95)<800'],    // 프로필 읽기 < 800ms

        // ── 비즈니스 흐름 메트릭 ──────────────────────────────────
        flow_success_rate: ['rate>0.90'],                  // 전체 흐름 성공 90%+
        profile_verification_success_rate: ['rate>0.80'],  // 제출 건 검증 80%+
        'flow_duration{phase:loop}': ['p(95)<90000'],      // 루프 1회 < 90s
    },
};

/**
 * Soak Test 기본 실행 함수
 * 각 VU가 authenticatedUserLoop()를 반복 호출한다.
 */
export default function () {
    authenticatedUserLoop();
}
