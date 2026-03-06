import asyncio

from fastapi import APIRouter

from core.config import get_settings
from schemas.feedback import (
    FeedbackRequest,
    FeedbackResponse,
    KeywordCheckResult,
    OverallFeedback,
    RubricEvaluationResult,
    TopicFeedback,
)

router = APIRouter()


@router.post("/interview/feedback/request", response_model=FeedbackResponse)
async def request_feedback(request: FeedbackRequest) -> FeedbackResponse:
    await asyncio.sleep(get_settings().mock_feedback_delay_sec)

    rubric_result = RubricEvaluationResult(
        accuracy=4,
        logic=4,
        specificity=4,
        completeness=4,
        delivery=4,
    )
    keyword_result = KeywordCheckResult(
        covered_keywords=["핵심 개념 설명"],
        missing_keywords=["트레이드오프"],
        coverage_ratio=0.5,
    )
    topics_feedback = [
        TopicFeedback(
            topic_id=1,
            main_question="운영체제에서 프로세스와 스레드의 차이를 설명해 주세요.",
            strengths="핵심 개념을 구조적으로 정리해 전달했습니다.",
            improvements="실무 예시와 장애 대응 관점 설명을 보강하면 더 좋습니다.",
        )
    ]
    overall_feedback = OverallFeedback(
        strengths="질문의 의도를 크게 벗어나지 않고 안정적으로 답변했습니다.",
        improvements="근거 수치와 사례를 추가하면 답변 설득력이 높아집니다.",
    )

    return FeedbackResponse.from_evaluation(
        user_id=request.user_id,
        question_id=request.question_id,
        session_id=request.session_id,
        rubric_result=rubric_result,
        keyword_result=keyword_result,
        topics_feedback=topics_feedback,
        overall_feedback=overall_feedback,
    )
