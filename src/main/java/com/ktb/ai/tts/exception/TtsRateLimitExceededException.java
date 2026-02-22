package com.ktb.ai.tts.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class TtsRateLimitExceededException extends BusinessException {

    public TtsRateLimitExceededException() {
        super(ErrorCode.TTS_RATE_LIMIT_EXCEEDED, ErrorCode.TTS_RATE_LIMIT_EXCEEDED.getMessage());
    }

    public TtsRateLimitExceededException(String message) {
        super(ErrorCode.TTS_RATE_LIMIT_EXCEEDED, message);
    }
}

