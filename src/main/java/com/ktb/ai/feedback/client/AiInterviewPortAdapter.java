package com.ktb.ai.feedback.client;

import com.ktb.ai.feedback.exception.AiFeedbackRequestRejectedException;
import com.ktb.ai.feedback.exception.AiFeedbackRetryableException;
import com.ktb.interview.dto.ai.InterviewFeedbackApiResponse;
import com.ktb.interview.dto.ai.InterviewFeedbackRequest;
import com.ktb.interview.dto.ai.InterviewFollowUpQuestionApiResponse;
import com.ktb.interview.dto.ai.InterviewFollowUpQuestionRequest;
import com.ktb.interview.port.out.AiInterviewPort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@link AiInterviewPort}의 HTTP 어댑터 구현체.
 */
@Component
@Slf4j
public class AiInterviewPortAdapter implements AiInterviewPort {

    private final RestClient restClient;

    @Value("${ai.feedback.base-url}")
    private String baseUrl;

    @Value("${ai.feedback.endpoint}")
    private String endpoint;

    @Value("${ai.feedback.follow-up-endpoint}")
    private String followUpEndpoint;

    public AiInterviewPortAdapter(@Qualifier("aiRestClient") RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public InterviewFeedbackApiResponse requestFeedback(InterviewFeedbackRequest request) {
        String url = baseUrl + endpoint;
        try {
            InterviewFeedbackApiResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        throw new AiFeedbackRequestRejectedException(
                                "AI feedback request rejected - status="
                                        + res.getStatusCode().value() + ", body=" + body);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        throw new AiFeedbackRetryableException(
                                "AI feedback dependency failed - status="
                                        + res.getStatusCode().value() + ", body=" + body);
                    })
                    .body(InterviewFeedbackApiResponse.class);

            if (response == null || response.data() == null) {
                throw new AiFeedbackRetryableException("AI feedback response is empty");
            }
            return response;
        } catch (AiFeedbackRequestRejectedException | AiFeedbackRetryableException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("AI feedback request failed - retryable, reason={}", e.getMessage());
            throw new AiFeedbackRetryableException("AI feedback request failed: " + e.getMessage(), e);
        }
    }

    @Override
    public InterviewFollowUpQuestionApiResponse requestFollowUpQuestion(InterviewFollowUpQuestionRequest request) {
        String url = baseUrl + followUpEndpoint;
        try {
            InterviewFollowUpQuestionApiResponse response = restClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        throw new AiFeedbackRequestRejectedException(
                                "AI follow-up request rejected - status="
                                        + res.getStatusCode().value() + ", body=" + body);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        throw new AiFeedbackRetryableException(
                                "AI follow-up dependency failed - status="
                                        + res.getStatusCode().value() + ", body=" + body);
                    })
                    .body(InterviewFollowUpQuestionApiResponse.class);

            if (response == null || response.data() == null) {
                throw new AiFeedbackRetryableException("AI follow-up response is empty");
            }
            return response;
        } catch (AiFeedbackRequestRejectedException | AiFeedbackRetryableException e) {
            throw e;
        } catch (RestClientException e) {
            log.warn("AI follow-up request failed - retryable, reason={}", e.getMessage());
            throw new AiFeedbackRetryableException("AI follow-up request failed: " + e.getMessage(), e);
        }
    }
}
