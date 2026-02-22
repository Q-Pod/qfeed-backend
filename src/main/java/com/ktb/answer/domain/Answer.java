package com.ktb.answer.domain;

import com.ktb.answer.exception.AnswerRequiredTypeException;
import com.ktb.answer.exception.InvalidAnswerStatusTransitionException;
import com.ktb.auth.domain.UserAccount;
import com.ktb.common.domain.BaseSoftDeleteEntity;
import com.ktb.metric.domain.AnswerMetric;
import com.ktb.question.domain.Question;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "ANSWER",
        indexes = {
                @Index(name = "idx_question_id", columnList = "question_id"),
                @Index(name = "idx_account_id", columnList = "account_id"),
                @Index(name = "idx_created_at", columnList = "created_at"),
                @Index(name = "idx_deleted_at", columnList = "deleted_at"),
                @Index(name = "idx_account_created", columnList = "account_id, created_at, deleted_at"),
                @Index(name = "idx_status", columnList = "answer_status_cd")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = {"question", "account"})
public class Answer extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "answer_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private UserAccount account;

    @Column(name = "answer_content", columnDefinition = "TEXT")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_status_cd", nullable = false, length = 30)
    private AnswerStatus status = AnswerStatus.SUBMITTED;

    @Enumerated(EnumType.STRING)
    @Column(name = "answer_type", nullable = false, length = 30)
    private AnswerType type;

    @Column(name = "answer_session_id", length = 64)
    private String sessionId;

    @Column(name = "answer_ai_feedback", columnDefinition = "TEXT")
    private String aiFeedback;

    @Column(name = "answer_strengths_feedback", columnDefinition = "TEXT")
    private String strengthsFeedback;

    @Column(name = "answer_improvements_feedback", columnDefinition = "TEXT")
    private String improvementsFeedback;

    @OneToMany(mappedBy = "id.answer", cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    private final List<AnswerMetric> answerMetrics = new ArrayList<>();

    @Builder
    private Answer(Question question, UserAccount account, String content, AnswerType type) {
        validateType(type);
        this.question = question;
        this.account = account;
        this.content = content;
        this.type = type;
        this.status = AnswerStatus.SUBMITTED;
    }

    public static Answer create(Question question, UserAccount account, String content, AnswerType type) {
        return Answer.builder()
                .question(question)
                .account(account)
                .content(content)
                .type(type)
                .build();
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void transitionTo(AnswerStatus nextStatus) {
        if (!this.status.canTransitionTo(nextStatus)) {
            throw new InvalidAnswerStatusTransitionException(this.status, nextStatus);
        }
        this.status = nextStatus;
    }

    public void assignSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public void setAiFeedback(String feedback) {
        this.aiFeedback = feedback;
        this.status = AnswerStatus.COMPLETED;
    }

    public void setOverallFeedback(String strengths, String improvements) {
        this.strengthsFeedback = strengths;
        this.improvementsFeedback = improvements;
        this.aiFeedback = (strengths == null ? "" : strengths) + "\n\n" + (improvements == null ? "" : improvements);
        this.status = AnswerStatus.COMPLETED;
    }

    public List<AnswerMetric> getAnswerMetrics() {
        return Collections.unmodifiableList(answerMetrics);
    }

    private void validateType(AnswerType type) {
        if (type == null) {
            throw new AnswerRequiredTypeException();
        }
    }

    public boolean isOwnedBy(Long accountId) {
        return this.account != null && this.account.getId().equals(accountId);
    }
}
