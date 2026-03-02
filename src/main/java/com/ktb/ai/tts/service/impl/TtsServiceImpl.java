package com.ktb.ai.tts.service.impl;

import com.ktb.ai.tts.client.TtsClient;
import com.ktb.ai.tts.dto.request.TtsRequest;
import com.ktb.ai.tts.dto.response.TtsAudioResponse;
import com.ktb.ai.tts.service.TtsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class TtsServiceImpl implements TtsService {

    private final TtsClient ttsClient;

    @Override
    public TtsAudioResponse convertToSpeech(Long userId, String sessionId, String text) {
        log.debug("Converting text to speech - userId: {}, sessionId: {}, textLength: {}",
                userId, sessionId, text == null ? 0 : text.length());

        TtsRequest request = new TtsRequest(userId, sessionId, text);
        TtsAudioResponse response = ttsClient.synthesize(request);

        log.info("TTS conversion completed - userId: {}, sessionId: {}, payloadBytes: {}",
                userId, sessionId, response.audioPayload().length);

        return response;
    }
}
