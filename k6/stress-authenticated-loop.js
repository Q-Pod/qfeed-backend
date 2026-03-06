/**
 * Stress Test — 인증된 사용자 루프
 *
 * 목적: 고부하 상황에서 핵심 사용자 흐름의 내구성 검증
 *   - 빠른 Ramp-up → 피크 부하 유지 → Ramp-down
 *   - 에러율, 응답시간, 흐름 성공률 검증
 *
 * 사용법:
 *   # 기본 실행 (ACCESS_TOKEN 필수)
 *   k6 run --env ACCESS_TOKEN=<JWT> k6/stress-authenticated-loop.js
 *
 *   # LOAD_TEST_USER_ID 사용 (loadtest 프로파일 서버)
 *   k6 run --env LOAD_TEST_USER_ID=1 k6/stress-authenticated-loop.js
 *
 *   # 부하 파라미터 오버라이드
 *   k6 run \
 *     --env ACCESS_TOKEN=<JWT> \
 *     --env STRESS_PEAK_VUS=100 \
 *     --env STRESS_PEAK_DURATION=5m \
 *     k6/stress-authenticated-loop.js
 *
 *   # 결과 저장
 *   k6 run --env ACCESS_TOKEN=<JWT> \
 *     --out json=results-stress.json \
 *     k6/stress-authenticated-loop.js
 *
 * ENV 오버라이드 가능 항목:
 *   STRESS_PEAK_VUS          피크 VU 수        (기본: 50)
 *   STRESS_RAMP_UP_DURATION  피크까지 ramp 시간 (기본: 1m)
 *   STRESS_PEAK_DURATION     피크 유지 시간     (기본: 3m)
 *   STRESS_RAMP_DOWN_DURATION ramp-down 시간   (기본: 1m)
 *   MAX_REAL_TURNS           실전 답변 루프 횟수 (기본: 3)
 *   TEST_QUESTION_ID         연습용 질문 ID 폴백 (기본: 1)
 */

import { sleep } from 'k6';
import { authenticatedUserLoop } from './scenarios/authenticated-user-loop.js';

// ===== 부하 파라미터 (ENV 오버라이드 가능) =====
const STRESS_PEAK_VUS = parseInt(__ENV.STRESS_PEAK_VUS || '50', 10);
const STRESS_RAMP_UP_DURATION = __ENV.STRESS_RAMP_UP_DURATION || '1m';
const STRESS_PEAK_DURATION = __ENV.STRESS_PEAK_DURATION || '3m';
const STRESS_RAMP_DOWN_DURATION = __ENV.STRESS_RAMP_DOWN_DURATION || '1m';

// 피크의 20% 수준으로 워밍업 (정수 보장)
const WARMUP_VUS = Math.max(1, Math.floor(STRESS_PEAK_VUS * 0.2));

export const options = {
    stages: [
        { duration: '30s', target: WARMUP_VUS },           // 워밍업: 피크의 20%
        { duration: STRESS_RAMP_UP_DURATION, target: STRESS_PEAK_VUS },  // 피크까지 ramp-up
        { duration: STRESS_PEAK_DURATION, target: STRESS_PEAK_VUS },     // 피크 유지
        { duration: STRESS_RAMP_DOWN_DURATION, target: 0 },              // ramp-down
    ],
    thresholds: {
        // ── 기본 HTTP 메트릭 ──────────────────────────────────────
        // 스트레스 환경이므로 일반 기준보다 완화
        http_req_failed: ['rate<0.05'],           // 에러율 5% 미만
        http_req_duration: ['p(95)<3000'],        // 전체 95th < 3s

        // ── 단계별 응답시간 (phase 태그 기반) ──────────────────────
        'http_req_duration{phase:home}': ['p(95)<1000'],      // 홈 읽기 < 1s
        'http_req_duration{phase:practice}': ['p(95)<2000'],  // 연습 답변 < 2s
        'http_req_duration{phase:real}': ['p(95)<8000'],      // 실전+AI < 8s
        'http_req_duration{phase:profile}': ['p(95)<1000'],   // 프로필 읽기 < 1s

        // ── 비즈니스 흐름 메트릭 ──────────────────────────────────
        flow_success_rate: ['rate>0.85'],                  // 전체 흐름 성공 85%+
        profile_verification_success_rate: ['rate>0.70'],  // 제출 건 검증 70%+
        'flow_duration{phase:loop}': ['p(95)<60000'],      // 루프 1회 < 60s
    },
};

/**
 * Stress Test 기본 실행 함수
 * 각 VU가 authenticatedUserLoop()를 반복 호출한다.
 */
export default function () {
    authenticatedUserLoop();
}
