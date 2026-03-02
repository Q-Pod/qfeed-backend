package com.ktb.ai.tts.service;

import com.ktb.ai.tts.dto.response.TtsAudioResponse;

/**
 * TTS 서비스 인터페이스.
 */
public interface TtsService {

    /**
     * 텍스트를 음성으로 변환하고 audio payload를 반환합니다.
     */
    TtsAudioResponse convertToSpeech(Long userId, String sessionId, String text);
}
