package com.ktb.async.core.worker;

import com.ktb.async.contract.BaseEvent;
import com.ktb.async.core.exception.RetryableException;
import java.time.Duration;
import java.time.Instant;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;

@Slf4j
public abstract class AbstractEventWorker<T extends BaseEvent> {

    private static final int MAX_RETRY_COUNT = 3;

    public void handle(T event) {
        Instant startedAt = Instant.now();
        MDC.put("traceId", event.traceId());
        MDC.put("eventId", event.eventId());
        try {
            log.info("Handling event - type={}, eventId={}, source={}, traceId={}",
                    event.eventType(), event.eventId(), event.source(), event.traceId());
            process(event);
            recordMetrics(event, Duration.between(startedAt, Instant.now()));
        } catch (RetryableException e) {
            handleRetryableError(event, e);
            throw e;
        } catch (RuntimeException e) {
            handleError(event, e);
            throw e;
        } finally {
            MDC.remove("traceId");
            MDC.remove("eventId");
        }
    }

    protected abstract void process(T event);

    protected void handleRetryableError(T event, RetryableException e) {
        if (event.retryCount() >= MAX_RETRY_COUNT) {
            log.warn("Max retry reached - type={}, eventId={}, retryCount={}",
                    event.eventType(), event.eventId(), event.retryCount());
            publishToDLQ(event, e);
        } else {
            log.warn("Retryable error - type={}, eventId={}, retry={}/{}",
                    event.eventType(), event.eventId(), event.retryCount(), MAX_RETRY_COUNT);
        }
    }

    protected void handleError(T event, RuntimeException e) {
        log.error("Event processing failed - type={}, eventId={}, message={}",
                event.eventType(), event.eventId(), e.getMessage(), e);
    }

    // Kafka 미도입 단계: 로그로 DLQ 이동 기록 (향후 KafkaTemplate.send("qfeed.dlq.*") 교체)
    protected void publishToDLQ(T event, Exception e) {
        log.error("[DLQ] Event moved to dead letter queue - type={}, eventId={}, error={}",
                event.eventType(), event.eventId(), e.getMessage());
    }

    protected void recordMetrics(T event, Duration duration) {
        log.debug("Event processed - type={}, eventId={}, durationMs={}",
                event.eventType(), event.eventId(), duration.toMillis());
    }
}
