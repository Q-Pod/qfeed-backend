#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

if ! command -v uv >/dev/null 2>&1; then
  echo "[오류] uv가 설치되어 있지 않습니다. 먼저 uv를 설치해주세요: https://docs.astral.sh/uv/" >&2
  exit 1
fi

HOST="${HOST:-0.0.0.0}"
PORT="${PORT:-8000}"
RELOAD="${RELOAD:-1}"
export UV_CACHE_DIR="${UV_CACHE_DIR:-$SCRIPT_DIR/.uv-cache}"
mkdir -p "$UV_CACHE_DIR"

DEFAULT_FEEDBACK_DELAY="${MOCK_FEEDBACK_DELAY_SEC:-40}"
DEFAULT_FOLLOW_UP_DELAY="${MOCK_FOLLOW_UP_DELAY_SEC:-15}"
DEFAULT_STT_DELAY="${MOCK_STT_DELAY_SEC:-1}"
DEFAULT_TTS_DELAY="${MOCK_TTS_DELAY_SEC:-1.5}"

read_delay() {
  local var_name="$1"
  local label="$2"
  local default_value="$3"
  local input_value=""

  if [[ -t 0 ]]; then
    read -r -p "${label} 지연 시간(초) [기본: ${default_value}]: " input_value
  fi

  if [[ -z "$input_value" ]]; then
    export "${var_name}=${default_value}"
    return
  fi

  if [[ "$input_value" =~ ^[0-9]+([.][0-9]+)?$ ]]; then
    export "${var_name}=${input_value}"
  else
    echo "[경고] ${label} 입력값이 숫자가 아니어서 기본값(${default_value})을 사용합니다."
    export "${var_name}=${default_value}"
  fi
}

read_port() {
  local default_port="$1"
  local input_port=""

  if [[ -t 0 ]]; then
    read -r -p "서버 포트 번호 [기본: ${default_port}]: " input_port
  fi

  if [[ -z "$input_port" ]]; then
    PORT="$default_port"
    return
  fi

  if [[ "$input_port" =~ ^[0-9]+$ ]] && (( input_port >= 1 && input_port <= 65535 )); then
    PORT="$input_port"
  else
    echo "[경고] 포트 번호는 1~65535의 정수여야 합니다. 기본값(${default_port})을 사용합니다."
    PORT="$default_port"
  fi
}

echo "[안내] 1/4 의존성 동기화(uv sync)를 시작합니다."
uv sync

echo "[안내] 2/4 지연 시간 입력을 진행합니다."
read_delay "MOCK_FEEDBACK_DELAY_SEC" "피드백 API" "$DEFAULT_FEEDBACK_DELAY"
read_delay "MOCK_FOLLOW_UP_DELAY_SEC" "꼬리질문 API" "$DEFAULT_FOLLOW_UP_DELAY"
read_delay "MOCK_STT_DELAY_SEC" "STT API" "$DEFAULT_STT_DELAY"
read_delay "MOCK_TTS_DELAY_SEC" "TTS API" "$DEFAULT_TTS_DELAY"

echo "[안내] 적용된 지연 시간: 피드백=${MOCK_FEEDBACK_DELAY_SEC}s, 꼬리질문=${MOCK_FOLLOW_UP_DELAY_SEC}s, STT=${MOCK_STT_DELAY_SEC}s, TTS=${MOCK_TTS_DELAY_SEC}s"

echo "[안내] 3/4 포트 번호 입력을 진행합니다."
read_port "$PORT"

echo "[안내] 4/4 서버를 실행합니다. (HOST=${HOST}, PORT=${PORT})"
if [[ "$RELOAD" == "1" ]]; then
  echo "[안내] 실행 명령: uv run uvicorn main:app --host $HOST --port $PORT --reload"
  exec uv run uvicorn main:app --host "$HOST" --port "$PORT" --reload
else
  echo "[안내] 실행 명령: uv run uvicorn main:app --host $HOST --port $PORT"
  exec uv run uvicorn main:app --host "$HOST" --port "$PORT"
fi
