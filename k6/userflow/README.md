# User Flow Load Test Guide

QFeed 유저 플로우 부하테스트를 `k6/userflow` 모듈만으로 독립 실행하기 위한 운영 가이드입니다.

## 1) 대상 스크립트 (독립 실행)

실제 실행 파일은 아래 3개입니다.

- `k6/userflow/practiceflow.js`: 연습모드 단독 사이클
- `k6/userflow/realflow.js`: 실전모드 단독 사이클
- `k6/userflow/interview_flow.js`: 연습+실전 혼합 사이클

공통 모듈:

- `k6/userflow/setup.js`: 토큰 풀 구성 및 VU 분배
- `k6/userflow/flow_utils.js`: 인증/공통 API/S3 mock/피드백 폴링 유틸

## 2) 빠른 시작

### 2.1 기본 실행

```bash
# 연습모드
k6 run k6/userflow/practiceflow.js \
  --env BASE_URL=http://localhost:8080 \
  --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv \
  --env PRACTICE_RATE=6

# 실전모드
k6 run k6/userflow/realflow.js \
  --env BASE_URL=http://localhost:8080 \
  --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv \
  --env REAL_RATE=2 \
  --env REAL_TURN_LIMIT=4

# 혼합모드 (연습:실전 = 6:2)
k6 run k6/userflow/interview_flow.js \
  --env BASE_URL=http://localhost:8080 \
  --env USER_TOKEN_CSV=loadtest/generated/user-access-token-list.csv \
  --env PRACTICE_RATE=6 \
  --env REAL_RATE=2 \
  --env REAL_TURN_LIMIT=4
```

### 2.2 CSV 없이 실행 (JSON 토큰 풀)

```bash
k6 run k6/userflow/interview_flow.js \
  --env BASE_URL=http://localhost:8080 \
  --env USER_TOKENS_JSON='[{"user_id":1,"access_token":"...","refresh_token":"..."},{"user_id":2,"access_token":"...","refresh_token":"..."}]' \
  --env PRACTICE_RATE=2 \
  --env REAL_RATE=1
```

### 2.3 단일 토큰 실행 (개발/디버그 용도)

```bash
k6 run k6/userflow/practiceflow.js \
  --env BASE_URL=http://localhost:8080 \
  --env ACCESS_TOKEN=eyJ... \
  --env REFRESH_TOKEN=eyJ... \
  --env PRACTICE_RATE=1 \
  --env PRACTICE_MAX_VUS=1
```

## 3) 토큰 입력 방식

`setup.js` 기준 우선순위:

1. `USER_TOKENS_JSON`
2. `ACCESS_TOKEN` (+ 선택: `REFRESH_TOKEN`, `SINGLE_USER_ID`, `ACCESS_EXPIRES_AT`)
3. `USER_TOKEN_CSV`

CSV 헤더 권장:

```csv
user_id,access_token,refresh_token,access_expires_at
1,eyJ...,eyJ...,2026-03-05 10:30:00
```

## 4) 환경변수 옵션

### 4.1 공통

| 변수 | 기본값 | 설명 |
|---|---|---|
| `BASE_URL` | `http://localhost:8080` | API 서버 주소 |
| `DURATION` | `5m` | 시나리오 실행 시간 |
| `ENABLE_HOME_CALLS` | `true` | 홈 공통 호출 수행 여부 |
| `ENABLE_FEEDBACK_POLL` | `false` | 피드백 폴링 수행 여부 |
| `MOCK_AI` | `true` | AI 호출(STT/TTS) mock 여부 |
| `MOCK_STT` | `MOCK_AI` | STT mock 여부 |
| `MOCK_TTS` | `MOCK_AI` | TTS mock 여부 |
| `MOCK_STT_LATENCY_SEC` | `0.05` | STT mock 지연(초) |
| `MOCK_TTS_LATENCY_SEC` | `0.03` | TTS mock 지연(초) |
| `MOCK_S3_UPLOAD` | `true` | S3 PUT 외부 호출 mock 여부 |
| `MOCK_S3_LATENCY_SEC` | `0.03` | mock 업로드 지연(초) |
| `ALLOW_STT_FALLBACK` | `true` | STT 실패 시 텍스트 fallback 허용 |
| `TOKEN_MARGIN_SEC` | `30` | access token 만료 선갱신 여유(초) |
| `FEEDBACK_POLL_MAX` | `15` | 피드백 폴링 최대 횟수 |
| `FEEDBACK_POLL_INTERVAL_SEC` | `0.3` | 피드백 폴링 간격(초) |
| `DEFAULT_ANSWER_TEXT` | 내부 기본문구 | STT fallback 답변 |
| `DEFAULT_FINAL_TTS_TEXT` | 내부 기본문구 | 실전 종료 TTS 기본 멘트 |

