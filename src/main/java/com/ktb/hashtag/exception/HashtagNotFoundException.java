package com.ktb.hashtag.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.exception.BusinessException;

public class HashtagNotFoundException extends BusinessException {

    public HashtagNotFoundException() {
        super(ErrorCode.HASHTAG_NOT_FOUND);
    }

    public HashtagNotFoundException(Long hashtagId) {
        super(ErrorCode.HASHTAG_NOT_FOUND,
              "해시태그를 찾을 수 없습니다: " + hashtagId);
    }
}
