package com.ktb.ai.tts.parser;

/**
 * multipart/mixed TTS 응답 파싱 결과.
 */
public record TtsMultipartParseResult(
        byte[] audioPayload,
        String audioContentType,
        String fileName,
        String message,
        Long userId,
        String sessionId
) {
}
