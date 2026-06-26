package com.ktb.common.util;

import io.opentelemetry.api.trace.Span;

public class TelemetryUtils {

    private TelemetryUtils() {}

    /**
     * 현재 활성 OTel span에 세션 메타데이터를 추가한다.
     * auto-instrumentation이 만든 span을 재사용하므로 새 span을 생성하지 않는다.
     * NonRecordingSpan의 set_attribute는 SDK 내부에서 no-op으로 처리된다.
     */
    public static void attachSessionAttributes(
            String sessionId,
            Long userId,
            String interviewType   // null 허용
    ) {
        Span span = Span.current();
        if (sessionId != null)     span.setAttribute("qfeed.session_id", sessionId);
        if (userId != null)        span.setAttribute("qfeed.user_id", String.valueOf(userId));
        if (interviewType != null) span.setAttribute("qfeed.interview_type", interviewType);
    }
}
