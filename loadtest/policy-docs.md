# 정책 문서 (Policy Docs)

본 문서는 현재 코드 기준 정책/제약을 정리한 문서입니다.
- 기준 소스: `src/main/java`, `src/main/resources/application.yaml`
- 기준일: 2026-02-26

## 1. 공통 규칙
- 별도 `@Pattern`이 없으면 문자셋 제한은 없습니다.
- `@NotBlank`는 전체 공백 문자열을 금지합니다(앞/뒤 공백 자체는 허용).
- `@NotNull`/`@NotBlank`가 없으면 null 허용입니다.

## 2. 보안/인증 정책

### 2.1 JWT/토큰
- Issuer: `QFeed`
- Access Token 만료: `600000ms` (10분)
- Refresh Token 만료: `1209600000ms` (14일)
- Refresh Token 쿠키명: `refreshToken`

### 2.2 Refresh Cookie 정책
- `HttpOnly=true`
- `Path=/api/auth`
- `Secure=${cookie.secure}` (기본 true)
- `SameSite`: Secure=true면 `Strict`, Secure=false면 `Lax`
- `Max-Age`: Refresh 만료 시간(초)

### 2.3 RTR(Refresh Token Rotation)
- Refresh Token 원문은 저장하지 않고 해시(Base64(SHA-256))로 저장합니다.
- `TokenFamily` 단위로 재사용 탐지 및 폐기(revoke)합니다.
- TokenFamily 기본 수명: 30일(`TokenFamily.DEFAULT_FAMILY_LIFETIME_DAYS`)

### 2.4 Spring Security 동작
- 공통: Stateless, CSRF 비활성화, JWT 필터(`JwtAuthenticationFilter`) 항상 체인에 포함.
- `spring.security.is-authenticated=false` (현재 기본): `anyRequest().permitAll()`.
- `spring.security.is-authenticated=true`일 때:
- permitAll: `/`, `/error`, `/actuator/health`, `/actuator/prometheus`, `/actuator/info`, `/api/auth/tokens`
- anonymous-only: `/api/auth/oauth/authorization-url`, `/api/auth/oauth/{provider}/callback`, `/api/auth/oauth/exchange`
- 그 외는 인증 필요.

주의:
- 인증 비활성화 모드에서도 JWT 필터는 동작합니다.
- 잘못된 Bearer 토큰을 보내면 인증 예외로 401이 발생할 수 있습니다.

### 2.5 CORS
- Allowed Origins: `cors.allowed-origins`
- Methods: `GET, POST, PUT, PATCH, DELETE, OPTIONS`
- Allowed Headers: `*`
- Exposed Headers: `Authorization`
- Credentials: `true`
- Preflight Max-Age: `3600s`

### 2.6 Actuator
- Management 포트: `8081`
- 노출 엔드포인트: `health, metrics, info, prometheus`
- Health probes: enabled

## 3. Answer 도메인

### 3.1 답변 제출
- 연습 모드 `PracticeAnswerSubmitRequest`
- `sessionId`: `@NotBlank`
- `questionId`: `@NotNull`
- `answerText`: `@NotBlank`, `@Size(min=2, max=1500)`
- 실전 모드 `RealAnswerSubmitRequest`
- `sessionId`: `@NotBlank`
- `answerText`: `@NotBlank`
- `question`: `@NotBlank`
- `questionType`: 선택값
- 실전 답변 길이 정책은 `InterviewSubmissionValidator.resolveRealAnswerText()`에서 trim 후 2~1500자로 검증합니다.

### 3.2 도메인 검증
- `AnswerDomainServiceImpl.validateAnswerContent()`:
- 공백/빈 문자열 불가
- 길이 최대 1500자

### 3.3 DB 제약
- `answer.answer_content`: `TEXT` (길이 500 제한 아님)
- `answer.answer_type`: `PRACTICE_INTERVIEW`, `REAL_INTERVIEW`

### 3.4 답변 목록 조회(`GET /api/answers`)
- `limit`: 1~50, 기본 10
- `dateTo` 미입력 시 오늘, `dateFrom` 미입력 시 `dateTo - 1개월`
- `dateFrom > dateTo`면 예외
- `questionType=PORTFOLIO`는 허용하지 않음(`CS`, `SYSTEM_DESIGN`만 허용)
- `cursor`는 Base64(JSON: `lastCreatedAt`, `lastAnswerId`) 형식

### 3.5 답변 상세 조회(`GET /api/answers/{answerId}`)
- 쿼리스트링 미지원: query param 존재 시 `AnswerDetailInvalidInputException`
- 연습모드: question/immediateFeedback/aiFeedback 포함
- 실전모드: `sessionFinalFeedback` 포함(단, answer 상태가 `COMPLETED`이고 sessionId가 있을 때만)

## 4. Interview Session 정책

### 4.1 세션 생성
- `InterviewSessionCreateRequest`
- `interviewType`: 필수 (`PRACTICE_INTERVIEW`, `REAL_INTERVIEW`)
- `questionType`: 필수 (`CS`, `SYSTEM_DESIGN`, `PORTFOLIO`)

### 4.2 제출/상태 검증
- 공통: 종료/실패/만료 세션은 제출 불가
- 연습모드: 단일 turn만 허용(중복 제출 차단)
- 실전모드: `answerText` trim 길이 2~1500
- 실전모드 최종 피드백 요청은 인터뷰 종료 상태에서만 가능