### 4.2 토큰 풀

| 변수 | 기본값 | 설명 |
|---|---|---|
| `USER_TOKEN_CSV` | 자동 탐색 | CSV 파일 경로 |
| `USER_TOKENS_JSON` | 빈값 | JSON 문자열 토큰 배열 |
| `ACCESS_TOKEN` | 빈값 | 단일 유저 Access Token |
| `REFRESH_TOKEN` | 빈값 | 단일 유저 Refresh Token |
| `SINGLE_USER_ID` | `1` | 단일 유저 모드 user_id |
| `ACCESS_EXPIRES_AT` | 빈값 | Access Token 만료 시각 |

### 4.3 연습모드 (`practiceflow.js`)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `PRACTICE_RATE` | `6` | 초당 사이클 시작 수 |
| `PRACTICE_PRE_ALLOCATED_VUS` | `20` | 초기 VU |
| `PRACTICE_MAX_VUS` | `60` | 최대 VU |
| `PRACTICE_USE_CURSOR_RATE` | `0.2` | 다음 페이지 조회 분기 확률 (`0=비활성`, `1=매 사이클 실행`) |
| `PRACTICE_SEARCH_RATE` | `0.2` | 검색 호출 분기 확률 (`0=비활성`, `1=매 사이클 실행`) |
| `PRACTICE_RECOMMEND_ENTRY_RATE` | `0` | 추천 질문 상세 진입 분기 확률 (`0=비활성`, `1=매 사이클 실행`) |
| `PRACTICE_CONTEXT_MISS_RATE` | `0.1` | 컨텍스트 미스 재조회 분기 확률 (`0=비활성`, `1=매 사이클 실행`) |
| `PRACTICE_VOICE_RATE` | `1.0` | 음성 경로 비율 (`0=항상 텍스트`, `1=항상 음성`) |

### 4.4 실전모드 (`realflow.js`)

| 변수 | 기본값 | 설명 |
|---|---|---|
| `REAL_RATE` | `2` | 초당 사이클 시작 수 |
| `REAL_PRE_ALLOCATED_VUS` | `10` | 초기 VU |
| `REAL_MAX_VUS` | `40` | 최대 VU |
| `REAL_TURN_LIMIT` | `4` | 질문/답변 최대 턴 |
| `REAL_SESSION_STATE_RATE` | `0.2` | 세션 상태 조회 분기 확률 (`0=비활성`, `1=매 사이클 실행`) |
| `REAL_VIDEO_PARTS` | `2` | 턴당 비디오 multipart part 수 |
| `REAL_THINK_SEC` | `0.2` | 턴 간 think time(초) |
| `ALLOW_TTS_FAILURE` | `false` | TTS 실패 시 사이클 실패 무시 여부 |
| `REAL_USER_OFFSET` | `0` | 유저 인덱스 시작 오프셋 |

### 4.5 혼합모드 (`interview_flow.js`)

`PRACTICE_*`, `REAL_*`를 함께 사용합니다.

- 비율 조절: `PRACTICE_RATE : REAL_RATE`
- 사용자 분리: practice 구간 다음 offset부터 real 구간 할당

### 4.6 분기 확률 변수 해석 가이드

분기 확률 변수(`*_RATE`)는 모두 `0.0 ~ 1.0` 범위를 사용합니다.

- `0`: 해당 분기 호출 비활성
- `1`: 해당 분기 호출 항상 실행
- `0.3`: 평균적으로 약 30% 사이클에서 실행

왜 필요한가:

- FE as-is에서도 검색, 다음 페이지, 컨텍스트 미스 재조회, 세션 상태 조회는 사용자 행동에 따라 선택적으로 발생합니다.
- `*_RATE`는 이 선택 호출의 빈도를 조절해, 실제 사용자 분포를 근사하거나 의도적으로 특정 API를 더 강하게 압박할 때 사용합니다.
- 고정 시나리오만 반복하면 특정 분기 API가 과소/과대 측정될 수 있어, 분기 확률로 트래픽 분포를 맞춥니다.

