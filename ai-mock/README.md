# AI Mock Server

Spring 백엔드 연동/부하 테스트용 FastAPI Mock 서버입니다.  
요청/응답 스키마와 API 경로를 유지한 상태에서, API별 `sleep` 지연 후 고정(mock) 성공 응답을 반환합니다.

## 1) 실행 방법 (uv 기반)

### 1-1) 원클릭 실행 (권장)

아래 스크립트 하나로 `1) uv sync -> 2) Sleep 지연시간 입력 -> 3) 포트 입력 -> 4) 서버 실행` 순서가 자동으로 진행됩니다.

```bash
cd ai-mock
./ai-mock-runner.sh
```

옵션 예시:

```bash
# 기본 포트값/리로드 제어 (포트 입력 단계에서 Enter를 누르면 PORT 기본값 사용)
PORT=9000 RELOAD=0 ./ai-mock-runner.sh
```

### 1-2) 수동 실행

```bash
cd ai-mock
uv sync
uv run uvicorn main:app --host 0.0.0.0 --port 8000 --reload
```

헬스체크:

```bash
curl http://localhost:8000/health
# {"status":"ok"}
```

## 2) API 경로

- `POST /ai/interview/feedback/request` (최종 피드백 요청)
- `POST /ai/interview/follow-up/questions` (질문 생성 요청)
- `POST /ai/stt` (STT 요청)
- `POST /ai/tts` (TTS 요청)
- `GET /health`

## 3) Sleep 환경변수 설명 (한글)

지연 시간은 `core/config.py`에서 환경변수로 읽습니다.

- 값이 없거나 숫자가 아니면 기본값을 사용합니다.
- 음수는 `0`초로 보정됩니다.

요청 유형별 매핑:

- `MOCK_STT_DELAY_SEC` (기본값: `1`)
  - 대상 요청: **STT 요청** `POST /ai/stt`
  - 의미: STT 응답을 반환하기 전에 대기하는 시간(초)

- `MOCK_FEEDBACK_DELAY_SEC` (기본값: `40`)
  - 대상 요청: **최종 피드백 요청** `POST /ai/interview/feedback/request`
  - 의미: 피드백 응답을 반환하기 전에 대기하는 시간(초)

- `MOCK_FOLLOW_UP_DELAY_SEC` (기본값: `15`)
  - 대상 요청: **질문 생성 요청** `POST /ai/interview/follow-up/questions`
  - 의미: 질문 생성 응답을 반환하기 전에 대기하는 시간(초)

- `MOCK_TTS_DELAY_SEC` (기본값: `1.5`)
  - 대상 요청: **TTS 요청** `POST /ai/tts`
  - 의미: TTS multipart 응답을 반환하기 전에 대기하는 시간(초)

예시:

```bash
export MOCK_STT_DELAY_SEC=1
export MOCK_FEEDBACK_DELAY_SEC=10
export MOCK_FOLLOW_UP_DELAY_SEC=4
export MOCK_TTS_DELAY_SEC=2
uv run uvicorn main:app --host 0.0.0.0 --port 8000
```

## 4) 테스트 실행 권장 순서

1. Mock 서버 실행
2. `/health` 확인
3. Spring 백엔드 실행
4. Spring에서 Mock 서버 URL로 API 호출
5. 부하 테스트 도구(k6 등) 실행

## 5) TTS 주의사항

- `POST /ai/tts`는 `multipart/mixed` 형식으로 응답합니다.
- `mock_data/sample.mp3` 파일이 있으면 해당 파일을 반환합니다.
- 파일이 없으면 placeholder audio bytes를 반환하지만 multipart 계약은 유지됩니다.

## 6) 요청/응답 로그

서버 실행 후 CLI 터미널에서 각 요청/응답 로그를 확인할 수 있습니다.

- 요청 로그: 메서드, 경로, 클라이언트 IP
- 응답 로그: 메서드, 경로, 상태코드, 처리시간(ms)

예시:

```text
[요청] method=POST path=/ai/stt client=127.0.0.1
[응답] method=POST path=/ai/stt status=200 elapsed_ms=1002.34
```