### 4.3 TTL/정리
- 연습 TTL: `PT10M`
- 실전 TTL: `PT90M`
- 실전 최대 턴: `interview.session.real-max-turn` (0 이하 금지)
- 만료 세션 정리 스케줄: 기본 300000ms

### 4.4 최종 피드백 조회 쿼리 정책
- `getSessionFeedbackCompleted()`는 DB read-model 단일 조회를 사용합니다.
- 쿼리 형태: 1개의 메인 SELECT + `jsonb_agg/jsonb_build_object` 서브쿼리(메트릭/토픽/히스토리 집계).
- 선행 조건:
- 세션 소유자 일치
- 세션 상태 `COMPLETED`
- `interview_session_feedback` 존재

## 5. Question 도메인

### 5.1 생성/수정
- `content`: `@NotBlank`, `@Size(max=200)`
- 도메인에서 추가 검증:
- trim 전후 동일해야 함(앞/뒤 공백 금지)
- 길이 2~200
- `type`, `category`: 생성 시 필수
- `keywords`: 최대 5개, 각 항목 `@NotBlank`, `@Size(max=100)`

### 5.2 키워드 저장 정책
- 저장 전 `trim + lower-case`
- 중복 제거(distinct)

### 5.3 조회/검색 제한
- 질문 목록 size: 1~100 (기본 10)
- 질문 검색 size: 1~100 (기본 10)
- 검색어 최소 길이: trim 기준 2자
- 질문 타입 노출(`GET /api/questions/types`): `CS`, `SYSTEM_DESIGN`만 노출

### 5.4 질문 키워드 체크
- 최대 5개
- 각 키워드 최대 100자
- null/공백 불가

## 6. File/S3 정책

### 6.1 Presigned URL 요청
- PUT(또는 method 생략): `file_name`, `file_size(>=1)`, `mime_type`, `category` 필수
- GET/HEAD: `file_id` 필수

### 6.2 카테고리별 제한
- PROFILE: 5MB, `jpg/jpeg/png/gif/webp`, `image/jpeg|png|gif|webp`
- ARCHITECTURE: 10MB, `jpg/jpeg/png/gif/svg`, `image/jpeg|png|gif|svg+xml`
- ATTACHMENT: 50MB, `jpg/jpeg/png/gif`, `image/jpeg|png|gif`
- TEMP: 100MB, `jpg/jpeg/png/gif`, `image/jpeg|png|gif`
- AUDIO: 10MB, `mp3/wav/m4a`, `audio/mpeg|audio/wav|audio/x-m4a|audio/mp4`
- VIDEO: 100MB, `mp4/mov/avi`, `video/mp4|video/quicktime|video/x-msvideo`

### 6.3 S3 Key/URL 규칙
- Presigned 만료: 300초(5분)
- S3 key: `{category.s3Directory}/{originalFileName}`
- `AUDIO -> audio/`, `VIDEO -> video/`, 그 외 카테고리 -> `image/`
- `storedName(UUID)`를 엔티티에 저장하지만 현재 key 생성에는 사용하지 않습니다.
- 업로드 확인 시 `cdn-url-prefix + "/" + s3Key`를 파일 URL로 저장합니다.

### 6.4 파일 엔티티 제약
- originalName <= 500
- storedName <= 500
- path <= 1000
- extension <= 20
- mimeType <= 100
- url <= 2000
- hash 길이 64(SHA-256)
- TEMP 자동 정리 기준: 생성 후 1일
- soft-delete 파일 영구 삭제 기준: 삭제 후 7일

## 7. AI 연동 정책

### 7.1 STT (`SttRequest`)
- `userId`: `@NotNull`
- `sessionId`: 선택
- `audioUrl`: `@NotBlank`
- STT 4xx 매핑:
- 400: 오디오 길이/크기 초과
- 404: 파일 없음
- 422: 처리 불가
- 429: 요청 한도 초과

### 7.2 TTS (`TtsRequest`)
- `userId`: `@NotNull`
- `sessionId`: `@NotBlank`
- `text`: `@NotBlank`

### 7.3 인터뷰 피드백 요청 DTO
- 내부 DTO(`InterviewFeedbackRequest`)는 Bean Validation 어노테이션 없음
- 값 구성/검증은 오케스트레이션 레이어에서 수행

### 7.4 HTTP 타임아웃
- AI RestClient connect timeout: 10초
- AI RestClient read timeout: 300초
- 참고: `application.yaml`의 `ai.*.timeout(60초)`는 현재 RestClient 타임아웃 설정에 직접 반영되지 않습니다.

## 8. Metric 도메인
- Metric 이름 DTO 검증: 최대 30자
- Metric 도메인/DB 상한: 100자
- Metric 설명: 최대 255자
- AnswerMetric 점수 범위: 1~5
- Metric 목록 size: 1~100 (기본 10)

## 9. 운영/부하테스트 참고
- 현재 `application.yaml`에서 `abuse.guard.*.enabled=false`(rate-limit/content/duplicate/quality/quota 비활성화)
- 순수 백엔드 성능 측정 시 위 설정을 유지하고, 인증은 유효 토큰 기반으로 호출하는 것을 권장
