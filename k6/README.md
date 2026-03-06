# K6 부하 테스트 스크립트

QFeed Backend API에 대한 K6 부하 테스트 스크립트입니다.

## 파일 구조

```
k6/
=======
├── config.js                          # 공통 설정 (BASE_URL, 헤더, 유틸 등)
├── auth.js                            # 인증 API 테스트
├── questions.js                       # 질문 API 테스트
├── answers.js                         # 답변 API 테스트
├── files.js                           # 파일 API 테스트
├── metrics.js                         # 메트릭 API 테스트
├── users.js                           # 사용자 학습 API 테스트
├── all.js                             # 전체 통합 테스트 (시나리오 기반)
├── stress-authenticated-loop.js       # ★ Stress Test (로그인 필수 유저 흐름)
├── soak-authenticated-loop.js         # ★ Soak Test  (로그인 필수 유저 흐름)
├── scenarios/
│   └── authenticated-user-loop.js    # ★ 재사용 시나리오 모듈 (stress/soak 공유)
├── userflow/
│   ├── setup.js              # 토큰 풀 생성 + VU별 토큰 분배
│   ├── flow_utils.js         # 유저 시나리오 공통 유틸(인증/폴링/S3 mock 등)
│   ├── practiceflow.js       # 연습모드 시나리오 단독 실행
│   ├── realflow.js           # 실전모드 시나리오 단독 실행
│   ├── interview_flow.js     # 연습+실전 혼합 시나리오 (비율 조절 가능)
│   └── README.md             # 유저 플로우 부하테스트 독립 실행 가이드
└── README.md                          # 이 문서
```

## 사전 요구사항

1. **K6 설치**
   ```bash
   # macOS
   brew install k6

   # Linux
   sudo gpg -k
   sudo gpg --no-default-keyring --keyring /usr/share/keyrings/k6-archive-keyring.gpg --keyserver hkp://keyserver.ubuntu.com:80 --recv-keys C5AD17C747E3415A3642D57D77C6C491D6AC1D69
   echo "deb [signed-by=/usr/share/keyrings/k6-archive-keyring.gpg] https://dl.k6.io/deb stable main" | sudo tee /etc/apt/sources.list.d/k6.list
   sudo apt-get update
   sudo apt-get install k6
   ```

2. **서버 실행**
   ```bash
   ./gradlew bootRun
   ```

3. **테스트 데이터 준비** (권장)
   - 질문 데이터 최소 10개 이상
   - 메트릭 데이터 최소 5개 이상

## 환경 변수

### 공통

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `BASE_URL` | API 서버 주소 | `http://localhost:8080` |
| `ACCESS_TOKEN` | JWT 인증 토큰 **(stress/soak 필수)** | - |
| `LOAD_TEST_USER_ID` | 로드테스트 전용 헤더 인증 (`X-Load-Test-User-Id`) | - |
| `TEST_QUESTION_ID` | 테스트용 질문 ID (홈 목록에서 못 가져오면 폴백) | `1` |
| `TEST_METRIC_ID` | 테스트용 메트릭 ID | `1` |
| `TEST_ANSWER_ID` | 테스트용 답변 ID | `1` |
| `TEST_FILE_ID` | 테스트용 파일 ID | `1` |
| `MAX_TURNS` | all.js 세션 답변 반복 횟수 | `9` |
| `MAX_REAL_TURNS` | stress/soak 실전 답변 루프 횟수 | `3` |
| `ENABLE_DELETE_TEST` | 삭제 테스트 활성화 | `false` |

### Stress Test 전용 ENV

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `STRESS_PEAK_VUS` | 피크 VU 수 | `50` |
| `STRESS_RAMP_UP_DURATION` | 피크까지 ramp-up 시간 | `1m` |
| `STRESS_PEAK_DURATION` | 피크 유지 시간 | `3m` |
| `STRESS_RAMP_DOWN_DURATION` | ramp-down 시간 | `1m` |

### Soak Test 전용 ENV

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `SOAK_VUS` | 안정 부하 VU 수 | `20` |
| `SOAK_RAMP_UP_DURATION` | ramp-up 시간 | `2m` |
| `SOAK_SUSTAIN_DURATION` | 안정 부하 유지 시간 | `30m` |
| `SOAK_RAMP_DOWN_DURATION` | ramp-down 시간 | `2m` |

## 실행 방법

### ★ Stress Test (로그인 필수 유저 흐름 — 고부하)

