package com.ktb.answer.service;

import com.ktb.ai.feedback.dto.response.AiFeedbackBadCaseFeedback;
import com.ktb.ai.feedback.dto.response.AiFeedbackFeedback;
import com.ktb.ai.feedback.dto.response.AiFeedbackMetric;
import com.ktb.ai.feedback.dto.response.AiFeedbackResponse;
import com.ktb.ai.feedback.dto.response.BadCaseType;
import com.ktb.ai.feedback.service.AiFeedbackService;
import com.ktb.answer.domain.Answer;
import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.FeedbackStatus;
import com.ktb.answer.dto.response.feedback.FeedbackResponse;
import com.ktb.answer.exception.AnswerAccessDeniedException;
import com.ktb.answer.exception.AnswerNotFoundException;
import com.ktb.answer.repository.AnswerRepository;
import com.ktb.answer.service.impl.AiFeedbackOrchestratorImpl;
import com.ktb.auth.domain.UserAccount;
import com.ktb.common.dto.ApiResponse;
import com.ktb.metric.domain.Metric;
import com.ktb.metric.repository.AnswerMetricRepository;
import com.ktb.metric.repository.MetricRepository;
import com.ktb.question.domain.Question;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("AiFeedbackOrchestrator 단위 테스트")
class AiFeedbackOrchestratorTest {

    @Mock
    private AnswerRepository answerRepository;

    @Mock
    private AnswerDomainService answerDomainService;

    @Mock
    private AiFeedbackService aiFeedbackService;

    @Mock
    private MetricRepository metricRepository;

    @Mock
    private AnswerMetricRepository answerMetricRepository;

    @InjectMocks
    private AiFeedbackOrchestratorImpl aiFeedbackOrchestrator;

    private static final Long ACCOUNT_ID = 1L;
    private static final Long ANSWER_ID = 100L;
    private static final Long QUESTION_ID = 200L;

    @Nested
    @DisplayName("getFeedbackSync() 테스트")
    class GetFeedbackSyncTest {

        @Test
        @DisplayName("정상 응답 시 COMPLETED 상태와 피드백 반환")
        void getFeedbackSync_WithSuccessResponse_ShouldReturnCompleted() {
            // Given
            Answer mockAnswer = createMockAnswer();
            AiFeedbackResponse aiResponse = createSuccessAiFeedbackResponse();
            ApiResponse<AiFeedbackResponse> apiResponse = new ApiResponse<>("success", aiResponse);

            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));
            when(aiFeedbackService.evaluateSync(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(apiResponse);
            when(metricRepository.findAllByNameIn(anyList())).thenReturn(Collections.emptyList());
            when(metricRepository.save(any(Metric.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            FeedbackResponse result = aiFeedbackOrchestrator.getFeedbackSync(ANSWER_ID, ACCOUNT_ID);

            // Then
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.feedback()).isNotNull();
            assertThat(result.radarChart()).isNotNull();
            verify(answerRepository).save(mockAnswer);
        }

        @Test
        @DisplayName("존재하지 않는 답변 조회 시 AnswerNotFoundException 발생")
        void getFeedbackSync_WithNonExistentAnswer_ShouldThrowException() {
            // Given
            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    aiFeedbackOrchestrator.getFeedbackSync(ANSWER_ID, ACCOUNT_ID))
                    .isInstanceOf(AnswerNotFoundException.class);
        }

        @Test
        @DisplayName("권한 없는 사용자 접근 시 AnswerAccessDeniedException 발생")
        void getFeedbackSync_WithUnauthorizedUser_ShouldThrowException() {
            // Given
            Long otherAccountId = 999L;
            Answer mockAnswer = createMockAnswer();

            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));
            doThrow(new AnswerAccessDeniedException(ANSWER_ID, otherAccountId))
                    .when(answerDomainService).validateOwnership(mockAnswer, otherAccountId);

            // When & Then
            assertThatThrownBy(() ->
                    aiFeedbackOrchestrator.getFeedbackSync(ANSWER_ID, otherAccountId))
                    .isInstanceOf(AnswerAccessDeniedException.class);
        }

