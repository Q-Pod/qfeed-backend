package com.ktb.question.domain;

import com.ktb.common.domain.BaseActivatableEntity;
import com.ktb.common.domain.ErrorCode;
import com.ktb.question.exception.QuestionAlreadyDeletedException;
import com.ktb.question.exception.QuestionInvalidContentException;
import com.ktb.question.exception.QuestionRequiredCategoryException;
import com.ktb.question.exception.QuestionRequiredTypeException;
import com.ktb.question.exception.QuestionTypeCategoryMismatchException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
    name = "QUESTION",
    indexes = {
        @Index(name = "idx_type", columnList = "question_type_cd"),
        @Index(name = "idx_ctg", columnList = "question_ctg"),
        @Index(name = "idx_use", columnList = "use_yn, deleted_at"),
        @Index(name = "idx_type_ctg_use", columnList = "question_type_cd, question_ctg, use_yn"),
        @Index(name = "idx_created", columnList = "created_at")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Question extends BaseActivatableEntity {
    private static final int MIN_CONTENT_LENGTH = 2;
    private static final int MAX_CONTENT_LENGTH = 200;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "question_id")
    private Long id;

    @Column(name = "question_content", nullable = false, length = 200)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_type_cd", nullable = false, length = 50)
    private QuestionType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "question_ctg", nullable = false, length = 50)
    private QuestionCategory category;

    @Builder
    private Question(String content, QuestionType type, QuestionCategory category) {
        validateContent(content);
        validateType(type);
        validateCategory(category);
        validateTypeAndCategory(type, category);
        this.content = content;
        this.type = type;
        this.category = category;
    }

    private Question(Long id) {
        this.id = id;
    }

    public static Question create(
            String content,
            QuestionType type,
            QuestionCategory category
    ) {
        return Question.builder()
                .content(content)
                .type(type)
                .category(category)
                .build();
    }

    public static Question createWithQuestionId(Long questionId) {
        return new Question(questionId);
    }

    public void updateType(QuestionType type) {
        validateNotDeleted();
        validateType(type);
        validateTypeAndCategory(type, this.category);
        this.type = type;
    }

    public void updateCategory(QuestionCategory category) {
        validateNotDeleted();
        validateCategory(category);
        validateTypeAndCategory(this.type, category);
        this.category = category;
    }

    public void updateContent(String content) {
        validateNotDeleted();
        validateContent(content);
        this.content = content;
    }

    public void delete() {
        if (getDeletedAt() != null) {
            throw new QuestionAlreadyDeletedException(id);
        }
        disable();
        softDelete();
    }

    public void activate() {
        validateNotDeleted();
        enable();
    }

    private void validateContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new QuestionInvalidContentException(ErrorCode.QUESTION_CONTENT_REQUIRED);
        }
        if (!content.equals(content.trim())) {
            throw new QuestionInvalidContentException(ErrorCode.QUESTION_CONTENT_HAS_SPACES);
        }
        if (content.length() < MIN_CONTENT_LENGTH) {
            throw new QuestionInvalidContentException(ErrorCode.QUESTION_CONTENT_TOO_SHORT);
        }
        if (content.length() > MAX_CONTENT_LENGTH) {
            throw new QuestionInvalidContentException(ErrorCode.QUESTION_CONTENT_TOO_LONG);
        }
    }

    private void validateType(QuestionType type) {
        if (type == null) {
            throw new QuestionRequiredTypeException();
        }
    }

    private void validateCategory(QuestionCategory category) {
        if (category == null) {
            throw new QuestionRequiredCategoryException();
        }
    }

    private void validateTypeAndCategory(QuestionType type, QuestionCategory category) {
        if (!category.supports(type)) {
            throw new QuestionTypeCategoryMismatchException();
        }
    }

    private void validateNotDeleted() {
        if (getDeletedAt() != null) {
            throw new QuestionAlreadyDeletedException(id);
        }
    }
}
