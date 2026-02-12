package com.ktb.question.domain;

import com.ktb.fixture.QuestionFixture;
import com.ktb.question.exception.QuestionAlreadyDeletedException;
import com.ktb.question.exception.QuestionInvalidContentException;
import com.ktb.question.exception.QuestionRequiredCategoryException;
import com.ktb.question.exception.QuestionRequiredTypeException;
import com.ktb.question.exception.QuestionTypeCategoryMismatchException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question 도메인 테스트")
class QuestionTest {

    @Nested
    @DisplayName("Question 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 값으로 Question 생성 성공")
        void create_WithValidData_ShouldSucceed() {
            // Given
            String content = "프로세스와 스레드의 차이를 설명해주세요.";
            QuestionType type = QuestionType.CS;
            QuestionCategory category = QuestionCategory.OS;

            // When
            Question question = Question.create(content, type, category);

            // Then
            assertThat(question).isNotNull();
            assertThat(question.getContent()).isEqualTo(content);
            assertThat(question.getType()).isEqualTo(type);
            assertThat(question.getCategory()).isEqualTo(category);
        }

        @Test
        @DisplayName("질문 유형이 null이면 QuestionRequiredTypeException 발생")
        void create_WithNullType_ShouldThrowException() {
            // Given
            String content = QuestionFixture.createContentWithMinLength();

            // When & Then
            assertThatThrownBy(() -> Question.create(content, null, QuestionCategory.OS))
                    .isInstanceOf(QuestionRequiredTypeException.class);
        }

        @Test
        @DisplayName("질문 카테고리가 null이면 QuestionRequiredCategoryException 발생")
        void create_WithNullCategory_ShouldThrowException() {
            // Given
            String content = QuestionFixture.createContentWithMinLength();

            // When & Then
            assertThatThrownBy(() -> Question.create(content, QuestionType.CS, null))
                    .isInstanceOf(QuestionRequiredCategoryException.class);
        }

