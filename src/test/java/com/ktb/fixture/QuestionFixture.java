package com.ktb.fixture;

import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;

public class QuestionFixture {

    private static final String DEFAULT_CONTENT = "프로세스와 스레드의 차이를 설명해주세요.";
    private static final QuestionType DEFAULT_TYPE = QuestionType.CS;
    private static final QuestionCategory DEFAULT_CATEGORY = QuestionCategory.OS;
    private static final int MAX_CONTENT_LENGTH = 200;
    private static final int MIN_CONTENT_LENGTH = 2;

    public static Question createQuestion() {
        return Question.create(DEFAULT_CONTENT, DEFAULT_TYPE, DEFAULT_CATEGORY);
    }

    public static Question createQuestion(String content) {
        return Question.create(content, DEFAULT_TYPE, DEFAULT_CATEGORY);
    }

    public static Question createQuestion(QuestionType type) {
        QuestionCategory category = switch (type) {
            case SYSTEM_DESIGN -> QuestionCategory.MEDIA;
            case CS -> DEFAULT_CATEGORY;
            case PORTFOLIO -> QuestionCategory.PORTFOLIO;
        };
        return Question.create(DEFAULT_CONTENT, type, category);
    }

    public static Question createQuestion(QuestionCategory category) {
        return Question.create(DEFAULT_CONTENT, DEFAULT_TYPE, category);
    }

    public static String createContentWithMaxLength() {
        return "A".repeat(MAX_CONTENT_LENGTH);
    }

    public static String createContentWithMinLength() {
        return "A".repeat(MIN_CONTENT_LENGTH);
    }

    public static String createContentExceedingMaxLength() {
        return "A".repeat(MAX_CONTENT_LENGTH + 1);
    }

    public static String createNullContent() {
        return null;
    }

    public static String createEmptyContent() {
        return "";
    }

    public static String createBlankContent() {
        return "   ";
    }

    public static Question createDisabledQuestion() {
        Question question = createQuestion();
        question.disable();
        return question;
    }

    public static Question createSoftDeletedQuestion() {
        Question question = createQuestion();
        question.delete();
        return question;
    }
}
