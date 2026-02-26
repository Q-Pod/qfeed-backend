# 클라우드팀 핸드오프 가이드 (Load Test Enablement)

- 문서 버전: 5.1
- 작성일: 2026-02-26
- 목적: 클라우드팀이 시나리오 작성에만 집중할 수 있도록 서버/데이터/API 기준 제공
- 관련 문서: `docs/load-test-api-spec.md`

### 본 문서는 V2 기능 배포 완료 기준으로 작성한 문서입니다. 
### V2 DB 마이그레이션 -> V2 기능 배포 후 진행을 권장합니다잉

## 1. 범위

백엔드 팀 제공:

- 현재 스키마 호환 더미 데이터 SQL
- 토큰 생성 스크립트/토큰 파일
- FE 기준 호출 순서 기반 API 명세/추천 사이클

클라우드팀 수행:

- 부하 시나리오 구현(k6/JMeter 등)
- 부하 프로파일(사용자수, 램프업, 지속시간) 설계
- 결과 리포트 작성

## 2. 제공 산출물

- 더미 데이터 SQL: `loadtest/seed_dummy.sql`
- 토큰 생성기: `loadtest/generate_user_tokens.mjs`
- 토큰 파일(DB 시드용): `loadtest/generated/user-token-list.csv`
- 토큰 파일(시나리오용): `loadtest/generated/user-access-token-list.csv`
- API 명세: `loadtest/load-test-api-spec.md`
- 참고 스크립트(선택): `loadtest/test.js` (클라우드팀 별도 시나리오 작성 권장)

## 3. 실행 명령어

1. 토큰 생성기 옵션 확인

```bash
node generate_user_tokens.mjs --help
```

2. 토큰 CSV 생성 (`USER_COUNT` 조정 가능)

```bash
USER_COUNT=100
node generate_user_tokens.mjs \
  --user-count "${USER_COUNT}" \
  --start-user-id 1 \
  --out-dir ./generated
```

3. 더미 데이터 적재 (`user_count` 파라미터 전달)

```bash
USER_COUNT=100
psql -h {DB_HOST} -p {DB_PORT} -U {DB_USER} -d {DB_NAME} \
  -v user_count="${USER_COUNT}" \
  -f seed_dummy.sql
```

4. 적재 확인 (권장)

```bash
psql -h {DB_HOST} -p {DB_PORT} -U {DB_USER} -d {DB_NAME} -c "
SELECT 'users' AS key, count(*) AS value FROM public.user_account
UNION ALL
SELECT 'questions', count(*) FROM public.question
UNION ALL
SELECT 'practice_answers', count(*) FROM public.answer WHERE answer_type = 'PRACTICE_INTERVIEW'
UNION ALL
SELECT 'real_answers', count(*) FROM public.answer WHERE answer_type = 'REAL_INTERVIEW';
"
```

5. (선택) 백엔드 참고 스크립트 실행

```bash
k6 run test.js \
  --env BASE_URL=http://{HOST}:8080 \
  --env USER_TOKEN_CSV=generated/user-access-token-list.csv \
  --env DURATION=5m
```

기본값:

- Access Token 만료: `21600초(6시간)`
- Refresh Token 만료: `1209600초(14일)`

## 4. 더미 데이터 구성 기준

- 사용자: `100명`
- 사용자별 이력:
  - 연습모드 `10회`
  - 실전모드 `10회`
- 질문 풀:
  - 타입/카테고리별 `10개`
  - `PORTFOLIO` 타입 제외
- 인증 데이터:
  - RTR family/refresh token 포함
  - 사용자별 Access/Refresh token CSV 포함

## 5. FE 기준 호출 시퀀스

기준:

1. 시작 화면: `/` (홈)
2. 연습모드: 음성 답변 경로 1회 제출 기준
3. 실전모드: 답변 1회 제출 후 분기까지 포함
4. FE -> S3 직접 PUT은 별도 표기

## 5.1 홈 진입 공통 (`/`)

1. `GET /api/users/me/stats/weekly`: 홈 상단 주간 학습 카드(요약/차트) 렌더링 데이터 로드
2. `GET /api/questions/categories`: 카테고리 탭/필터 UI 초기화
3. `GET /api/questions/recommendation`: 홈 추천 질문 카드 노출

## 5.2 연습모드 1사이클 (홈 -> 연습모드 -> 음성답변 -> 제출)

1. `GET /api/questions/categories`: 연습 화면 카테고리 필터 칩 구성
2. `GET /api/questions/types`: 질문 타입(CS/시스템디자인 등) 필터 칩 구성
3. `GET /api/questions?size=10`: 초기 질문 목록 1페이지 로드
4. `[선택]` 무한스크롤 시 `GET /api/questions?...&cursor=...` 반복: 다음 페이지 추가 로드
5. `[선택]` 검색 시 `GET /api/questions/search?q=...&size=10`: 키워드 기반 질문 탐색
6. 질문 선택 후 세션 생성
   - `POST /api/interview/sessions`
   - Body: `{"interviewType":"PRACTICE_INTERVIEW","questionType":"CS|SYSTEM_DESIGN|..."}`
   - 이유: 답변 제출/피드백 요청의 기준이 되는 연습 세션 확보