        @Test
        @DisplayName("null 내용으로 생성 시 QuestionInvalidContentException 발생")
        void create_WithNullContent_ShouldThrowException() {
            // Given
            String nullContent = QuestionFixture.createNullContent();

            // When & Then
            assertThatThrownBy(() -> Question.create(nullContent, QuestionType.CS, QuestionCategory.OS))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("빈 문자열 내용으로 생성 시 QuestionInvalidContentException 발생")
        void create_WithEmptyContent_ShouldThrowException() {
            // Given
            String emptyContent = QuestionFixture.createEmptyContent();

            // When & Then
            assertThatThrownBy(() -> Question.create(emptyContent, QuestionType.CS, QuestionCategory.OS))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("공백만 있는 내용으로 생성 시 QuestionInvalidContentException 발생")
        void create_WithOnlySpaces_ShouldThrowException() {
            // Given
            String blankContent = QuestionFixture.createBlankContent();

            // When & Then
            assertThatThrownBy(() -> Question.create(blankContent, QuestionType.CS, QuestionCategory.OS))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("200자 내용으로 생성 성공")
        void create_WithMaxLength_ShouldSucceed() {
            // Given
            String content = QuestionFixture.createContentWithMaxLength();

            // When
            Question question = Question.create(content, QuestionType.CS, QuestionCategory.OS);

            // Then
            assertThat(question.getContent()).hasSize(200);
        }

        @Test
        @DisplayName("2자 내용으로 생성 성공")
        void create_WithMinLength_ShouldSucceed() {
            // Given
            String content = QuestionFixture.createContentWithMinLength();

            // When
            Question question = Question.create(content, QuestionType.CS, QuestionCategory.OS);

            // Then
            assertThat(question.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("1자 내용으로 생성 시 QuestionInvalidContentException 발생")
        void create_WithBelowMinLength_ShouldThrowException() {
            // Given
            String content = "A";

            // When & Then
            assertThatThrownBy(() -> Question.create(content, QuestionType.CS, QuestionCategory.OS))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("201자 내용으로 생성 시 QuestionInvalidContentException 발생")
        void create_WithExceedingMaxLength_ShouldThrowException() {
            // Given
            String content = QuestionFixture.createContentExceedingMaxLength();

            // When & Then
            assertThatThrownBy(() -> Question.create(content, QuestionType.CS, QuestionCategory.OS))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("앞 공백이 포함된 내용으로 생성 시 QuestionInvalidContentException 발생")
        void create_WithLeadingSpaces_ShouldThrowException() {
            // Given
            String content = "  질문 내용";

            // When & Then
            assertThatThrownBy(() -> Question.create(content, QuestionType.CS, QuestionCategory.OS))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("뒤 공백이 포함된 내용으로 생성 시 QuestionInvalidContentException 발생")
        void create_WithTrailingSpaces_ShouldThrowException() {
            // Given
            String content = "질문 내용  ";

            // When & Then
            assertThatThrownBy(() -> Question.create(content, QuestionType.CS, QuestionCategory.OS))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("질문 유형-카테고리 조합이 다르면 QuestionTypeCategoryMismatchException 발생")
        void create_WithMismatchedTypeCategory_ShouldThrowException() {
            // Given
            String content = QuestionFixture.createContentWithMinLength();

            // When & Then
            assertThatThrownBy(() -> Question.create(
                    content,
                    QuestionType.CS,
                    QuestionCategory.MEDIA
            )).isInstanceOf(QuestionTypeCategoryMismatchException.class);
        }

        @Test
        @DisplayName("PORTFOLIO 유형은 PORTFOLIO 카테고리로 생성 성공")
        void create_PortfolioTypeWithPortfolioCategory_ShouldSucceed() {
            // Given
            String content = QuestionFixture.createContentWithMinLength();

            // When
            Question question = Question.create(content, QuestionType.PORTFOLIO, QuestionCategory.PORTFOLIO);

            // Then
            assertThat(question.getType()).isEqualTo(QuestionType.PORTFOLIO);
            assertThat(question.getCategory()).isEqualTo(QuestionCategory.PORTFOLIO);
        }
    }

    @Nested
    @DisplayName("Question 업데이트 테스트")
    class UpdateTest {

        @Test
        @DisplayName("내용 업데이트 성공")
        void updateContent_WithValidContent_ShouldSucceed() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String newContent = "새로운 질문 내용입니다.";

            // When
            question.updateContent(newContent);

            // Then
            assertThat(question.getContent()).isEqualTo(newContent);
        }

        @Test
        @DisplayName("200자 내용으로 업데이트 성공")
        void updateContent_WithMaxLength_ShouldSucceed() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String content = QuestionFixture.createContentWithMaxLength();

            // When
            question.updateContent(content);

            // Then
            assertThat(question.getContent()).hasSize(200);
        }

        @Test
        @DisplayName("2자 내용으로 업데이트 성공")
        void updateContent_WithMinLength_ShouldSucceed() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String content = QuestionFixture.createContentWithMinLength();

            // When
            question.updateContent(content);

            // Then
            assertThat(question.getContent()).hasSize(2);
        }

        @Test
        @DisplayName("1자 내용으로 업데이트 시 QuestionInvalidContentException 발생")
        void updateContent_WithBelowMinLength_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String content = "A";

            // When & Then
            assertThatThrownBy(() -> question.updateContent(content))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("201자 내용으로 업데이트 시 QuestionInvalidContentException 발생")
        void updateContent_WithExceedingMaxLength_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String content = QuestionFixture.createContentExceedingMaxLength();

            // When & Then
            assertThatThrownBy(() -> question.updateContent(content))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("null 내용으로 업데이트 시 QuestionInvalidContentException 발생")
        void updateContent_WithNullContent_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String content = QuestionFixture.createNullContent();

            // When & Then
            assertThatThrownBy(() -> question.updateContent(content))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("공백 내용으로 업데이트 시 QuestionInvalidContentException 발생")
        void updateContent_WithBlankContent_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String content = QuestionFixture.createBlankContent();

            // When & Then
            assertThatThrownBy(() -> question.updateContent(content))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("앞 공백이 포함된 내용으로 업데이트 시 QuestionInvalidContentException 발생")
        void updateContent_WithLeadingSpaces_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String content = "  질문 내용";

            // When & Then
            assertThatThrownBy(() -> question.updateContent(content))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("뒤 공백이 포함된 내용으로 업데이트 시 QuestionInvalidContentException 발생")
        void updateContent_WithTrailingSpaces_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion();
            String content = "질문 내용  ";

            // When & Then
            assertThatThrownBy(() -> question.updateContent(content))
                    .isInstanceOf(QuestionInvalidContentException.class);
        }

        @Test
        @DisplayName("동일한 질문 유형으로 업데이트 성공")
        void updateType_WithValidType_ShouldSucceed() {
            // Given
            Question question = QuestionFixture.createQuestion();

            // When
            question.updateType(QuestionType.CS);

            // Then
            assertThat(question.getType()).isEqualTo(QuestionType.CS);
        }

        @Test
        @DisplayName("질문 유형이 null이면 QuestionRequiredTypeException 발생")
        void updateType_WithNull_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion();

            // When & Then
            assertThatThrownBy(() -> question.updateType(null))
                    .isInstanceOf(QuestionRequiredTypeException.class);
        }

        @Test
        @DisplayName("soft delete 처리된 질문은 유형 업데이트 불가")
        void updateType_WhenSoftDeleted_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createSoftDeletedQuestion();

            // When & Then
            assertThatThrownBy(() -> question.updateType(QuestionType.SYSTEM_DESIGN))
                    .isInstanceOf(QuestionAlreadyDeletedException.class);
        }

        @Test
        @DisplayName("질문 유형 변경 시 현재 카테고리와 맞지 않으면 예외 발생")
        void updateType_WithMismatchedCategory_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion(); // CS + OS

            // When & Then
            assertThatThrownBy(() -> question.updateType(QuestionType.SYSTEM_DESIGN))
                    .isInstanceOf(QuestionTypeCategoryMismatchException.class);
        }

        @Test
        @DisplayName("PORTFOLIO 유형으로 변경 시 카테고리 미허용으로 예외 발생")
        void updateType_ToPortfolio_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion(); // CS + OS

            // When & Then
            assertThatThrownBy(() -> question.updateType(QuestionType.PORTFOLIO))
                    .isInstanceOf(QuestionTypeCategoryMismatchException.class);
        }

        @Test
        @DisplayName("질문 카테고리 업데이트 성공")
        void updateCategory_WithValidCategory_ShouldSucceed() {
            // Given
            Question question = QuestionFixture.createQuestion();

            // When
            question.updateCategory(QuestionCategory.NETWORK);

            // Then
            assertThat(question.getCategory()).isEqualTo(QuestionCategory.NETWORK);
        }

        @Test
        @DisplayName("질문 카테고리가 null이면 QuestionRequiredCategoryException 발생")
        void updateCategory_WithNull_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion();

            // When & Then
            assertThatThrownBy(() -> question.updateCategory(null))
                    .isInstanceOf(QuestionRequiredCategoryException.class);
        }

