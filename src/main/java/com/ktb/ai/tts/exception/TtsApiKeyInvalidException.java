package com.ktb.ai.tts.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class TtsApiKeyInvalidException extends BusinessException {

    public TtsApiKeyInvalidException() {
        super(ErrorCode.TTS_API_KEY_INVALID, ErrorCode.TTS_API_KEY_INVALID.getMessage());
    }

    public TtsApiKeyInvalidException(String message) {
        super(ErrorCode.TTS_API_KEY_INVALID, message);
    }
}

