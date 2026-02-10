package com.ktb.answer.service;

import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.exception.AnswerAccessDeniedException;
import com.ktb.answer.exception.AnswerInvalidContentException;
import com.ktb.answer.exception.InvalidAnswerStatusTransitionException;
import com.ktb.answer.service.impl.AnswerDomainServiceImpl;
import com.ktb.auth.domain.UserAccount;
import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.fixture.AnswerFixture;
import com.ktb.question.domain.Question;
import com.ktb.question.exception.QuestionNotFoundException;
import com.ktb.question.repository.QuestionRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerDomainService 단위 테스트")
class AnswerDomainServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private AnswerDomainServiceImpl answerDomainService;

    private static final Long ACCOUNT_ID = 1L;
    private static final Long QUESTION_ID = 100L;
    private static final String VALID_CONTENT = "유효한 답변 내용입니다.";
    private static final int MAX_CONTENT_LENGTH = 1500;

    @Nested
    @DisplayName("createAnswer() 테스트")
    class CreateAnswerTest {

        @Test
        @DisplayName("정상적인 파라미터로 Answer 생성 성공")
        void createAnswer_WithValidParams_ShouldSucceed() {
            // Given
            UserAccount mockAccount = mock(UserAccount.class);
            Question mockQuestion = mock(Question.class);

            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(mockQuestion));

            // When
            Answer result = answerDomainService.createAnswer(
                    ACCOUNT_ID, QUESTION_ID, VALID_CONTENT, AnswerType.PRACTICE_INTERVIEW
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getContent()).isEqualTo(VALID_CONTENT);
            assertThat(result.getType()).isEqualTo(AnswerType.PRACTICE_INTERVIEW);
            assertThat(result.getStatus()).isEqualTo(AnswerStatus.SUBMITTED);
        }

        @Test
        @DisplayName("존재하지 않는 계정 ID로 생성 시 AccountNotFoundException 발생")
        void createAnswer_WithNonExistentAccount_ShouldThrowException() {
            // Given
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.createAnswer(
                            ACCOUNT_ID, QUESTION_ID, VALID_CONTENT, AnswerType.PRACTICE_INTERVIEW))
                    .isInstanceOf(AccountNotFoundException.class);
        }

        @Test
        @DisplayName("존재하지 않는 질문 ID로 생성 시 QuestionNotFoundException 발생")
        void createAnswer_WithNonExistentQuestion_ShouldThrowException() {
            // Given
            UserAccount mockAccount = mock(UserAccount.class);

            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.createAnswer(
                            ACCOUNT_ID, QUESTION_ID, VALID_CONTENT, AnswerType.PRACTICE_INTERVIEW))
                    .isInstanceOf(QuestionNotFoundException.class);
        }

        @Test
        @DisplayName("REAL_INTERVIEW 타입으로 Answer 생성 성공")
        void createAnswer_WithRealInterviewType_ShouldSucceed() {
            // Given
            UserAccount mockAccount = mock(UserAccount.class);
            Question mockQuestion = mock(Question.class);

            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(mockAccount));
            when(questionRepository.findById(QUESTION_ID)).thenReturn(Optional.of(mockQuestion));

            // When
            Answer result = answerDomainService.createAnswer(
                    ACCOUNT_ID, QUESTION_ID, VALID_CONTENT, AnswerType.REAL_INTERVIEW
            );

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getType()).isEqualTo(AnswerType.REAL_INTERVIEW);
        }
    }

    @Nested
    @DisplayName("validateOwnership() 테스트")
    class ValidateOwnershipTest {

        @Test
        @DisplayName("소유자 검증 통과")
        void validateOwnership_WithOwner_ShouldPass() {
            // Given
            Answer answer = AnswerFixture.createAnswerWithAccountId(ACCOUNT_ID);

            // When & Then (예외 없이 통과)
            answerDomainService.validateOwnership(answer, ACCOUNT_ID);
        }

        @Test
        @DisplayName("비소유자 검증 시 AnswerAccessDeniedException 발생")
        void validateOwnership_WithNonOwner_ShouldThrowException() {
            // Given
            Long ownerAccountId = 1L;
            Long otherAccountId = 999L;
            Answer answer = AnswerFixture.createAnswerWithAccountId(ownerAccountId);

            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.validateOwnership(answer, otherAccountId))
                    .isInstanceOf(AnswerAccessDeniedException.class);
        }

        @Test
        @DisplayName("null 계정 ID로 검증 시 AnswerAccessDeniedException 발생")
        void validateOwnership_WithNullAccountId_ShouldThrowException() {
            // Given
            Answer answer = AnswerFixture.createAnswerWithAccountId(ACCOUNT_ID);

            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.validateOwnership(answer, null))
                    .isInstanceOf(AnswerAccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("transitionStatus() 테스트")
    class TransitionStatusTest {

        @Test
        @DisplayName("SUBMITTED -> IMMEDIATE_FEEDBACK_READY 유효한 전이")
        void transitionStatus_FromSubmittedToImmediateFeedbackReady_ShouldSucceed() {
            // Given
            Answer answer = AnswerFixture.createAnswer();
            assertThat(answer.getStatus()).isEqualTo(AnswerStatus.SUBMITTED);

            // When
            answerDomainService.transitionStatus(answer, AnswerStatus.IMMEDIATE_FEEDBACK_READY);

            // Then
            assertThat(answer.getStatus()).isEqualTo(AnswerStatus.IMMEDIATE_FEEDBACK_READY);
        }

        @Test
        @DisplayName("SUBMITTED -> COMPLETED 무효한 전이 시 예외 발생")
        void transitionStatus_FromSubmittedToCompleted_ShouldThrowException() {
            // Given
            Answer answer = AnswerFixture.createAnswer();

            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.transitionStatus(answer, AnswerStatus.COMPLETED))
                    .isInstanceOf(InvalidAnswerStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("validateAnswerContent() 테스트")
    class ValidateAnswerContentTest {

        @Test
        @DisplayName("유효한 내용 검증 통과")
        void validateAnswerContent_WithValidContent_ShouldPass() {
            // When & Then (예외 없이 통과)
            answerDomainService.validateAnswerContent(VALID_CONTENT);
        }

        @Test
        @DisplayName("null 내용 검증 시 AnswerInvalidContentException 발생")
        void validateAnswerContent_WithNullContent_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.validateAnswerContent(null))
                    .isInstanceOf(AnswerInvalidContentException.class);
        }

        @Test
        @DisplayName("빈 문자열 검증 시 AnswerInvalidContentException 발생")
        void validateAnswerContent_WithEmptyContent_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.validateAnswerContent(""))
                    .isInstanceOf(AnswerInvalidContentException.class);
        }

        @Test
        @DisplayName("공백만 있는 내용 검증 시 AnswerInvalidContentException 발생")
        void validateAnswerContent_WithBlankContent_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.validateAnswerContent("   "))
                    .isInstanceOf(AnswerInvalidContentException.class);
        }

        @Test
        @DisplayName("최대 길이 초과 시 AnswerInvalidContentException 발생")
        void validateAnswerContent_WithExceedingLength_ShouldThrowException() {
            // Given
            String exceedingContent = "A".repeat(MAX_CONTENT_LENGTH + 1);

            // When & Then
            assertThatThrownBy(() ->
                    answerDomainService.validateAnswerContent(exceedingContent))
                    .isInstanceOf(AnswerInvalidContentException.class);
        }

        @Test
        @DisplayName("최대 길이 경계값 검증 통과")
        void validateAnswerContent_WithMaxLength_ShouldPass() {
            // Given
            String maxContent = "A".repeat(MAX_CONTENT_LENGTH);

            // When & Then (예외 없이 통과)
            answerDomainService.validateAnswerContent(maxContent);
        }
    }
}
