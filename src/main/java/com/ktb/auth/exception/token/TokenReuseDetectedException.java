package com.ktb.auth.exception.token;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class TokenReuseDetectedException extends BusinessException {

    public TokenReuseDetectedException(Long familyId) {
        log.error("{} Family 폐기됨: {}", ErrorCode.TOKEN_REUSE_DETECTED, familyId);
        super(ErrorCode.TOKEN_REUSE_DETECTED);
    }

    public TokenReuseDetectedException(String familyUuid) {
        log.error("{} Family 폐기됨: {}", ErrorCode.TOKEN_REUSE_DETECTED, familyUuid);
        super(ErrorCode.TOKEN_REUSE_DETECTED);
    }
}
