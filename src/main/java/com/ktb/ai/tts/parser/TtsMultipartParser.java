package com.ktb.ai.tts.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ktb.ai.tts.exception.TtsDependencyFailedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class TtsMultipartParser {

    private static final byte CR = 13;
    private static final byte LF = 10;
    private static final byte HYPHEN = 45;

    private static final byte[] CRLF_CRLF = new byte[]{CR, LF, CR, LF};
    private static final byte[] LF_LF = new byte[]{LF, LF};

    private static final String APPLICATION_JSON = "application/json";
    private static final String HEADER_CONTENT_TYPE = "content-type";
    private static final String HEADER_CONTENT_DISPOSITION = "content-disposition";

    private static final Pattern BOUNDARY_PATTERN = Pattern.compile("boundary=(?:\"([^\"]+)\"|([^;]+))",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_NAME_QUOTED_PATTERN = Pattern.compile("filename=\"([^\"]+)\"",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern FILE_NAME_PLAIN_PATTERN = Pattern.compile("filename=([^;]+)",
            Pattern.CASE_INSENSITIVE);

    private final ObjectMapper objectMapper;

    public TtsMultipartParseResult parse(byte[] payload, String contentType) {
        log.debug("TTS multipart parse started - payloadBytes={}, contentType={}",
                payload == null ? 0 : payload.length, contentType);
        try {
            String boundary = extractBoundary(contentType);
            if (boundary == null || boundary.isBlank()) {
                throw new TtsDependencyFailedException("TTS multipart 응답 boundary 파싱에 실패했습니다");
            }

            List<MultipartPart> parts = parseMultipartParts(payload, boundary);
            byte[] audioPayload = null;
            String audioContentType = null;
            String fileName = null;
            String message = null;
            Long userId = null;
            String sessionId = null;
            List<String> parsedContentTypes = new ArrayList<>();

            for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
                MultipartPart part = parts.get(partIndex);
                String partContentType = normalizeContentType(part.headers().get(HEADER_CONTENT_TYPE));
                String contentDisposition = part.headers().get(HEADER_CONTENT_DISPOSITION);
                Optional<String> detectedFileNameOptional = extractFileName(contentDisposition);
                String detectedFileName = detectedFileNameOptional.orElse("");
                boolean hasBody = part.body() != null && part.body().length > 0;
                boolean isAudioContentType = partContentType.startsWith("audio/");
                boolean isJsonPart = APPLICATION_JSON.equals(partContentType);

                parsedContentTypes.add(partContentType.isBlank() ? "(empty)" : partContentType);
                log.debug("TTS multipart part inspected - index={}, contentType={}, bodyBytes={}, hasFileName={}, fileName={}",
                        partIndex, partContentType, hasBody ? part.body().length : 0,
                        detectedFileNameOptional.isPresent(), detectedFileName);

                if (audioPayload == null && hasBody && isAudioContentType) {
                    audioPayload = part.body();
                    audioContentType = partContentType;
                    fileName = detectedFileNameOptional.orElse(null);
                    continue;
                }

                if (!isJsonPart && hasBody) {
                    log.warn("Ignored non-audio multipart part in strict mode - index={}, contentType={}, bodyBytes={}",
                            partIndex, partContentType, part.body().length);
                }

                if (isJsonPart && message == null) {
                    try {
                        JsonNode root = objectMapper.readTree(part.body());
                        if (root.hasNonNull("message")) {
                            message = root.get("message").asText();
                        }
                        JsonNode dataNode = root.path("data");
                        if (dataNode.hasNonNull("user_id")) {
                            userId = dataNode.get("user_id").asLong();
                        }
                        if (dataNode.hasNonNull("session_id")) {
                            sessionId = dataNode.get("session_id").asText();
                        }
                    } catch (Exception e) {
                        log.warn("Failed to parse TTS multipart json metadata - error: {}", e.getMessage());
                    }
                }
            }

            if (audioPayload == null || audioPayload.length == 0) {
                log.warn("TTS multipart had no audio/* part in strict mode - contentTypes={}", parsedContentTypes);
                throw new TtsDependencyFailedException("TTS multipart 응답에서 audio/* 파트를 찾지 못했습니다");
            }

            log.info("TTS multipart parsed - payloadBytes: {}, audioBytes: {}, overheadBytes: {}, parts: {}",
                    payload.length, audioPayload.length, payload.length - audioPayload.length, parts.size());

            return new TtsMultipartParseResult(audioPayload, audioContentType, fileName, message, userId, sessionId);
        } catch (TtsDependencyFailedException e) {
            log.warn("TTS multipart parse failed - payloadBytes={}, contentType={}, reason={}",
                    payload == null ? 0 : payload.length, contentType, e.getMessage());
            throw e;
        }
    }

    private List<MultipartPart> parseMultipartParts(byte[] bodyBytes, String boundary) {
        byte[] boundaryBytes = ("--" + boundary).getBytes(StandardCharsets.US_ASCII);
        int firstBoundaryIndex = findBoundaryLine(bodyBytes, boundaryBytes, 0, true, true);
        if (firstBoundaryIndex == -1) {
            throw new TtsDependencyFailedException("TTS multipart 시작 boundary를 찾지 못했습니다");
        }
        if (isClosingBoundaryAt(bodyBytes, boundaryBytes, firstBoundaryIndex)) {
            throw new TtsDependencyFailedException("TTS multipart 응답이 비어 있습니다");
        }

        int firstBoundarySuffix = firstBoundaryIndex + boundaryBytes.length;
        int firstLineBreakLength = getLineBreakLength(bodyBytes, firstBoundarySuffix);
        if (firstLineBreakLength == 0) {
            throw new TtsDependencyFailedException("TTS multipart 경계 포맷이 올바르지 않습니다");
        }

        int partStart = firstBoundarySuffix + firstLineBreakLength;
        List<MultipartPart> parts = new ArrayList<>();

        while (partStart < bodyBytes.length) {
            int nextBoundaryIndex = findBoundaryLine(bodyBytes, boundaryBytes, partStart, false, true);
            if (nextBoundaryIndex == -1) {
                throw new TtsDependencyFailedException("TTS multipart 종료 boundary를 찾지 못했습니다");
            }

            int partEnd = trimBoundaryLeadingLineBreak(bodyBytes, nextBoundaryIndex);
            if (partEnd < partStart) {
                throw new TtsDependencyFailedException("TTS multipart part 범위가 올바르지 않습니다");
            }

            byte[] partBytes = Arrays.copyOfRange(bodyBytes, partStart, partEnd);
            if (partBytes.length > 0) {
                parts.add(splitHeadersAndBody(partBytes));
            }

            if (isClosingBoundaryAt(bodyBytes, boundaryBytes, nextBoundaryIndex)) {
                return parts;
            }

            int boundarySuffix = nextBoundaryIndex + boundaryBytes.length;
            int nextLineBreakLength = getLineBreakLength(bodyBytes, boundarySuffix);
            if (nextLineBreakLength == 0) {
                throw new TtsDependencyFailedException("TTS multipart part 구분 포맷이 올바르지 않습니다");
            }
            partStart = boundarySuffix + nextLineBreakLength;
        }

        throw new TtsDependencyFailedException("TTS multipart 파싱 중 예기치 않게 종료되었습니다");
    }

    private MultipartPart splitHeadersAndBody(byte[] partBytes) {
        int headerEndIndex = findSequence(partBytes, CRLF_CRLF, 0);
        int bodyOffset = 4;
        if (headerEndIndex == -1) {
            headerEndIndex = findSequence(partBytes, LF_LF, 0);
            bodyOffset = 2;
        }
        if (headerEndIndex == -1) {
            throw new TtsDependencyFailedException("TTS multipart part 헤더 파싱에 실패했습니다");
        }

        byte[] headerBytes = Arrays.copyOfRange(partBytes, 0, headerEndIndex);
        byte[] body = Arrays.copyOfRange(partBytes, headerEndIndex + bodyOffset, partBytes.length);
        return new MultipartPart(parseHeaders(headerBytes), body);
    }

    private Map<String, String> parseHeaders(byte[] headerBytes) {
        String headersText = new String(headerBytes, StandardCharsets.UTF_8);
        String[] lines = headersText.split("\\r?\\n");
        Map<String, String> headers = new LinkedHashMap<>();
        for (String line : lines) {
            int separatorIndex = line.indexOf(':');
            if (separatorIndex == -1) {
                continue;
            }
            String key = line.substring(0, separatorIndex).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(separatorIndex + 1).trim();
            if (!key.isEmpty()) {
                headers.put(key, value);
            }
        }
        return headers;
    }

    private String extractBoundary(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return null;
        }
        Matcher matcher = BOUNDARY_PATTERN.matcher(contentType);
        if (!matcher.find()) {
            return null;
        }
        String boundary = matcher.group(1) != null ? matcher.group(1) : matcher.group(2);
        return boundary == null ? null : boundary.trim();
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

    private int findBoundaryLine(
            byte[] source,
            byte[] boundaryBytes,
            int startIndex,
            boolean allowStart,
            boolean allowLfPrefix
    ) {
        int cursor = startIndex;
        while (cursor < source.length) {
            int index = findSequence(source, boundaryBytes, cursor);
            if (index == -1) {
                return -1;
            }

            boolean isAtStart = index == 0;
            boolean hasCrLfPrefix = index >= 2 && source[index - 2] == CR && source[index - 1] == LF;
            boolean hasLfPrefixOnly = index >= 1 && source[index - 1] == LF && !hasCrLfPrefix;
            boolean hasValidPrefix = (allowStart && isAtStart) || hasCrLfPrefix || (allowLfPrefix && hasLfPrefixOnly);

            int suffixIndex = index + boundaryBytes.length;
            boolean isClosing = suffixIndex + 1 < source.length
                    && source[suffixIndex] == HYPHEN
                    && source[suffixIndex + 1] == HYPHEN;
            int lineBreakLength = getLineBreakLength(source, suffixIndex);
            boolean hasValidSuffix = isClosing || lineBreakLength > 0;

            if (hasValidPrefix && hasValidSuffix) {
                return index;
            }

            cursor = index + 1;
        }
        return -1;
    }

    private int findSequence(byte[] source, byte[] target, int startIndex) {
        if (target.length == 0 || source.length < target.length) {
            return -1;
        }
        for (int i = startIndex; i <= source.length - target.length; i++) {
            boolean matched = true;
            for (int j = 0; j < target.length; j++) {
                if (source[i + j] != target[j]) {
                    matched = false;
                    break;
                }
            }
            if (matched) {
                return i;
            }
        }
        return -1;
    }

    private int getLineBreakLength(byte[] source, int index) {
        if (index >= source.length) {
            return 0;
        }
        if (index + 1 < source.length && source[index] == CR && source[index + 1] == LF) {
            return 2;
        }
        if (source[index] == LF) {
            return 1;
        }
        return 0;
    }

    private int trimBoundaryLeadingLineBreak(byte[] source, int boundaryIndex) {
        if (boundaryIndex >= 2 && source[boundaryIndex - 2] == CR && source[boundaryIndex - 1] == LF) {
            return boundaryIndex - 2;
        }
        if (boundaryIndex >= 1 && source[boundaryIndex - 1] == LF) {
            return boundaryIndex - 1;
        }
        return boundaryIndex;
    }

    private boolean isClosingBoundaryAt(byte[] source, byte[] boundaryBytes, int boundaryIndex) {
        int suffixIndex = boundaryIndex + boundaryBytes.length;
        return suffixIndex + 1 < source.length
                && source[suffixIndex] == HYPHEN
                && source[suffixIndex + 1] == HYPHEN;
    }

    private record MultipartPart(Map<String, String> headers, byte[] body) {
    }
}
