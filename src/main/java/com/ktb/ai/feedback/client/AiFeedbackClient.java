package com.ktb.ai.feedback.client;

import static com.ktb.common.domain.ErrorCode.AI_FEEDBACK_SERVICE_ERROR;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.ktb.ai.feedback.dto.request.AiFeedbackRequest;
import com.ktb.ai.feedback.dto.response.AiFeedbackResponse;
import com.ktb.common.dto.ApiResponse;
import org.springframework.core.ParameterizedTypeReference;
import com.ktb.ai.feedback.exception.AiFeedbackAlreadyInProgressException;
import com.ktb.ai.feedback.exception.AiFeedbackAnswerTooLongException;
import com.ktb.ai.feedback.exception.AiFeedbackAnswerTooShortException;
import com.ktb.ai.feedback.exception.AiFeedbackEmptyAnswerException;
import com.ktb.ai.feedback.exception.AiFeedbackEmptyQuestionException;
import com.ktb.ai.feedback.exception.AiFeedbackInternalServerException;
import com.ktb.ai.feedback.exception.AiFeedbackLlmServiceUnavailableException;
import com.ktb.ai.feedback.exception.AiFeedbackRateLimitException;
import com.ktb.ai.feedback.exception.AiFeedbackServiceException;
import com.ktb.ai.feedback.exception.AiFeedbackServiceTemporarilyUnavailableException;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiFeedbackClient {

    private static final String MESSAGE_EMPTY_QUESTION = "EMPTY_QUESTION";
    private static final String MESSAGE_EMPTY_ANSWER = "EMPTY_ANSWER";
    private static final String MESSAGE_ANSWER_TOO_SHORT = "ANSWER_TOO_SHORT";
    private static final String MESSAGE_ANSWER_TOO_LONG = "ANSWER_TOO_LONG";

    private final RestClient aiRestClient;

    @Value("${ai.feedback.base-url}")
    private String baseUrl;

    @Value("${ai.feedback.endpoint}")
    private String endpoint;

    public ApiResponse<AiFeedbackResponse> evaluate(AiFeedbackRequest request) {
        String url = baseUrl + endpoint;

        log.info("Requesting AI feedback - URL: {}, userId: {}, questionId: {}",
                url, request.userId(), request.questionId());

        try {
            // AI 서버가 정의한 요청 형태 그대로 전달합니다.
            ApiResponse<AiFeedbackResponse> response = aiRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        int statusCode = res.getStatusCode().value();
                        String responseBody = new String(res.getBody().readAllBytes());

                        log.error("AI Feedback 4xx error - status: {}, body: {}", statusCode, responseBody);

                        if (statusCode == 400) {
                            handle400Error(responseBody);
                        } else if (statusCode == 409) {
                            throw new AiFeedbackAlreadyInProgressException();
                        } else if (statusCode == 429) {
                            throw new AiFeedbackRateLimitException();
                        } else {
                            throw new AiFeedbackServiceException(
                                String.format("%s - status: %s", AI_FEEDBACK_SERVICE_ERROR.getMessage(), statusCode)
                            );
                        }
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        int statusCode = res.getStatusCode().value();
                        String responseBody = new String(res.getBody().readAllBytes());

                        log.error("AI Feedback 5xx error - status: {}, body: {}", statusCode, responseBody);

                        if (statusCode == 500) {
                            throw new AiFeedbackInternalServerException();
                        } else if (statusCode == 502) {
                            throw new AiFeedbackLlmServiceUnavailableException();
                        } else if (statusCode == 503) {
                            throw new AiFeedbackServiceTemporarilyUnavailableException();
                        } else {
                            throw new AiFeedbackServiceException(
                                String.format("%s - status: %s", AI_FEEDBACK_SERVICE_ERROR.getMessage(), statusCode)
                            );
                        }
                    })
                    .body(new ParameterizedTypeReference<>() {
                    });

            if (response == null) {
                throw new AiFeedbackServiceException("AI 피드백 응답이 null입니다");
            }

            if (!"generate_feedback_success".equals(response.message()) && !"bad_case_detected".equals(response.message())) {
                log.error("AI feedback request failed - unknown message: {}", response.message());
                throw new AiFeedbackServiceException(
                        "AI 피드백 생성 실패: 알 수 없는 응답 - " + response.message()
                );
            }

            log.info("AI feedback request successful - userId: {}, questionId: {}, message: {}",
                    request.userId(), request.questionId(), response.message());

            return response;

        } catch (RestClientException e) {
            log.error("AI feedback API call failed - URL: {}, error: {}", url, e.getMessage(), e);
            throw new AiFeedbackServiceException("AI 피드백 서버 호출 실패", e);
        }
    }

    private void handle400Error(String responseBody) {
        try {
            JsonNode jsonNode = JsonMapper.builder().build().readTree(responseBody);
            String message = jsonNode.has("message") ? jsonNode.get("message").asText() : "";

            switch (message) {
                case MESSAGE_EMPTY_QUESTION:
                    throw new AiFeedbackEmptyQuestionException();
                case MESSAGE_EMPTY_ANSWER:
                    throw new AiFeedbackEmptyAnswerException();
                case MESSAGE_ANSWER_TOO_SHORT:
                    throw new AiFeedbackAnswerTooShortException();
                case MESSAGE_ANSWER_TOO_LONG:
                    throw new AiFeedbackAnswerTooLongException();
                default:
                    throw new AiFeedbackServiceException(
                        String.format("%s - message: %s",AI_FEEDBACK_SERVICE_ERROR.getMessage() , message)
                    );
            }
        } catch (IOException e) {
            log.error("Failed to parse error response body: {}", responseBody, e);
            throw new AiFeedbackServiceException("AI 피드백 에러 응답 파싱 실패", e);
        }
    }
}
