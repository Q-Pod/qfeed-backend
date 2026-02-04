package com.ktb.answer.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AnswerStatus {

    SUBMITTED("제출됨"),

    TRANSCRIBING("음성 변환 중"),

    IMMEDIATE_FEEDBACK_READY("즉각 피드백 준비됨"),

    AI_FEEDBACK_PROCESSING("AI 피드백 처리 중"),
    NOT_AVAILABLE("답변 품질 낮음"),

    COMPLETED("완료"),

    FAILED_RETRYABLE("실패(재시도 가능)"),

    FAILED("실패");

    private final String description;

    public boolean canTransitionTo(AnswerStatus nextStatus) {
        return switch (this) {
            case SUBMITTED -> nextStatus == TRANSCRIBING ||
                              nextStatus == IMMEDIATE_FEEDBACK_READY ||
                              nextStatus == AI_FEEDBACK_PROCESSING ||
                              nextStatus == NOT_AVAILABLE ||
                              nextStatus == FAILED;

            case TRANSCRIBING -> nextStatus == IMMEDIATE_FEEDBACK_READY ||
                                 nextStatus == NOT_AVAILABLE ||
                                 nextStatus == FAILED;

            case IMMEDIATE_FEEDBACK_READY, FAILED_RETRYABLE -> nextStatus == AI_FEEDBACK_PROCESSING ||
                                             nextStatus == FAILED;

            case AI_FEEDBACK_PROCESSING -> nextStatus == COMPLETED ||
                                           nextStatus == FAILED_RETRYABLE ||
                                           nextStatus == FAILED;

            case COMPLETED, FAILED, NOT_AVAILABLE -> false; // 종료 상태에서는 전이 불가
        };
    }
}
