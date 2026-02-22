package com.ktb.ai.tts.service;

import com.ktb.ai.tts.dto.response.TtsMultipartResponse;

/**
 * TTS 서비스 인터페이스.
 */
public interface TtsService {

    /**
     * 텍스트를 음성으로 변환하고 multipart/mixed 응답을 반환합니다.
     */
    TtsMultipartResponse convertToSpeech(Long userId, String sessionId, String text);
}

