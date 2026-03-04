package com.ktb.file.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class FileInvalidMetadataException extends BusinessException {

    public FileInvalidMetadataException(ErrorCode errorCode) {
        super(errorCode);
    }

    public FileInvalidMetadataException(ErrorCode errorCode, String reason) {
        super(errorCode, String.format("%s %s", errorCode.getMessage(), reason));
    }

    public FileInvalidMetadataException(String reason) {
        super(ErrorCode.FILE_INVALID_METADATA,
                String.format("%s %s", ErrorCode.FILE_INVALID_METADATA.getMessage(), reason));
    }
}
