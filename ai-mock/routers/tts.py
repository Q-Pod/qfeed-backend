import asyncio
import json
from pathlib import Path

from fastapi import APIRouter
from fastapi.responses import Response

from core.config import get_settings
from schemas.tts import TTSRequest

router = APIRouter()


def _load_audio_bytes() -> bytes:
    sample_path = Path(__file__).resolve().parents[1] / "mock_data" / "sample.mp3"
    if sample_path.exists():
        return sample_path.read_bytes()

    return b"ID3\x04\x00\x00\x00\x00\x00\x21TIT2\x00\x00\x00\x0f\x00\x00Mock TTS audio"


@router.post("/tts")
async def text_to_speech(request: TTSRequest) -> Response:
    await asyncio.sleep(get_settings().mock_tts_delay_sec)

    boundary = "----QFeedMockBoundary"
    json_data = {
        "message": "get_audio_file_success",
        "data": {
            "user_id": request.user_id,
            "session_id": request.session_id,
        },
    }
    json_content = json.dumps(json_data, ensure_ascii=False)
    audio_bytes = _load_audio_bytes()

    body = (
        f"--{boundary}\r\n"
        "Content-Disposition: form-data; name=\"meta\"\r\n"
        "Content-Type: application/json; charset=utf-8\r\n"
        "\r\n"
        f"{json_content}\r\n"
        f"--{boundary}\r\n"
        "Content-Type: audio/mpeg\r\n"
        "Content-Disposition: attachment; filename=\"tts_output.mp3\"\r\n"
        "Content-Transfer-Encoding: binary\r\n"
        "\r\n"
    ).encode("utf-8") + audio_bytes + f"\r\n--{boundary}--\r\n".encode("utf-8")

    return Response(content=body, media_type=f"multipart/mixed; boundary={boundary}")
