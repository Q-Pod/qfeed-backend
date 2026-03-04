package com.ktb.notification.service;

import com.ktb.notification.dto.request.NoticePublishRequest;
import com.ktb.notification.dto.response.NoticePublishResult;

public interface NoticePublishApplicationService {

    NoticePublishResult publish(Long noticeId, NoticePublishRequest request);
}