        @Test
        @DisplayName("bad_case 응답 시 실패 피드백 반환")
        void getFeedbackSync_WithBadCaseResponse_ShouldReturnFailed() {
            // Given
            Answer mockAnswer = createMockAnswer();
            AiFeedbackResponse badCaseResponse = createBadCaseAiFeedbackResponse();
            ApiResponse<AiFeedbackResponse> apiResponse = new ApiResponse<>("bad_case_detected", badCaseResponse);

            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));
            when(aiFeedbackService.evaluateSync(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(apiResponse);

            // When
            FeedbackResponse result = aiFeedbackOrchestrator.getFeedbackSync(ANSWER_ID, ACCOUNT_ID);

            // Then
            assertThat(result.status()).isEqualTo("COMPLETED");
            assertThat(result.feedback()).contains(BadCaseType.TOO_SHORT.getMessage());
            assertThat(result.radarChart()).isNull();
            verify(answerRepository).save(mockAnswer);
        }

        @Test
        @DisplayName("메트릭 저장 성공")
        void getFeedbackSync_ShouldSaveMetrics() {
            // Given
            Answer mockAnswer = createMockAnswer();
            AiFeedbackResponse aiResponse = createSuccessAiFeedbackResponse();
            ApiResponse<AiFeedbackResponse> apiResponse = new ApiResponse<>("success", aiResponse);

            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));
            when(aiFeedbackService.evaluateSync(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(apiResponse);
            when(metricRepository.findAllByNameIn(anyList())).thenReturn(Collections.emptyList());
            when(metricRepository.save(any(Metric.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            aiFeedbackOrchestrator.getFeedbackSync(ANSWER_ID, ACCOUNT_ID);

            // Then
            verify(answerMetricRepository).saveAll(anyList());
        }

        @Test
        @DisplayName("빈 메트릭 응답 시 메트릭 저장 스킵")
        void getFeedbackSync_WithEmptyMetrics_ShouldSkipMetricSave() {
            // Given
            Answer mockAnswer = createMockAnswer();
            AiFeedbackResponse aiResponse = new AiFeedbackResponse(
                    ACCOUNT_ID, QUESTION_ID, "PRACTICE_INTERVIEW", "CS", "DB",
                    null, null, false,
                    new AiFeedbackFeedback("강점입니다.", "개선사항입니다.")
            );
            ApiResponse<AiFeedbackResponse> apiResponse = new ApiResponse<>("success", aiResponse);

            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));
            when(aiFeedbackService.evaluateSync(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(apiResponse);

            // When
            aiFeedbackOrchestrator.getFeedbackSync(ANSWER_ID, ACCOUNT_ID);

            // Then
            verify(answerMetricRepository, never()).saveAll(anyList());
        }

        @Test
        @DisplayName("레이더 차트 데이터 정상 변환")
        void getFeedbackSync_ShouldConvertRadarChartCorrectly() {
            // Given
            Answer mockAnswer = createMockAnswer();
            AiFeedbackResponse aiResponse = createSuccessAiFeedbackResponse();
            ApiResponse<AiFeedbackResponse> apiResponse = new ApiResponse<>("success", aiResponse);

            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));
            when(aiFeedbackService.evaluateSync(any(), any(), any(), any(), any(), any(), any()))
                    .thenReturn(apiResponse);
            when(metricRepository.findAllByNameIn(anyList())).thenReturn(Collections.emptyList());
            when(metricRepository.save(any(Metric.class))).thenAnswer(invocation -> invocation.getArgument(0));

            // When
            FeedbackResponse result = aiFeedbackOrchestrator.getFeedbackSync(ANSWER_ID, ACCOUNT_ID);

            // Then
            assertThat(result.radarChart()).hasSize(2);
            assertThat(result.radarChart().get(0).metricName()).isEqualTo("논리성");
            assertThat(result.radarChart().get(0).score()).isEqualTo(4);
            assertThat(result.radarChart().get(0).maxScore()).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("getStatus() 테스트")
    class GetStatusTest {

        @Test
        @DisplayName("AI_FEEDBACK_PROCESSING 상태 매핑")
        void getStatus_WithProcessing_ShouldReturnProcessing() {
            // Given
            Answer mockAnswer = mock(Answer.class);
            when(mockAnswer.getStatus()).thenReturn(AnswerStatus.AI_FEEDBACK_PROCESSING);
            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));

            // When
            FeedbackStatus result = aiFeedbackOrchestrator.getStatus(ANSWER_ID);

            // Then
            assertThat(result).isEqualTo(FeedbackStatus.PROCESSING);
        }

        @Test
        @DisplayName("COMPLETED 상태 매핑")
        void getStatus_WithCompleted_ShouldReturnCompleted() {
            // Given
            Answer mockAnswer = mock(Answer.class);
            when(mockAnswer.getStatus()).thenReturn(AnswerStatus.COMPLETED);
            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));

            // When
            FeedbackStatus result = aiFeedbackOrchestrator.getStatus(ANSWER_ID);

            // Then
            assertThat(result).isEqualTo(FeedbackStatus.COMPLETED);
        }

        @Test
        @DisplayName("FAILED 상태 매핑")
        void getStatus_WithFailed_ShouldReturnFailed() {
            // Given
            Answer mockAnswer = mock(Answer.class);
            when(mockAnswer.getStatus()).thenReturn(AnswerStatus.FAILED);
            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.of(mockAnswer));

            // When
            FeedbackStatus result = aiFeedbackOrchestrator.getStatus(ANSWER_ID);

            // Then
            assertThat(result).isEqualTo(FeedbackStatus.FAILED);
        }

        @Test
        @DisplayName("존재하지 않는 답변 조회 시 예외 발생")
        void getStatus_WithNonExistentAnswer_ShouldThrowException() {
            // Given
            when(answerRepository.findById(ANSWER_ID)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() ->
                    aiFeedbackOrchestrator.getStatus(ANSWER_ID))
                    .isInstanceOf(AnswerNotFoundException.class);
        }
    }

    @Nested
    @DisplayName("enqueue() 테스트")
    class EnqueueTest {

        @Test
        @DisplayName("enqueue 호출 시 경고 로그만 출력 (미구현)")
        void enqueue_ShouldLogWarning() {
            // When & Then (예외 없이 호출 가능)
            aiFeedbackOrchestrator.enqueue(ANSWER_ID);
        }
    }

    @Nested
    @DisplayName("requestRetry() 테스트")
    class RequestRetryTest {

        @Test
        @DisplayName("requestRetry 호출 시 UnsupportedOperationException 발생")
        void requestRetry_ShouldThrowUnsupportedOperationException() {
            // When & Then
            assertThatThrownBy(() ->
                    aiFeedbackOrchestrator.requestRetry(ANSWER_ID))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("not yet implemented");
        }
    }

    // Helper methods

    private Answer createMockAnswer() {
        Answer answer = mock(Answer.class);
        Question question = mock(Question.class);
        UserAccount account = mock(UserAccount.class);

        when(answer.getId()).thenReturn(ANSWER_ID);
        when(answer.getQuestion()).thenReturn(question);
        when(answer.getAccount()).thenReturn(account);
        when(answer.getContent()).thenReturn("테스트 답변 내용");
        when(answer.getType()).thenReturn(AnswerType.PRACTICE_INTERVIEW);

        when(question.getId()).thenReturn(QUESTION_ID);
        when(question.getContent()).thenReturn("테스트 질문");
        when(question.getType()).thenReturn(QuestionType.CS);
        when(question.getCategory()).thenReturn(QuestionCategory.DB);

        when(account.getId()).thenReturn(ACCOUNT_ID);

        return answer;
    }

    private AiFeedbackResponse createSuccessAiFeedbackResponse() {
        List<AiFeedbackMetric> metrics = List.of(
                new AiFeedbackMetric("논리성", 4, "논리적으로 잘 설명했습니다."),
                new AiFeedbackMetric("명확성", 5, "매우 명확하게 답변했습니다.")
        );

        AiFeedbackFeedback feedback = new AiFeedbackFeedback(
                "강점: 논리적인 설명",
                "개선사항: 더 구체적인 예시 필요"
        );

        return new AiFeedbackResponse(
                ACCOUNT_ID, QUESTION_ID, "PRACTICE_INTERVIEW", "CS", "DB",
                metrics, null, false, feedback
        );
    }

    private AiFeedbackResponse createBadCaseAiFeedbackResponse() {
        AiFeedbackBadCaseFeedback badCaseFeedback = new AiFeedbackBadCaseFeedback(
                "TOO_SHORT",
                BadCaseType.TOO_SHORT.getMessage(),
                BadCaseType.TOO_SHORT.getGuidance()
        );

        return new AiFeedbackResponse(
                ACCOUNT_ID, QUESTION_ID, "PRACTICE_INTERVIEW", "CS", "DB",
                null, badCaseFeedback, null, null
        );
    }
}
