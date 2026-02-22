package com.ktb.ai.tts.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class TtsVoiceNotFoundException extends BusinessException {

    public TtsVoiceNotFoundException() {
        super(ErrorCode.TTS_VOICE_NOT_FOUND, ErrorCode.TTS_VOICE_NOT_FOUND.getMessage());
    }

    public TtsVoiceNotFoundException(String message) {
        super(ErrorCode.TTS_VOICE_NOT_FOUND, message);
    }
}

