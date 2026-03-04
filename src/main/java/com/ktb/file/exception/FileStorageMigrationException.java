package com.ktb.file.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class FileStorageMigrationException extends BusinessException {

    public FileStorageMigrationException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileStorageMigrationException(ErrorCode errorCode, Throwable cause) {
        super(errorCode, errorCode.getMessage(), cause);
    }
}
