package com.ktb.ai.tts.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class TtsDependencyFailedException extends BusinessException {

    public TtsDependencyFailedException() {
        super(ErrorCode.TTS_DEPENDENCY_FAILED, ErrorCode.TTS_DEPENDENCY_FAILED.getMessage());
    }

    public TtsDependencyFailedException(String message) {
        super(ErrorCode.TTS_DEPENDENCY_FAILED, message);
    }

    public TtsDependencyFailedException(String message, Throwable cause) {
        super(ErrorCode.TTS_DEPENDENCY_FAILED, message, cause);
    }
}

