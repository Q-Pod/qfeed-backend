package com.ktb.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 기본 Jackson ObjectMapper 빈 설정.
 * 애플리케이션 컨텍스트에 ObjectMapper가 없을 때만 등록합니다.
 */
@Configuration
public class JacksonConfig {

    /**
     * JSON 직렬화/역직렬화 공통 ObjectMapper를 제공합니다.
     */
    @Bean
    @ConditionalOnMissingBean(ObjectMapper.class)
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }
}
