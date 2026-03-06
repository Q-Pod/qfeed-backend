import asyncio

from fastapi import APIRouter

from core.config import get_settings
from schemas.stt import STTData, STTRequest, STTResponse

router = APIRouter()


@router.post("/stt", response_model=STTResponse)
async def speech_to_text(request: STTRequest) -> STTResponse:
    await asyncio.sleep(get_settings().mock_stt_delay_sec)
    return STTResponse(
        message="speech_to_text_success",
        data=STTData(
            user_id=request.user_id,
            session_id=request.session_id,
            text="mock speech to text result",
        ),
    )
