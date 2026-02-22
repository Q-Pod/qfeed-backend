package com.ktb.ai.tts.client;

import com.ktb.ai.tts.dto.request.TtsRequest;
import com.ktb.ai.tts.dto.response.TtsMultipartResponse;
import com.ktb.ai.tts.exception.TtsApiKeyInvalidException;
import com.ktb.ai.tts.exception.TtsDependencyFailedException;
import com.ktb.ai.tts.exception.TtsRateLimitExceededException;
import com.ktb.ai.tts.exception.TtsServiceException;
import com.ktb.ai.tts.exception.TtsTimeoutException;
import com.ktb.ai.tts.exception.TtsVoiceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Slf4j
@Component
@RequiredArgsConstructor
public class TtsClient {

    private static final int STATUS_API_KEY_INVALID = 401;
    private static final int STATUS_TTS_VOICE_NOT_FOUND = 404;
    private static final int STATUS_TTS_TIMEOUT = 408;
    private static final int STATUS_RATE_LIMIT_EXCEEDED = 429;
    private static final int STATUS_BAD_GATEWAY = 502;

    private final RestClient aiRestClient;

    @Value("${ai.tts.base-url}")
    private String baseUrl;

    @Value("${ai.tts.endpoint}")
    private String endpoint;

    /**
     * TTS 변환 요청을 수행하고 multipart/mixed 바이트를 그대로 반환합니다.
     */
    public TtsMultipartResponse synthesize(TtsRequest request) {
        String url = baseUrl + endpoint;
        log.info("Requesting TTS conversion - URL: {}, userId: {}, sessionId: {}, textLength: {}",
                url, request.userId(), request.sessionId(), request.text() == null ? 0 : request.text().length());

        try {
            ResponseEntity<byte[]> response = aiRestClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        handleClientError(res.getStatusCode().value(), body);
                    })
                    .onStatus(HttpStatusCode::is5xxServerError, (req, res) -> {
                        String body = new String(res.getBody().readAllBytes());
                        handleServerError(res.getStatusCode().value(), body);
                    })
                    .toEntity(byte[].class);

            byte[] payload = response.getBody() == null ? new byte[0] : response.getBody();
            String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);

            log.info("TTS conversion successful - userId: {}, sessionId: {}, payloadBytes: {}",
                    request.userId(), request.sessionId(), payload.length);

            return new TtsMultipartResponse(payload, contentType, contentDisposition);
        } catch (TtsApiKeyInvalidException
                 | TtsVoiceNotFoundException
                 | TtsTimeoutException
                 | TtsRateLimitExceededException
                 | TtsDependencyFailedException
                 | TtsServiceException e) {
            throw e;
        } catch (RestClientException e) {
            log.error("TTS API call failed - URL: {}, error: {}", url, e.getMessage(), e);
            throw new TtsDependencyFailedException("TTS 서버 호출 실패", e);
        }
    }

    private void handleClientError(int statusCode, String responseBody) {
        log.error("TTS client error - statusCode: {}, body: {}", statusCode, responseBody);
        switch (statusCode) {
            case STATUS_API_KEY_INVALID:
                throw new TtsApiKeyInvalidException("TTS API 키가 유효하지 않습니다");
            case STATUS_TTS_VOICE_NOT_FOUND:
                throw new TtsVoiceNotFoundException("TTS 음성 모델을 찾을 수 없습니다");
            case STATUS_TTS_TIMEOUT:
                throw new TtsTimeoutException("TTS 변환 시간이 초과되었습니다");
            case STATUS_RATE_LIMIT_EXCEEDED:
                throw new TtsRateLimitExceededException("TTS 요청 한도를 초과했습니다");
            default:
                throw new TtsServiceException("TTS 클라이언트 오류: " + statusCode);
        }
    }

    private void handleServerError(int statusCode, String responseBody) {
        log.error("TTS server error - statusCode: {}, body: {}", statusCode, responseBody);
        if (statusCode == STATUS_BAD_GATEWAY) {
            throw new TtsDependencyFailedException("TTS 의존 서비스 호출 실패");
        }
        throw new TtsServiceException("TTS 서버 오류: " + statusCode);
    }
}
