package com.ktb.ai.tts.dto.response;

/**
 * API 클라이언트로 반환할 TTS 오디오 응답 모델.
 */
public record TtsAudioResponse(
        byte[] audioPayload,
        String audioContentType,
        String fileName,
        String message,
        Long userId,
        String sessionId
) {
}
