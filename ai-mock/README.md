# AI Mock Server

Spring 백엔드 연동 및 부하 테스트용 FastAPI Mock 서버입니다.
실제 AI 서버의 API 경로와 요청/응답 스키마를 유지한 채, 요청 종류별로 설정된 지연시간 후 고정(mock) 응답을 반환합니다.

## 1) 구성 파일

`ai-mock` 디렉토리 안에서 아래 파일들로 독립 실행됩니다.

- `main.py`: FastAPI 앱 엔트리포인트
- `ai-mock-runner.sh`: 로컬 개발용 실행 스크립트
- `Dockerfile`: 컨테이너 이미지 빌드 정의
- `compose.yaml`: Docker Compose 실행 정의
- `.dockerignore`: Docker build context 제외 규칙

## 2) 로컬 실행

### 2-1) 원클릭 실행

아래 스크립트 하나로 `uv sync -> 지연시간 입력 -> 포트 입력 -> 서버 실행` 순서가 자동으로 진행됩니다.

```bash
cd ai-mock
./ai-mock-runner.sh
```

옵션 예시:

```bash
cd ai-mock
PORT=9000 RELOAD=0 ./ai-mock-runner.sh
```

### 2-2) 수동 실행

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

## 3) Docker 실행

### 3-1) 로컬 또는 서버에서 바로 실행

`ai-mock` 디렉토리로 이동한 뒤 아래 명령으로 컨테이너를 빌드하고 실행합니다.

```bash
cd ai-mock
docker compose up -d --build
```

포트와 지연시간을 바꾸고 싶으면 환경변수를 함께 넘기면 됩니다.

```bash
cd ai-mock
AI_MOCK_PORT=9000 \
MOCK_FEEDBACK_DELAY_SEC=10 \
MOCK_FOLLOW_UP_DELAY_SEC=4 \
MOCK_STT_DELAY_SEC=1 \
MOCK_TTS_DELAY_SEC=2 \
docker compose up -d --build
```

운영 확인 명령:

```bash
cd ai-mock
docker compose ps
docker compose logs -f ai-mock
docker compose restart ai-mock
docker compose down
```

### 3-2) EC2 배포 절차

EC2에 프로젝트 루트 전체를 올려도 되지만, 실제로 컨테이너 빌드에는 `ai-mock` 디렉토리 안의 파일만 사용됩니다.

1. EC2에 소스 복사 또는 Git clone
2. Docker 및 Docker Compose 사용 가능 상태 확인
3. `cd ai-mock`
4. `docker compose up -d --build`
5. `docker compose ps`로 컨테이너 상태 확인
6. `curl http://localhost:8000/health`로 헬스체크

외부에서 직접 접근해야 한다면 EC2 보안 그룹에서 `AI_MOCK_PORT` 값에 맞는 TCP 포트를 열어야 합니다.

## 4) 환경변수

지연 시간은 `core/config.py`에서 환경변수로 읽습니다.

- 값이 없거나 숫자가 아니면 기본값을 사용합니다.
- 음수는 `0`초로 보정됩니다.

| 환경변수 | 기본값 | 대상 API | 설명 |
| --- | --- | --- | --- |
| `AI_MOCK_PORT` | `8000` | 컨테이너 외부 포트 | 호스트에서 노출할 포트 |
| `MOCK_FEEDBACK_DELAY_SEC` | `40` | `POST /ai/interview/feedback/request` | 최종 피드백 응답 전 대기 시간 |
| `MOCK_FOLLOW_UP_DELAY_SEC` | `15` | `POST /ai/interview/follow-up/questions` | 꼬리질문 응답 전 대기 시간 |
| `MOCK_STT_DELAY_SEC` | `1` | `POST /ai/stt` | STT 응답 전 대기 시간 |
| `MOCK_TTS_DELAY_SEC` | `1.5` | `POST /ai/tts` | TTS 응답 전 대기 시간 |

수동 실행 예시:

```bash
cd ai-mock
export MOCK_FEEDBACK_DELAY_SEC=10
export MOCK_FOLLOW_UP_DELAY_SEC=4
export MOCK_STT_DELAY_SEC=1
export MOCK_TTS_DELAY_SEC=2
uv run uvicorn main:app --host 0.0.0.0 --port 8000
```

## 5) API 경로

- `POST /ai/interview/feedback/request`
- `POST /ai/interview/follow-up/questions`
- `POST /ai/stt`
- `POST /ai/tts`
- `GET /health`

## 6) 테스트 권장 순서

1. Mock 서버 실행
2. `/health` 확인
3. Spring 백엔드 실행
4. Spring에서 Mock 서버 URL로 API 호출
5. k6 등 부하 테스트 도구 실행

## 7) TTS 응답 주의사항

- `POST /ai/tts`는 `multipart/mixed` 형식으로 응답합니다.
- `mock_data/sample.mp3` 파일이 있으면 해당 파일을 반환합니다.
- 파일이 없으면 placeholder audio bytes를 반환하지만 multipart 계약은 유지합니다.

## 8) 로그

서버 실행 후 터미널에서 각 요청/응답 로그를 확인할 수 있습니다.

- 요청 로그: 메서드, 경로, 클라이언트 IP
- 응답 로그: 메서드, 경로, 상태코드, 처리시간(ms)

예시:

```text
[요청] method=POST path=/ai/stt client=127.0.0.1
[응답] method=POST path=/ai/stt status=200 elapsed_ms=1002.34
```