```bash
# 기본 실행 (ACCESS_TOKEN 방식)
k6 run --env ACCESS_TOKEN=your_jwt_token k6/stress-authenticated-loop.js

# loadtest 프로파일 서버 사용 (X-Load-Test-User-Id 헤더 방식)
k6 run --env LOAD_TEST_USER_ID=1 k6/stress-authenticated-loop.js

# 부하 파라미터 커스터마이즈
k6 run \
  --env ACCESS_TOKEN=your_jwt_token \
  --env STRESS_PEAK_VUS=100 \
  --env STRESS_PEAK_DURATION=5m \
  --env MAX_REAL_TURNS=2 \
  k6/stress-authenticated-loop.js

# 결과 저장
k6 run --env ACCESS_TOKEN=your_jwt_token \
  --out json=results-stress.json \
  k6/stress-authenticated-loop.js
```

**기본 부하 프로파일:**
```
30s  → 10 VU (워밍업)
1m   → 50 VU (ramp-up)
3m   → 50 VU (피크 유지)
1m   → 0 VU  (ramp-down)
총 약 5분 30초
```

---

### ★ Soak Test (로그인 필수 유저 흐름 — 장시간)

```bash
# 기본 실행 (30분 소크)
k6 run --env ACCESS_TOKEN=your_jwt_token k6/soak-authenticated-loop.js

# loadtest 프로파일 서버 사용
k6 run --env LOAD_TEST_USER_ID=1 k6/soak-authenticated-loop.js

# 부하/시간 커스터마이즈 (1시간 소크)
k6 run \
  --env ACCESS_TOKEN=your_jwt_token \
  --env SOAK_VUS=30 \
  --env SOAK_SUSTAIN_DURATION=1h \
  k6/soak-authenticated-loop.js

# 결과 저장
k6 run --env ACCESS_TOKEN=your_jwt_token \
  --out json=results-soak.json \
  k6/soak-authenticated-loop.js
```

**기본 부하 프로파일:**
```
2m   → 20 VU (ramp-up)
30m  → 20 VU (안정 부하 유지)
2m   → 0 VU  (ramp-down)
총 약 34분
```

---

### 유저 흐름 시나리오 설명 (stress/soak 공통)

```
1. [홈]        카테고리 조회 + 오늘의 추천 + 질문 목록
2. [연습(2-1)] 질문 상세 → 텍스트 답변 제출 (submittedAnswerId 캡처)
3. [홈 복귀]   질문 목록 재조회
4. [실전(2-2)] 세션 생성 → 답변 루프(MAX_REAL_TURNS) → AI 피드백 요청
5. [프로필(3-1)] 내 답변 목록 → 제출 건 검증 → 최근 상세 → 학습 통계
      검증 전략:
        - submittedAnswerId 수집 성공: 목록에 해당 ID 포함 여부
        - submittedAnswerId 없음:     최신 createdAt > 제출 시각
        - createdAt도 없음:           목록 존재 여부만 확인 + 경고
6. 위 흐름 반복 (1~3초 sleep 후)
```

---

### 개별 도메인 테스트

```bash
# 질문 API 테스트
k6 run k6/questions.js

# 메트릭 API 테스트
k6 run k6/metrics.js

# 파일 API 테스트
k6 run k6/files.js

# 인증 API 테스트
k6 run k6/auth.js

# 답변 API 테스트 (인증 필요)
k6 run --env ACCESS_TOKEN=your_token k6/answers.js

# 사용자 API 테스트 (인증 필요)
k6 run --env ACCESS_TOKEN=your_token k6/users.js
```

### 전체 통합 테스트 (기존)

```bash
# 기본 실행
k6 run k6/all.js

# 인증 토큰과 함께 실행
k6 run --env ACCESS_TOKEN=your_token k6/all.js

# 다른 서버 주소로 실행
k6 run --env BASE_URL=http://staging.example.com k6/all.js
```

### 유저 시나리오 기반 플로우 테스트

상세 운영 가이드: `k6/userflow/README.md`

토큰 입력은 아래 3가지 중 하나를 사용합니다 (우선순위: `USER_TOKENS_JSON` > `ACCESS_TOKEN` > `USER_TOKEN_CSV`).

- `USER_TOKEN_CSV`: CSV 파일 (`user_id`, `access_token`, `refresh_token`, `access_expires_at`)
- `USER_TOKENS_JSON`: JSON 문자열 배열
- `ACCESS_TOKEN` (+ 선택: `REFRESH_TOKEN`, `SINGLE_USER_ID`, `ACCESS_EXPIRES_AT`)

