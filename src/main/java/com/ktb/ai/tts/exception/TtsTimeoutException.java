package com.ktb.ai.tts.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class TtsTimeoutException extends BusinessException {

    public TtsTimeoutException() {
        super(ErrorCode.TTS_TIMEOUT, ErrorCode.TTS_TIMEOUT.getMessage());
    }

    public TtsTimeoutException(String message) {
        super(ErrorCode.TTS_TIMEOUT, message);
    }

    public TtsTimeoutException(Throwable cause) {
        super(ErrorCode.TTS_TIMEOUT, ErrorCode.TTS_TIMEOUT.getMessage(), cause);
    }
}

