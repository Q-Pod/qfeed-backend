package com.ktb.interview.session.domain;

import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.domain.TurnType;
import com.ktb.question.domain.QuestionType;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.Getter;

/**
 * 인터뷰 세션 애그리거트.
 * turn 진행, 상태 전이, 만료 시간을 관리한다.
 */
@Getter
public class InterviewSession {

    private final String sessionId;
    private final Long accountId;
    private final AnswerType interviewType;
    private final QuestionType questionType;

    private InterviewSessionStatus status;
    private int turnCount;
    private int retryCount;

    private Integer currentTopicId;
    private TurnType nextTurnType;
    private InterviewQuestionSnapshot currentQuestion;

    private final List<InterviewHistoryItem> interviewHistory;

    private String errorCode;
    private String errorMessage;

    private LocalDateTime nextRetryAt;
    private LocalDateTime failedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime updatedAt;

    /**
     * 인터뷰 세션을 초기화합니다.
     */
    public InterviewSession(
            String sessionId,
            Long accountId,
            AnswerType interviewType,
            QuestionType questionType,
            InterviewQuestionSnapshot currentQuestion,
            TurnType nextTurnType,
            Integer currentTopicId,
            Duration ttl
    ) {
        this.sessionId = sessionId;
        this.accountId = accountId;
        this.interviewType = interviewType;
        this.questionType = questionType;
        this.currentQuestion = currentQuestion;
        this.nextTurnType = nextTurnType;
        this.currentTopicId = currentTopicId;

        this.status = InterviewSessionStatus.IN_PROGRESS;
        this.turnCount = 0;
        this.retryCount = 0;
        this.interviewHistory = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();
        this.updatedAt = now;
        this.expiresAt = now.plus(ttl);
    }

    /**
     * 연습 모드 세션을 생성합니다.
     */
    public static InterviewSession createPractice(
            String sessionId,
            Long accountId,
            QuestionType questionType,
            Duration ttl
    ) {
        return new InterviewSession(
                sessionId,
                accountId,
                AnswerType.PRACTICE_INTERVIEW,
                questionType,
                null,
                TurnType.NEW_TOPIC,
                null,
                ttl
        );
    }

    /**
     * 실전 모드 세션을 생성합니다.
     */
    public static InterviewSession createReal(
            String sessionId,
            Long accountId,
            QuestionType questionType,
            Duration ttl
    ) {
        return new InterviewSession(
                sessionId,
                accountId,
                AnswerType.REAL_INTERVIEW,
                questionType,
                null,
                TurnType.MAIN,
                1,
                ttl
        );
    }

    /**
     * 현재 시각 기준 세션 만료 여부를 확인합니다.
     */
    public synchronized boolean isExpired(LocalDateTime now) {
        return expiresAt != null && now.isAfter(expiresAt);
    }

    /**
     * 마지막 갱신 시각과 만료 시각을 현재 기준으로 연장합니다.
     */
    public synchronized void touch(Duration ttl) {
        this.updatedAt = LocalDateTime.now();
        this.expiresAt = this.updatedAt.plus(ttl);
    }

    /**
     * turn 이력 1건을 추가하고 turn 카운트를 증가시킵니다.
     */
    public synchronized void appendHistory(InterviewHistoryItem item) {
        this.interviewHistory.add(item);
        this.turnCount += 1;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 세션에 누적된 인터뷰 이력을 모두 비웁니다.
     */
    public synchronized void clearHistory() {
        this.interviewHistory.clear();
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * AI 재시도 대기 상태로 전이합니다.
     */
    public synchronized void markRetrying(int retryCount, LocalDateTime nextRetryAt) {
        this.status = InterviewSessionStatus.RETRYING;
        this.retryCount = retryCount;
        this.nextRetryAt = nextRetryAt;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 세션 상태를 진행 중으로 복원합니다.
     */
    public synchronized void markInProgress() {
        this.status = InterviewSessionStatus.IN_PROGRESS;
        this.nextRetryAt = null;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 세션을 완료 상태로 전이하고 다음 turn 을 종료로 고정합니다.
     */
    public synchronized void markCompleted() {
        this.status = InterviewSessionStatus.COMPLETED;
        this.nextTurnType = TurnType.SESSION_END;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 세션을 실패 상태로 전이하고 에러 정보를 기록합니다.
     */
    public synchronized void markFailed(String errorCode, String errorMessage, int retryCount) {
        this.status = InterviewSessionStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.retryCount = retryCount;
        this.failedAt = LocalDateTime.now();
        this.nextRetryAt = null;
        this.updatedAt = this.failedAt;
    }

    /**
     * 세션을 만료 상태로 전이합니다.
     */
    public synchronized void markExpired() {
        this.status = InterviewSessionStatus.EXPIRED;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 다음 질문 컨텍스트(질문/turn/topic)를 갱신합니다.
     */
    public synchronized void updateNextQuestion(
            InterviewQuestionSnapshot nextQuestion,
            TurnType nextTurnType,
            Integer nextTopicId
    ) {
        this.currentQuestion = nextQuestion;
        this.nextTurnType = nextTurnType;
        this.currentTopicId = nextTopicId;
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 외부 변경을 막기 위해 읽기 전용 이력 뷰를 반환합니다.
     */
    public synchronized List<InterviewHistoryItem> getInterviewHistoryView() {
        return Collections.unmodifiableList(new ArrayList<>(interviewHistory));
    }
}