        @Test
        @DisplayName("soft delete 처리된 질문은 카테고리 업데이트 불가")
        void updateCategory_WhenSoftDeleted_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createSoftDeletedQuestion();

            // When & Then
            assertThatThrownBy(() -> question.updateCategory(QuestionCategory.NETWORK))
                    .isInstanceOf(QuestionAlreadyDeletedException.class);
        }

        @Test
        @DisplayName("질문 카테고리 변경 시 현재 유형과 맞지 않으면 예외 발생")
        void updateCategory_WithMismatchedType_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createQuestion(); // CS + OS

            // When & Then
            assertThatThrownBy(() -> question.updateCategory(QuestionCategory.MEDIA))
                    .isInstanceOf(QuestionTypeCategoryMismatchException.class);
        }
    }

    @Nested
    @DisplayName("Question 삭제/활성화 테스트")
    class StateTest {

        @Test
        @DisplayName("활성 상태에서 삭제 시 soft delete 처리")
        void delete_WhenEnabled_ShouldSoftDelete() {
            // Given
            Question question = QuestionFixture.createQuestion();

            // When
            question.delete();

            // Then
            assertThat(question.isUseYn()).isFalse();
            assertThat(question.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("soft delete 처리된 질문을 다시 삭제하면 QuestionAlreadyDeletedException 발생")
        void delete_WhenAlreadySoftDeleted_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createSoftDeletedQuestion();

            // When & Then
            assertThatThrownBy(question::delete)
                    .isInstanceOf(QuestionAlreadyDeletedException.class);
        }

        @Test
        @DisplayName("비활성 상태에서 activate 호출 시 활성화")
        void activate_WhenDisabled_ShouldEnable() {
            // Given
            Question question = QuestionFixture.createDisabledQuestion();

            // When
            question.activate();

            // Then
            assertThat(question.isUseYn()).isTrue();
        }

        @Test
        @DisplayName("soft delete 처리된 질문은 activate 호출 시 예외 발생")
        void activate_WhenSoftDeleted_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createSoftDeletedQuestion();

            // When & Then
            assertThatThrownBy(() -> question.activate())
                    .isInstanceOf(QuestionAlreadyDeletedException.class);
        }

        @Test
        @DisplayName("soft delete 처리된 질문은 내용 업데이트 불가")
        void updateContent_WhenSoftDeleted_ShouldThrowException() {
            // Given
            Question question = QuestionFixture.createSoftDeletedQuestion();
            String content = "새로운 질문 내용입니다.";

            // When & Then
            assertThatThrownBy(() -> question.updateContent(content))
                    .isInstanceOf(QuestionAlreadyDeletedException.class);
        }
    }
}
