# 부하테스트 대상 API 명세 (Cloud Team)

- 문서 버전: 3.0
- 작성일: 2026-02-26
- 목적: FE 기준 사이클을 그대로 구현할 수 있는 최소 API 계약 제공
- 관련 가이드: `docs/load-test-guide.md`

## 1. 공통

## 1.1 Base URL

- API: `http://{HOST}:8080`

## 1.2 인증

- 기본: `Authorization: Bearer {ACCESS_TOKEN}`
- 만료 시: `POST /api/auth/tokens` 호출 후 응답 헤더 `Authorization` 신규 토큰 사용

## 1.3 공통 응답 형식

성공:

```json
{
  "message": "string",
  "data": {}
}
```

실패:

```json
{
  "code": "AI013",
  "message": "에러 메시지",
  "detail": "상세 원인",
  "timestamp": "2026-02-21T06:12:22.522016Z",
  "path": "/api/interview/sessions/feedback"
}
```

## 1.4 BE 부하 범위

- 포함: `/api/**`
- 제외: `PUT {presignedUrl}` (S3 직접 업로드)

## 2. FE 기준 호출 맵

기준:

1. 시작 화면: `/` (홈)
2. 연습모드: 음성 답변 경로 1회 제출 기준
3. 실전모드: 답변 1회 제출 후 분기 포함
4. FE -> S3 직접 PUT은 별도 표기

## 2.1 홈 공통 (`/`)

1. `GET /api/users/me/stats/weekly`: 홈 주간 통계 위젯 데이터 조회
2. `GET /api/questions/categories`: 질문 카테고리 필터 데이터 조회
3. `GET /api/questions/recommendation`: 홈 추천 질문 1건 조회

## 2.2 연습모드 1사이클

1. `GET /api/questions/categories`: 연습 카테고리 필터 초기화
2. `GET /api/questions/types`: 연습 타입 필터 초기화
3. `GET /api/questions?size=10`: 초기 질문 목록 조회
4. `[선택]` `GET /api/questions?...&cursor=...` (무한스크롤): 다음 페이지 조회
5. `[선택]` `GET /api/questions/search?q=...&size=10` (검색): 키워드 질문 조회
6. `POST /api/interview/sessions` (`PRACTICE_INTERVIEW`): 연습 답변 세션 생성
7. `POST /api/files/presigned-url`: 음성 파일 업로드 URL/`fileId` 발급
8. `PUT {presignedUrl}` (S3 직접 업로드): 음성 파일 전송
9. `POST /api/files/{fileId}/confirm`: 업로드 완료 확정
10. `POST /api/ai/stt`: 음성 -> 텍스트 변환
11. `POST /api/answers/practice`: 연습 답변 저장
12. `POST /api/interview/sessions/feedback/request`: 최종 피드백 생성 트리거

참고:

- 텍스트 답변이면 `7~10` 생략
- 추천 질문에서 바로 진입하면 `GET /api/questions/{questionId}`가 먼저 호출될 수 있음

## 2.3 실전모드 1사이클

1. `POST /api/interview/sessions` (`REAL_INTERVIEW`): 실전 면접 세션 시작
2. `POST /api/ai/tts` (첫 질문): 질문 음성 재생
3. `POST /api/files/presigned-url`: 답변 음성 업로드 URL 발급
4. `PUT {presignedUrl}` (S3 직접 업로드): 답변 음성 전송
5. `POST /api/files/{fileId}/confirm`: 업로드 완료 확정
6. `POST /api/ai/stt`: 음성 답변 텍스트 변환
7. `POST /api/answers/real`: 턴 처리 및 분기 계산
8. 분기 A: `message=bad_case_detected` -> `POST /api/ai/tts` (재답변 안내)
9. 분기 B: `is_final=false` -> `POST /api/ai/tts` (다음 질문 안내)
10. 분기 C: `is_final=true` -> `POST /api/ai/tts` (종료 멘트 안내)
11. `POST /api/interview/sessions/feedback/request`: 최종 분석 실행
12. `[선택]` `GET /api/interview/sessions/feedback?sessionId=...` (폴링): 처리 상태 확인

## 3. 엔드포인트 상세

## 3.1 `POST /api/auth/tokens`

- 목적: Access Token 재발급
- 요청: `Cookie: refreshToken={...}`
- 성공: `200`, `message=token_refresh_success`
- 검증: 응답 헤더 `Authorization` 존재

## 3.2 `GET /api/users/me/stats/weekly`

