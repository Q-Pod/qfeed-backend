package com.ktb.ai.tts.client;

import com.ktb.ai.tts.dto.request.TtsRequest;
import com.ktb.ai.tts.dto.response.TtsAudioResponse;
import com.ktb.ai.tts.exception.TtsApiKeyInvalidException;
import com.ktb.ai.tts.exception.TtsDependencyFailedException;
import com.ktb.ai.tts.exception.TtsRateLimitExceededException;
import com.ktb.ai.tts.exception.TtsServiceException;
import com.ktb.ai.tts.exception.TtsTimeoutException;
import com.ktb.ai.tts.exception.TtsVoiceNotFoundException;
import com.ktb.ai.tts.parser.TtsMultipartParseResult;
import com.ktb.ai.tts.parser.TtsMultipartParser;
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

import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TtsClient {

    private static final String MULTIPART_MIXED = "multipart/mixed";
    private static final String DEFAULT_AUDIO_CONTENT_TYPE = "audio/mpeg";
    private static final Pattern FILE_NAME_QUOTED_PATTERN = Pattern.compile("filename=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_NAME_PLAIN_PATTERN = Pattern.compile("filename=([^;]+)",
            Pattern.CASE_INSENSITIVE);

    private static final int STATUS_PARTIAL_CONTENT = 206;
    private static final int STATUS_API_KEY_INVALID = 401;
    private static final int STATUS_TTS_VOICE_NOT_FOUND = 404;
    private static final int STATUS_TTS_TIMEOUT = 408;
    private static final int STATUS_RATE_LIMIT_EXCEEDED = 429;
    private static final int STATUS_BAD_GATEWAY = 502;

    private final RestClient aiRestClient;
    private final TtsMultipartParser ttsMultipartParser;

    @Value("${ai.tts.base-url}")
    private String baseUrl;

    @Value("${ai.tts.endpoint}")
    private String endpoint;

    /**
     * TTS 변환 요청을 수행하고 오디오 payload를 반환합니다.
     */
    public TtsAudioResponse synthesize(TtsRequest request) {
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

            int statusCode = response.getStatusCode().value();
            byte[] payload = response.getBody() == null ? new byte[0] : response.getBody();
            String contentType = response.getHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
            String contentDisposition = response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION);
            if (statusCode == STATUS_PARTIAL_CONTENT) {
                String contentRange = response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE);
                log.error("TTS partial content response detected - URL: {}, userId: {}, sessionId: {}, contentRange: {}, payloadBytes: {}",
                        url, request.userId(), request.sessionId(), contentRange, payload.length);
                throw new TtsDependencyFailedException(
                        "TTS 응답이 부분 콘텐츠(206)로 반환되었습니다. 전체 페이로드가 필요합니다.");
            }

            log.info("TTS conversion successful - userId: {}, sessionId: {}, payloadBytes: {}",
                    request.userId(), request.sessionId(), payload.length);

            return toAudioResponse(request, payload, contentType, contentDisposition);
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

    private TtsAudioResponse toAudioResponse(
            TtsRequest request,
            byte[] payload,
            String responseContentType,
            String responseContentDisposition
    ) {
        String normalizedContentType = normalizeContentType(responseContentType);
        if (normalizedContentType.startsWith("audio/")) {
            log.debug("TTS upstream response is direct audio - contentType={}, payloadBytes={}",
                    normalizedContentType, payload.length);
            String fileName = extractFileName(responseContentDisposition).orElse(null);
            return new TtsAudioResponse(
                    payload,
                    normalizedContentType,
                    fileName,
                    null,
                    request.userId(),
                    request.sessionId()
            );
        }

        if (MULTIPART_MIXED.equals(normalizedContentType)) {
            log.debug("TTS upstream response is multipart - contentType={}, payloadBytes={}",
                    responseContentType, payload.length);
            TtsMultipartParseResult parsed = ttsMultipartParser.parse(payload, responseContentType);
            return new TtsAudioResponse(
                    parsed.audioPayload(),
                    parsed.audioContentType(),
                    parsed.fileName(),
                    parsed.message(),
                    parsed.userId() != null ? parsed.userId() : request.userId(),
                    parsed.sessionId() != null && !parsed.sessionId().isBlank()
                            ? parsed.sessionId()
                            : request.sessionId()
            );
        }

        throw new TtsDependencyFailedException("지원하지 않는 TTS 응답 Content-Type: " + responseContentType);
    }

    private Optional<String> extractFileName(String contentDisposition) {
        if (contentDisposition == null || contentDisposition.isBlank()) {
            return Optional.empty();
        }

        Matcher quotedMatcher = FILE_NAME_QUOTED_PATTERN.matcher(contentDisposition);
        if (quotedMatcher.find()) {
            return Optional.of(quotedMatcher.group(1));
        }

        Matcher plainMatcher = FILE_NAME_PLAIN_PATTERN.matcher(contentDisposition);
        if (plainMatcher.find()) {
            return Optional.of(plainMatcher.group(1).trim().replaceAll("^\"|\"$", ""));
        }

        return Optional.empty();
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        return contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
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
