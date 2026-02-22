package com.ktb.ai.tts.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class TtsServiceException extends BusinessException {

    public TtsServiceException() {
        super(ErrorCode.TTS_SERVICE_ERROR, ErrorCode.TTS_SERVICE_ERROR.getMessage());
    }

    public TtsServiceException(String message) {
        super(ErrorCode.TTS_SERVICE_ERROR, message);
    }

    public TtsServiceException(Throwable cause) {
        super(ErrorCode.TTS_SERVICE_ERROR, ErrorCode.TTS_SERVICE_ERROR.getMessage(), cause);
    }

    public TtsServiceException(String message, Throwable cause) {
        super(ErrorCode.TTS_SERVICE_ERROR, message, cause);
    }
}

