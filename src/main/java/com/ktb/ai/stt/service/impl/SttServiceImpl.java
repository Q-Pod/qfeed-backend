package com.ktb.ai.stt.service.impl;

import com.ktb.ai.stt.client.SttClient;
import com.ktb.ai.stt.dto.request.SttRequest;
import com.ktb.ai.stt.service.SttService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SttServiceImpl implements SttService {

    private final SttClient sttClient;

    @Override
    public String convertToText(Long userId, String sessionId, String audioUrl) {
        log.debug("Converting audio to text - userId: {}, sessionId: {}, audioUrl: {}",
                userId, sessionId, audioUrl);

        SttRequest request = new SttRequest(userId, sessionId, audioUrl);

        String convertedText = sttClient.convert(request);

        log.info("STT conversion completed - userId: {}, sessionId: {}, textLength: {}",
                userId, sessionId, convertedText.length());

        return convertedText;
    }
}
