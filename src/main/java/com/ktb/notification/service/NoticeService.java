package com.ktb.notification.service;

import com.ktb.notification.dto.request.NoticeCreateRequest;
import com.ktb.notification.dto.request.NoticeUpdateRequest;
import com.ktb.notification.dto.response.NoticeResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface NoticeService {

    Page<NoticeResponse> getPublishedNotices(Pageable pageable);

    NoticeResponse getNotice(Long noticeId);

    NoticeResponse createNotice(NoticeCreateRequest request);

    NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request);

    NoticeResponse publishNotice(Long noticeId);

    void deleteNotice(Long noticeId);

    Page<NoticeResponse> getAllNotices(Pageable pageable);
}
