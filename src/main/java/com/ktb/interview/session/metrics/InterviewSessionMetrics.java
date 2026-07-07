package com.ktb.interview.session.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class InterviewSessionMetrics {

    private final Counter realCompletedCounter;
    private final Counter practiceCompletedCounter;
    private final Counter realFailedCounter;
    private final Counter practiceFailedCounter;

    public InterviewSessionMetrics(MeterRegistry registry) {
        this.realCompletedCounter = Counter.builder("interview_session_completed_total")
                .tag("interview_type", "REAL_INTERVIEW")
                .register(registry);
        this.practiceCompletedCounter = Counter.builder("interview_session_completed_total")
                .tag("interview_type", "PRACTICE_INTERVIEW")
                .register(registry);
        this.realFailedCounter = Counter.builder("interview_session_failed_total")
                .tag("interview_type", "REAL_INTERVIEW")
                .register(registry);
        this.practiceFailedCounter = Counter.builder("interview_session_failed_total")
                .tag("interview_type", "PRACTICE_INTERVIEW")
                .register(registry);
    }

    private Counter resolveCompletedCounter(String interviewType) {
        return "REAL_INTERVIEW".equals(interviewType)
                ? realCompletedCounter
                : practiceCompletedCounter;
    }

    private Counter resolveFailedCounter(String interviewType) {
        return "REAL_INTERVIEW".equals(interviewType)
                ? realFailedCounter
                : practiceFailedCounter;
    }

    public void recordCompleted(String interviewType) {
        resolveCompletedCounter(interviewType).increment();
    }

    public void recordFailed(String interviewType) {
        resolveFailedCounter(interviewType).increment();
    }
}
