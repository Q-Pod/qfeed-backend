package com.ktb.notification.service.impl;

import com.ktb.notification.domain.Notice;
import com.ktb.notification.dto.request.NoticeCreateRequest;
import com.ktb.notification.dto.request.NoticeUpdateRequest;
import com.ktb.notification.dto.response.NoticeResponse;
import com.ktb.notification.exception.NoticeNotFoundException;
import com.ktb.notification.service.NoticeService;
import com.ktb.notification.service.NoticeStore;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeServiceImpl implements NoticeService {

    private final NoticeStore noticeStore;

    @Override
    public Page<NoticeResponse> getPublishedNotices(Pageable pageable) {
        return noticeStore.findPublished(pageable)
                .map(NoticeResponse::from);
    }

    @Override
    public NoticeResponse getNotice(Long noticeId) {
        Notice notice = noticeStore.findActiveById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        return NoticeResponse.from(notice);
    }

    @Override
    @Transactional
    public NoticeResponse createNotice(NoticeCreateRequest request) {
        Notice notice = Notice.create(
                request.title(),
                request.body(),
                request.deeplink()
        );

        Notice saved = noticeStore.save(notice);

        return NoticeResponse.from(saved);
    }

    @Override
    @Transactional
    public NoticeResponse updateNotice(Long noticeId, NoticeUpdateRequest request) {
        Notice notice = noticeStore.findActiveById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        if (request.title() != null) {
            notice.updateTitle(request.title());
        }
        if (request.body() != null) {
            notice.updateBody(request.body());
        }
        if (request.deeplink() != null) {
            notice.updateDeeplink(request.deeplink());
        }

        return NoticeResponse.from(notice);
    }

    @Override
    @Transactional
    public NoticeResponse publishNotice(Long noticeId) {
        Notice notice = noticeStore.findActiveById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        notice.publish();

        return NoticeResponse.from(notice);
    }

    @Override
    @Transactional
    public void deleteNotice(Long noticeId) {
        Notice notice = noticeStore.findActiveById(noticeId)
                .orElseThrow(() -> new NoticeNotFoundException(noticeId));

        notice.delete();
    }

    @Override
    public Page<NoticeResponse> getAllNotices(Pageable pageable) {
        return noticeStore.findAllActive(pageable)
                .map(NoticeResponse::from);
    }
}
