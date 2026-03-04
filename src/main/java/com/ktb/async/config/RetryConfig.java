package com.ktb.async.config;

import com.ktb.async.core.exception.RetryableException;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@EnableRetry
public class RetryConfig {

    @Bean
    public RetryTemplate eventWorkerRetryTemplate() {
        return RetryTemplate.builder()
                .maxAttempts(3)
                .exponentialBackoff(
                        Duration.ofMinutes(1).toMillis(),
                        2.0,
                        Duration.ofMinutes(30).toMillis()
                )
                .retryOn(RetryableException.class)
                .build();
    }
}
