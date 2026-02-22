package com.ktb.ai.common.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * AI 서버 호출용 RestClient 공통 설정.
 */
@Configuration
public class AiClientConfig {

    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(300);

    @Bean
    public RestClient aiRestClient() {
        HttpClient httpClient = HttpClient.newBuilder()
                // Force HTTP/1.1 for deterministic protocol behavior
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(DEFAULT_READ_TIMEOUT);

        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }
}
