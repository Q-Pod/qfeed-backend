package com.ktb.common.config;

import com.ktb.interview.session.domain.InterviewSession;
import com.ktb.interview.session.domain.InterviewSessionFeedback;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.ObjectMapper;

@Configuration
@Profile("redis")
@RequiredArgsConstructor
public class RedisConfig {

    private final ObjectMapper objectMapper;

    @Bean
    public RedisTemplate<String, InterviewSession> interviewSessionRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        var serializer = new JacksonJsonRedisSerializer<>(objectMapper, InterviewSession.class);

        RedisTemplate<String, InterviewSession> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        return template;
    }

    @Bean
    public RedisTemplate<String, InterviewSessionFeedback> interviewSessionFeedbackRedisTemplate(
            RedisConnectionFactory connectionFactory) {

        var serializer = new JacksonJsonRedisSerializer<>(objectMapper, InterviewSessionFeedback.class);

        RedisTemplate<String, InterviewSessionFeedback> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);
        template.setKeySerializer(new StringRedisSerializer());
        template.setValueSerializer(serializer);
        return template;
    }
}