package com.ktb.ai.tts.dto.response;

/**
 * AI TTS 서버에서 반환한 multipart/mixed 페이로드를 그대로 전달하기 위한 응답 모델.
 */
public record TtsMultipartResponse(
        byte[] payload,
        String contentType,
        String contentDisposition
) {
}