- 목적: 홈 주간 통계
- 성공: `200`, `message=weekly_stats_retrieval_success`
- 검증: `data.week_summary`, `data.daily_stats` 존재

## 3.3 `GET /api/questions/categories`

- 목적: 홈 카테고리 목록
- 성공: `200`, `message=question_categories_retrieval_success`

## 3.4 `GET /api/questions/types`

- 목적: 연습 질문 타입 목록
- 성공: `200`, `message=question_types_retrieval_success`

## 3.5 `GET /api/questions?size=10`

- 목적: 연습 질문 리스트 조회
- 성공: `200`, `message=questions_retrieval_success`
- 확장:
  - `cursor` 기반 페이지네이션
  - `type`, `category` 필터

## 3.6 `GET /api/questions/search?q=...&size=10`

- 목적: 연습 질문 검색
- 성공: `200`, `message=search_success`

## 3.7 `GET /api/questions/recommendation`

- 목적: 홈 추천 질문
- 성공: `200`, `message=question_recommendation_success`

## 3.8 `GET /api/questions/{questionId}`

- 목적: 문제 상세 로드
- 성공: `200`, `message=question_retrieval_success`

## 3.9 `POST /api/files/presigned-url`

- 목적: S3 업로드 URL 발급
- 성공: `200`, `message=presigned_url_generated`
- 요청 예시:

```json
{
  "file_name": "answer.m4a",
  "file_size": 1048576,
  "mime_type": "audio/mp4",
  "category": "AUDIO",
  "method": "PUT"
}
```

- 검증: `data.file_id`, `data.presigned_url` 존재

## 3.10 `POST /api/files/{fileId}/confirm`

- 목적: 업로드 완료 확인
- 성공: `200`, `message=upload_confirmed`
- 검증: `data.status == "UPLOADED"`

## 3.11 `POST /api/ai/stt`

- 목적: 음성 텍스트 변환
- 성공: `200`, `message=speech_to_text_success`
- 요청 예시:

```json
{
  "user_id": 1,
  "session_id": "<questionId-string | real-session-id-string>",
  "audio_url": "https://.../audio-file.m4a"
}
```

- 검증: `data.text` 존재

## 3.12 `POST /api/interview/sessions`

- 목적: 연습/실전 세션 생성
- 성공: `201`, `message=session_created_success`
- 요청 예시:

```json
{
  "interviewType": "PRACTICE_INTERVIEW | REAL_INTERVIEW",
  "questionType": "CS"
}
```

- 검증: `data.session_id` 존재

## 3.13 `POST /api/answers/practice`

- 목적: 연습 답변 제출
- 성공: `201`, `message=answer_submission_success`

## 3.14 `POST /api/answers/real`

- 목적: 실전 답변 제출
- 성공: `201`, `message=generate_feedback_success | bad_case_detected`
- 요청 예시:

```json
{
  "session_id": "10151031-858c-4158-93b9-a3f4b766975f",
  "answer_text": "답변 본문",
  "question_type": "CS",
  "question": "현재 질문 본문"
}
```

- 검증: `data.session_id` 존재, `data.is_final` boolean

## 3.15 `POST /api/ai/tts`

- 목적: 다음 질문/멘트 TTS 변환
- 성공: `200`
- 응답: `multipart/mixed` (JSON 아님)
- 검증: 응답 `Content-Type`에 `multipart/mixed`

## 3.16 `POST /api/interview/sessions/feedback/request`

- 목적: 최종 피드백 생성
- 성공: `201`, `message=generate_feedback_success`
- 요청 예시:

```json
{
  "sessionId": "10151031-858c-4158-93b9-a3f4b766975f"
}
```

- 검증: `data.status == "COMPLETED"`

## 3.17 `GET /api/interview/sessions/feedback?sessionId=...`

- 목적: 최종 피드백 상태/결과 폴링
- 상태:
  - `200` 완료 또는 실패
  - `202` 처리중
- 검증:
  - `status in [200, 202]`
  - 완료 시 `data.status == "COMPLETED"`

## 4. 최소 Assert 권장

- 공통:
  - HTTP `5xx` 비율 `< 1%`
  - 핵심 식별자(`session_id`, `file_id`) null 아님
- S3 제외:
  - `PUT presignedUrl`은 BE 지표에서 분리
- TTS:
  - 본문 JSON 파싱 대신 상태코드/헤더 기반 검증
- 실전 분기:
  - `bad_case_detected / is_final` 기준 분기 호출 검증
