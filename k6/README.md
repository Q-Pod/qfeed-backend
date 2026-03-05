# K6 부하 테스트 스크립트

QFeed Backend API에 대한 K6 부하 테스트 스크립트입니다.

## 파일 구조

```
k6/
├── config.js      # 공통 설정 (BASE_URL, 헤더, 옵션 등)
├── auth.js        # 인증 API 테스트
├── questions.js   # 질문 API 테스트
├── answers.js     # 답변 API 테스트 (AI 호출 제외)
├── files.js       # 파일 API 테스트
├── metrics.js     # 메트릭 API 테스트
├── users.js       # 사용자 학습 API 테스트
├── all.js         # 전체 통합 테스트
├── userflow/
│   ├── setup.js              # 토큰 풀 생성 + VU별 토큰 분배
│   ├── flow_utils.js         # 유저 시나리오 공통 유틸(인증/폴링/S3 mock 등)
│   ├── practiceflow.js       # 연습모드 시나리오 단독 실행
│   ├── realflow.js           # 실전모드 시나리오 단독 실행
│   ├── interview_flow.js     # 연습+실전 혼합 시나리오 (비율 조절 가능)
│   └── README.md             # 유저 플로우 부하테스트 독립 실행 가이드
└── README.md      # 이 문서
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

| 변수명 | 설명 | 기본값 |
|--------|------|--------|
| `BASE_URL` | API 서버 주소 | `http://localhost:8080` |
| `ACCESS_TOKEN` | JWT 인증 토큰 (인증 필요 API용) | - |
| `TEST_QUESTION_ID` | 테스트용 질문 ID | `1` |
| `TEST_METRIC_ID` | 테스트용 메트릭 ID | `1` |
| `TEST_ANSWER_ID` | 테스트용 답변 ID | `1` |
| `TEST_FILE_ID` | 테스트용 파일 ID | `1` |
| `ENABLE_DELETE_TEST` | 삭제 테스트 활성화 | `false` |

## 실행 방법

### 개별 테스트

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

### 전체 통합 테스트

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

### 답변 API (`/api`) - 5개 (모두 Auth 필요)
- GET `/api/answers`
- GET `/api/answers/{answerId}`
- POST `/api/interview/answers`
- POST `/api/interview/sessions/{sessionId}/answers`
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

각 테스트 스크립트는 다음 커스텀 메트릭을 수집합니다:

| 메트릭 | 타입 | 설명 |
|--------|------|------|
| `{domain}_error_rate` | Rate | 해당 도메인 에러율 |
| `{domain}_duration` | Trend | 해당 도메인 응답 시간 |
| `questions_created` | Counter | 생성된 질문 수 |
| `metrics_created` | Counter | 생성된 메트릭 수 |
| `presigned_urls_generated` | Counter | 생성된 Presigned URL 수 |

## 주의사항

1. **쓰기 테스트 주의**: `writeTests()` 함수는 실제 데이터를 생성합니다. 프로덕션 환경에서 실행하지 마세요.

2. **삭제 테스트**: 기본적으로 비활성화되어 있습니다. `ENABLE_DELETE_TEST=true`로 활성화할 수 있습니다.

3. **인증 토큰**: Answer, User API 테스트는 유효한 JWT 토큰이 필요합니다.

4. **AI 호출 제외**: AI 피드백 생성 관련 엔드포인트는 부하 테스트에서 제외되었습니다.

## 트러블슈팅

### 401 Unauthorized 에러
- `ACCESS_TOKEN` 환경 변수가 올바르게 설정되었는지 확인
- 토큰이 만료되지 않았는지 확인

### Connection Refused 에러
- 서버가 실행 중인지 확인
- `BASE_URL`이 올바르게 설정되었는지 확인

### 404 Not Found 에러
- `TEST_QUESTION_ID`, `TEST_METRIC_ID` 등이 실제 존재하는 데이터인지 확인