7. 음성 업로드 Presigned 발급: `POST /api/files/presigned-url`
   - 이유: 클라이언트가 S3에 직접 업로드할 임시 URL/`fileId` 발급
8. `PUT <presignedUrl>` (S3 직접 업로드, FE->S3)
   - 이유: 실제 음성 파일 전송 (백엔드 우회)
9. 업로드 확인: `POST /api/files/{fileId}/confirm`
   - 이유: 서버가 파일 상태를 `UPLOADED`로 확정하고 후속 처리 가능 상태로 전환
10. STT 요청
   - `POST /api/ai/stt`
   - Body: `{"user_id":1,"session_id":"<questionId-string>","audio_url":"<s3-url>"}`
   - 이유: 음성 파일을 텍스트 답변으로 변환
11. 연습 답변 제출
   - `POST /api/answers/practice`
   - Body: `{"sessionId":"...","questionId":123,"answerText":"..."}`
   - 이유: 세션 이력에 사용자 답변 기록
12. 최종 피드백 생성 요청
   - `POST /api/interview/sessions/feedback/request`
   - Body: `{"sessionId":"..."}`
   - 이유: 누적 이력 기반 최종 AI 피드백 생성 트리거

참고:

- 텍스트 답변이면 `7~10` (파일/STT 체인) 없음
- 홈의 추천 질문에서 바로 진입하면 `1~5` 일부를 건너뛰고 `GET /api/questions/{id}`가 먼저 호출될 수 있음

## 5.3 실전모드 1사이클 (홈 -> 실전카드 -> 답변 1회)

1. 실전 세션 생성
   - `POST /api/interview/sessions`
   - Body: `{"interviewType":"REAL_INTERVIEW","questionType":"CS|SYSTEM_DESIGN"}`
   - 이유: 실전 면접 컨텍스트(현재 질문/턴/토픽) 시작점 확보
2. 첫 질문 TTS 재생
   - `POST /api/ai/tts`
   - Body: `{"user_id":...,"session_id":"...","text":"<question_text>"}`
   - 이유: 질문 텍스트를 음성으로 재생해 실전 UX 제공
3. Presigned 발급: `POST /api/files/presigned-url`
   - 이유: 답변 음성 업로드용 임시 URL/`fileId` 발급
4. `PUT <presignedUrl>` (S3 직접 업로드, FE->S3)
   - 이유: 실제 답변 음성 파일 전송
5. 업로드 확인: `POST /api/files/{fileId}/confirm`
   - 이유: 업로드 완료 상태 확정
6. STT 요청
   - `POST /api/ai/stt`
   - Body: `{"user_id":...,"session_id":"<real-session-id-string>","audio_url":"<s3-url>"}`
   - 이유: 음성 답변을 텍스트로 변환
7. 실전 답변 제출
   - `POST /api/answers/real`
   - Body: `{"session_id":"...","question_type":"CS","question":"...","answer_text":"..."}`
   - 이유: 현재 턴 채점/분기(배드케이스, 다음 질문, 종료) 결정
8. 분기 A: `message=bad_case_detected`
   - `POST /api/ai/tts` with `bad_case_feedback.message` (재녹화 유도)
   - 이유: 부적절/부족 답변 시 재안내 음성 제공
9. 분기 B: `is_final=false`
   - `POST /api/ai/tts` with `next_question.content` (다음 턴 진행)
   - 이유: 다음 질문을 음성으로 재생하고 인터뷰 지속
10. 분기 C: `is_final=true`
    - `POST /api/ai/tts` with 종료 멘트 `next_question.content`
    - 이유: 면접 종료 안내 음성 재생
11. AI 분석하기 클릭 시
    - `POST /api/interview/sessions/feedback/request`
    - Body: `{"sessionId":"..."}`
    - 이유: 실전 전체 히스토리 최종 분석 실행
12. `[선택]` 필요 시 폴링
    - `GET /api/interview/sessions/feedback?sessionId=...`
    - 이유: 비동기 처리 상태(진행중/완료/실패) 확인

## 6. BE 부하 측정 범위

- 포함: `/api/**` 백엔드 엔드포인트
- 제외: `PUT presignedUrl` (S3 직접 업로드 트래픽)

## 7. 인증 부담 최소화 가이드

1. 기본: `user-access-token-list.csv`의 Access Token 사용
2. 만료 시: `POST /api/auth/tokens`로 재발급


## 8. 추천 사이클 조합 (시작점)

- Mix A: `홈 공통 1회 + 연습 1사이클`
- Mix B: `홈 공통 1회 + 실전 1사이클`
- Mix C: `Mix A 70% + Mix B 30%`