```bash
# 연습모드 단독 실행
k6 run k6/userflow/practiceflow.js \
  --env BASE_URL=http://localhost:8080 \
  --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv \
  --env PRACTICE_RATE=6

# 실전모드 단독 실행
k6 run k6/userflow/realflow.js \
  --env BASE_URL=http://localhost:8080 \
  --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv \
  --env REAL_RATE=2 \
  --env REAL_TURN_LIMIT=4

# 연습/실전 혼합 실행 (비율 조절: PRACTICE_RATE : REAL_RATE)
k6 run k6/userflow/interview_flow.js \
  --env BASE_URL=http://localhost:8080 \
  --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv \
  --env PRACTICE_RATE=6 \
  --env REAL_RATE=2 \
  --env REAL_TURN_LIMIT=4

# JSON 토큰 풀 예시 (CSV 없이 실행)
k6 run k6/userflow/interview_flow.js \
  --env BASE_URL=http://localhost:8080 \
  --env USER_TOKENS_JSON='[{"user_id":1,"access_token":"...","refresh_token":"..."},{"user_id":2,"access_token":"...","refresh_token":"..."}]' \
  --env PRACTICE_RATE=2 \
  --env REAL_RATE=1
```

주요 환경변수:
- `PRACTICE_RATE`, `REAL_RATE`: 연습/실전 사이클 비율 조절 (초당 시작 수)
- `REAL_TURN_LIMIT`: 실전 질문-답변 턴 수(기본값 `4`)
- `MOCK_AI`: AI(STT/TTS) mock 여부(기본값 `true`)
- `MOCK_S3_UPLOAD`: S3 PUT 외부 호출 mock 여부(기본값 `true`)
- `MOCK_S3_LATENCY_SEC`: mock PUT 지연 시간(기본값 `0.03`)
- `ALLOW_STT_FALLBACK`: STT 실패 시 텍스트 fallback 허용(기본값 `true`)

### 결과 출력

```bash
# JSON 형식으로 결과 저장
k6 run --out json=results.json k6/all.js

# InfluxDB로 메트릭 전송
k6 run --out influxdb=http://localhost:8086/k6 k6/all.js

# 여러 출력 동시 사용
k6 run --out json=results.json --out influxdb=http://localhost:8086/k6 k6/all.js
```

## 부하 설정

### 기본 옵션 (defaultOptions)

```javascript
stages: [
    { duration: '30s', target: 10 },   // Ramp-up: 30초 동안 10 VU까지 증가
    { duration: '1m', target: 50 },    // Load: 1분 동안 50 VU 유지
    { duration: '30s', target: 100 },  // Spike: 30초 동안 100 VU까지 증가
    { duration: '1m', target: 50 },    // Sustained: 1분 동안 50 VU 유지
    { duration: '30s', target: 0 },    // Ramp-down: 30초 동안 0 VU로 감소
]
```

### 성능 임계값 (Thresholds)

| 메트릭 | 조건 | 설명 |
|--------|------|------|
| `http_req_duration` | `p(95)<500` | 95% 요청이 500ms 이내 |
| `http_req_failed` | `rate<0.01` | 에러율 1% 미만 |

## 테스트 대상 API

### 인증 API (`/api/auth`) - 6개
- GET `/api/auth/oauth/authorization-url?provider=kakao`
- GET `/api/auth/oauth/{provider}/callback`
- POST `/api/auth/oauth/exchange`
- POST `/api/auth/tokens`
- POST `/api/auth/logout` (Auth)
- POST `/api/auth/logout/all` (Auth)

### 질문 API (`/api/questions`) - 11개
- GET `/api/questions`
- GET `/api/questions/categories`
- GET `/api/questions/types`
- GET `/api/questions/{questionId}`
- GET `/api/questions/search?q=keyword`
- GET `/api/questions/recommendation`
- POST `/api/questions`
- PATCH `/api/questions/{questionId}`
- DELETE `/api/questions/{questionId}`
- GET `/api/questions/{questionId}/keywords`
- POST `/api/questions/{questionId}/keyword-checks`

### 답변 API (`/api`) - 6개 (모두 Auth 필요)
- GET `/api/answers`
- GET `/api/answers/{answerId}`
- POST `/api/interview/answers`
- POST `/api/interview/sessions` (세션 생성, 첫 질문 반환)
- POST `/api/answers/real` (세션 기반 답변 제출, 다음 질문 반환 — 반복 루프)
- POST `/api/interview/sessions/feedback/request` (AI 최종 피드백)
- GET `/api/interviews/answers/{answerId}/feedback`

### 파일 API (`/api/files`) - 2개
- POST `/api/files/presigned-url`
- POST `/api/files/{fileId}/confirm`