예시:

- `PRACTICE_RATE=20`, `PRACTICE_SEARCH_RATE=0.25`이면 검색 호출은 초당 약 `5`회 수준으로 기대됩니다.
- `REAL_RATE=10`, `REAL_SESSION_STATE_RATE=0.2`이면 세션 상태 조회는 초당 약 `2`회 수준입니다.

주의:

- 실제 순간값은 랜덤 분산이 있어 기대값과 완전히 일치하지 않을 수 있습니다.
- 분기 확률을 높이면 API 종류가 늘어나고, 동일 rate 대비 총 요청 수가 증가합니다.
- `PRACTICE_VOICE_RATE`는 특히 영향이 큽니다. 음성 경로는 `files + STT (+S3)` 트래픽이 추가됩니다.

변수별 호출 영향:

| 변수 | 분기 의미 | 추가 호출(대략) | 기대 추가 호출량(초당, 근사) |
|---|---|---|---|
| `PRACTICE_USE_CURSOR_RATE` | 질문 목록 다음 페이지 조회 | `GET /api/questions?...&cursor=...` | `PRACTICE_RATE * PRACTICE_USE_CURSOR_RATE * hasNext확률` |
| `PRACTICE_SEARCH_RATE` | 키워드 검색 수행 | `GET /api/questions/search?...` | `PRACTICE_RATE * PRACTICE_SEARCH_RATE` |
| `PRACTICE_RECOMMEND_ENTRY_RATE` | 추천 질문 상세 진입 시도 | `GET /api/questions/recommendation` + (조건 충족 시) `GET /api/questions/{id}` | `PRACTICE_RATE * PRACTICE_RECOMMEND_ENTRY_RATE * (1~2 호출)` |
| `PRACTICE_CONTEXT_MISS_RATE` | 컨텍스트 미스 재조회 | `GET /api/questions/{questionId}` | `PRACTICE_RATE * PRACTICE_CONTEXT_MISS_RATE` |
| `PRACTICE_VOICE_RATE` | 텍스트 대신 음성 경로 사용 | `POST /api/files/presigned-url`, `POST /api/files/{fileId}/confirm`, `POST /api/ai/stt` (+ S3 PUT) | `PRACTICE_RATE * PRACTICE_VOICE_RATE` |
| `REAL_SESSION_STATE_RATE` | 실전 세션 상태 조회 | `GET /api/interview/sessions?sessionId=...` | `REAL_RATE * REAL_SESSION_STATE_RATE` |

참고:

- `PRACTICE_VOICE_RATE`는 연습 사이클의 텍스트/음성 비율을 정하는 핵심 변수입니다.
- `REAL_VIDEO_PARTS`는 확률 변수가 아니라, 음성+영상 업로드 중 영상 multipart의 파트 개수를 정하는 부하 크기 변수입니다.
- 실전의 `bad_case/final` 분기는 현재 응답(`POST /api/answers/real`) 결과에 의해 결정되며, 별도 확률 env 변수로 강제하지 않습니다.

권장 시작값 (FE as-is 근사):

- `PRACTICE_VOICE_RATE=1`
- `PRACTICE_CONTEXT_MISS_RATE=0.1`
- `PRACTICE_SEARCH_RATE=0.2`
- `REAL_SESSION_STATE_RATE=0.2`
- `REAL_VIDEO_PARTS=2`

## 5) 부하테스트 사이클

## 5.1 홈 공통 호출

모든 사이클에서 옵션(`ENABLE_HOME_CALLS=true`)으로 수행됩니다.

1. `GET /api/users/me/stats/weekly`
2. `GET /api/questions/categories`
3. `GET /api/questions/recommendation`

## 5.2 연습모드 1사이클 (텍스트)

1. `GET /api/questions/categories`
2. `GET /api/questions/types`
3. `GET /api/questions?type=...&category=...&cursor=...&size=10`
4. 선택: `GET /api/questions/search?q=...&type=...&category=...&cursor=...&size=10`
5. 선택: `GET /api/questions/{questionId}` (컨텍스트 미스 시)
6. `POST /api/interview/sessions` (`PRACTICE_INTERVIEW`)
7. `POST /api/answers/practice`
8. `POST /api/interview/sessions/feedback/request`

## 5.3 연습모드 1사이클 (음성)

