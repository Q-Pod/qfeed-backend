package com.ktb.notification.dto.response;

public record NoticePublishResult(
        Long noticeId,
        Long campaignId,
        int targetCount,
        int notificationCount,
        String message
) {

    public static NoticePublishResult success(
            Long noticeId,
            Long campaignId,
            int targetCount,
            int notificationCount
    ) {
        return new NoticePublishResult(
                noticeId,
                campaignId,
                targetCount,
                notificationCount,
                "공지사항 발행 및 알림 생성 완료"
        );
    }

    public static NoticePublishResult noTargets(Long noticeId) {
        return new NoticePublishResult(
                noticeId,
                null,
                0,
                0,
                "발송 대상이 없습니다"
        );
    }

    public static NoticePublishResult scheduled(Long noticeId, Long campaignId) {
        return new NoticePublishResult(
                noticeId,
                campaignId,
                0,
                0,
                "공지사항 발행 완료. 알림 대상 등록 처리 중"
        );
    }
}