### 메트릭 API (`/api/metrics`) - 5개
- GET `/api/metrics`
- GET `/api/metrics/{metricId}`
- POST `/api/metrics`
- PATCH `/api/metrics/{metricId}`
- DELETE `/api/metrics/{metricId}`

### 사용자 학습 API (`/api/users/me`) - 2개 (모두 Auth 필요)
- GET `/api/users/me/stats`
- GET `/api/users/me/stats/weekly`

## 커스텀 메트릭

### 기존 도메인별 메트릭

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `{domain}_error_rate` | Rate | 해당 도메인 에러율 |
| `{domain}_duration` | Trend | 해당 도메인 응답 시간 |
| `questions_created` | Counter | 생성된 질문 수 |
| `metrics_created` | Counter | 생성된 메트릭 수 |
| `presigned_urls_generated` | Counter | 생성된 Presigned URL 수 |

### ★ Stress/Soak 전용 메트릭

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `flow_success_rate` | Rate | 사용자 흐름 단계별·루프 전체 성공률 |
| `flow_duration` | Trend | 각 단계(`phase` 태그) 및 루프 전체 소요시간 |
| `profile_verification_success_rate` | Rate | 프로필 제출 건 검증 성공률 |

**`flow_duration` phase 태그 값:**

| 태그 | 대응 단계 |
|------|----------|
| `home` | 홈 진입·복귀 API 호출 |
| `practice` | 연습 질문 조회 + 답변 제출 |
| `real` | 실전 세션 생성 + 답변 루프 + 피드백 |
| `profile` | 내 답변 목록 + 상세 + 통계 |
| `loop` | 루프 1회 전체 |

## 주의사항

1. **쓰기 테스트 주의**: `writeTests()` 함수는 실제 데이터를 생성합니다. 프로덕션 환경에서 실행하지 마세요.

2. **삭제 테스트**: 기본적으로 비활성화되어 있습니다. `ENABLE_DELETE_TEST=true`로 활성화할 수 있습니다.

3. **인증 토큰**: Answer, User API 테스트는 유효한 JWT 토큰이 필요합니다.

4. **세션 답변 루프**: `submitTests()`의 `Submit Session Answer` 그룹은 REAL 인터뷰 세션 전체 플로우를 시뮬레이션합니다.
   - 세션 생성 → 답변 제출 & 다음 질문 수신 반복 (최대 `MAX_TURNS`번) → AI 최종 피드백 요청
   - `is_final: true` 응답 시 루프 조기 종료 후 피드백 요청

5. **AI 피드백 포함**: `submitTests()`는 AI 최종 피드백 엔드포인트를 호출합니다. AI 서비스가 실행 중이어야 정상 응답을 받을 수 있습니다.

6. **★ stress/soak Fail-fast**: `ACCESS_TOKEN`, `LOAD_TEST_USER_ID` 둘 다 미설정 시 테스트가 즉시 종료됩니다.

7. **★ MAX_REAL_TURNS 기본값 3**: stress/soak에서는 루프 시간 단축을 위해 기본 3회로 설정되어 있습니다. `all.js`의 `MAX_TURNS`(9)와 별개입니다.

## 트러블슈팅

### 401 Unauthorized 에러
- `ACCESS_TOKEN` 환경 변수가 올바르게 설정되었는지 확인
- 토큰이 만료되지 않았는지 확인

### Connection Refused 에러
- 서버가 실행 중인지 확인
- `BASE_URL`이 올바르게 설정되었는지 확인

### 404 Not Found 에러
- `TEST_QUESTION_ID`, `TEST_METRIC_ID` 등이 실제 존재하는 데이터인지 확인

### [FAIL-FAST] 인증 설정이 없습니다 에러 (stress/soak)
- `--env ACCESS_TOKEN=<JWT>` 또는 `--env LOAD_TEST_USER_ID=<ID>` 설정 필요

### `[profile] 제출 answerId 미발견` 경고
- 백엔드 `POST /api/interview/answers` 응답 body의 answerId 필드명 확인
- 현재 코드는 `answerId`, `id`, `answer_id` 순서로 탐색
- 필드명이 다르면 `scenarios/authenticated-user-loop.js`의 `toAnswerId()` 함수 수정

### `[profile] createdAt 없음` 경고
- `GET /api/answers` 응답에 `createdAt` 또는 `created_at` 필드가 없는 경우
- 목록 존재 여부만 검증하며 완전한 검증은 불가 (TODO: 백엔드 응답 필드 확인 필요)