1. `POST /api/files/presigned-url` (`method=PUT`, `category=AUDIO`)
2. `PUT {presignedUrl}` (S3)
3. `POST /api/files/{fileId}/confirm`
4. `POST /api/ai/stt`
5. `POST /api/interview/sessions` (`PRACTICE_INTERVIEW`)
6. `POST /api/answers/practice`
7. `POST /api/interview/sessions/feedback/request`

참고:

- 현재 FE as-is 기준으로 음성 경로는 STT가 세션 생성보다 먼저 발생할 수 있습니다.
- 현재 FE as-is 기준으로 연습모드는 `GET /api/interview/sessions/feedback` 폴링을 사용하지 않습니다.

## 5.4 실전모드 1사이클

1. `POST /api/interview/sessions` (`REAL_INTERVIEW`)
2. 선택: `GET /api/interview/sessions?sessionId=...`
3. `POST /api/ai/tts` (첫 질문)
4. 턴 반복 (`REAL_TURN_LIMIT`, 기본 4)
5. `POST /api/files/presigned-url` (audio)
6. `PUT {audioPresignedUrl}` (S3)
7. `POST /api/files/{audioFileId}/confirm`
8. `POST /api/files/presigned-url` (video multipart 시작)
9. `POST /api/files/{videoFileId}/multipart/parts` (part N회)
10. `PUT {videoPartPresignedUrl}` (S3, part N회)
11. `POST /api/files/{videoFileId}/multipart/complete`
12. `POST /api/ai/stt`
13. `POST /api/answers/real`
14. 분기 A: bad case 감지 -> `POST /api/ai/tts` 안내
15. 분기 B: final=false -> `POST /api/ai/tts` 다음 질문
16. 분기 C: final=true -> `POST /api/ai/tts` 종료 멘트
17. `POST /api/interview/sessions/feedback/request`

참고:

- 기본 설정(`MOCK_AI=true`)에서는 `POST /api/ai/stt`, `POST /api/ai/tts`를 실제 호출하지 않습니다.
- 현재 FE as-is 기준으로 실전모드도 `GET /api/interview/sessions/feedback` 폴링을 사용하지 않습니다.

## 6) 시나리오 구성

### 6.1 Practice 단독

- executor: `constant-arrival-rate`
- scenario name: `practice_cycle`
- 실패 지표: `practice_cycle_failed`
- 지연 지표: `practice_cycle_duration_ms`

### 6.2 Real 단독

- executor: `constant-arrival-rate`
- scenario name: `real_cycle`
- 실패 지표: `real_cycle_failed`
- 지연 지표: `real_cycle_duration_ms`

### 6.3 혼합

- `practice_cycle` + `real_cycle` 동시 실행
- 유저 인덱스 충돌 방지: practice span과 real span 분리
- 필요 토큰 수: `practice user span + real user span`

## 7) 임계값(Threshold) 기본값

- 공통: `http_req_failed < 0.10`
- 연습: `practice_cycle_failed < 0.20`, `practice_cycle_duration_ms p(95) < 15000`
- 실전: `real_cycle_failed < 0.25`, `real_cycle_duration_ms p(95) < 20000`

## 8) 운영 체크리스트

1. API 서버 실행 상태 확인 (`BASE_URL` 응답 가능)
2. 테스트 사용자 토큰 풀 수량 확인
3. mock 정책 확인 (`MOCK_S3_UPLOAD=true` 권장)
4. 실전 턴 수 확인 (`REAL_TURN_LIMIT`, 기본 4)
5. 실행 전 `k6 inspect`로 옵션 검증

```bash
k6 inspect k6/userflow/practiceflow.js --env ACCESS_TOKEN=dummy
k6 inspect k6/userflow/realflow.js --env ACCESS_TOKEN=dummy
k6 inspect k6/userflow/interview_flow.js --env ACCESS_TOKEN=dummy
```

## 9) 자주 발생하는 오류

- `token pool is empty`
  - `USER_TOKENS_JSON`, `ACCESS_TOKEN`, `USER_TOKEN_CSV` 중 하나를 반드시 제공
- `not enough users in token pool`
  - 토큰 수를 `maxVUs` 기준으로 늘리거나 `PRACTICE_MAX_VUS`/`REAL_MAX_VUS`를 낮춤
- `refresh 401/실패`
  - `REFRESH_TOKEN` 유효성 확인 또는 토큰 재발급
- `feedback poll timeout`
  - `FEEDBACK_POLL_MAX`/`FEEDBACK_POLL_INTERVAL_SEC` 조정
