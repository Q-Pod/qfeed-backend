package com.ktb.answer.controller;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.ktb.answer.domain.AnswerStatus;
import com.ktb.answer.domain.AnswerType;
import com.ktb.answer.dto.AnswerDetailResult;
import com.ktb.answer.dto.response.detail.AnswerDetailResponse;
import com.ktb.answer.exception.AnswerDetailInvalidInputException;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.common.dto.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
@DisplayName("AnswerController 단위 테스트")
class AnswerControllerTest {

    @Mock
    private AnswerDomainService answerDomainService;

    @Test
    @DisplayName("답변 상세 조회에서 쿼리 파라미터가 있으면 INVALID_INPUT 예외를 반환한다")
    void getAnswerDetail_WithQueryParams_ShouldThrowInvalidInput() {
        // Given
        AnswerController controller = new AnswerController(answerDomainService);
        SecurityUserAccount principal = new SecurityUserAccount(1L, "tester", List.of("ROLE_USER"));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of("expand", new String[] {"question"}));

        // When & Then
        assertThatThrownBy(() -> controller.getAnswerDetail(principal, 100L, request))
                .isInstanceOf(AnswerDetailInvalidInputException.class);
        verifyNoInteractions(answerDomainService);
    }

    @Test
    @DisplayName("답변 상세 조회에서 쿼리 파라미터가 없으면 상세 응답을 반환한다")
    void getAnswerDetail_WithoutQueryParams_ShouldReturnDetail() {
        // Given
        AnswerController controller = new AnswerController(answerDomainService);
        SecurityUserAccount principal = new SecurityUserAccount(1L, "tester", List.of("ROLE_USER"));

        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getParameterMap()).thenReturn(Map.of());

        AnswerDetailResult result = new AnswerDetailResult(
                100L,
                AnswerStatus.COMPLETED,
                AnswerType.PRACTICE_INTERVIEW,
                null,
                null,
                null,
                null,
                null
        );
        when(answerDomainService.getDetail(1L, 100L)).thenReturn(result);

        // When
        ResponseEntity<ApiResponse<AnswerDetailResponse>> response = controller.getAnswerDetail(principal, 100L, request);

        // Then
        verify(answerDomainService).getDetail(1L, 100L);
        org.assertj.core.api.Assertions.assertThat(response.getBody()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(response.getBody().data()).isNotNull();
        org.assertj.core.api.Assertions.assertThat(response.getBody().data().answerId()).isEqualTo(100L);
    }
}
