import asyncio

from fastapi import APIRouter

from core.config import get_settings
from schemas.feedback import CSCategory
from schemas.question import GeneratedQuestion, QuestionGenerateRequest, QuestionGenerateResponse

router = APIRouter()


@router.post("/interview/follow-up/questions", response_model=QuestionGenerateResponse)
async def request_follow_up_question(request: QuestionGenerateRequest) -> QuestionGenerateResponse:
    await asyncio.sleep(get_settings().mock_follow_up_delay_sec)

    generated = GeneratedQuestion(
        user_id=request.user_id,
        session_id=request.session_id,
        question_text="방금 설명한 방식의 장단점을 실제 트래픽 상황 기준으로 설명해 주세요.",
        category=request.initial_category or CSCategory.OS,
        topic_id=1,
        turn_type="follow_up",
        is_session_ended=False,
        end_reason=None,
        is_bad_case=False,
        bad_case_feedback=None,
    )
    return QuestionGenerateResponse(message="question_generated", data=generated)
