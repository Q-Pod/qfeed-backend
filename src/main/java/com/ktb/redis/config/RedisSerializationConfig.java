package com.ktb.redis.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.databind.jsontype.PolymorphicTypeValidator;

@Configuration
public class RedisSerializationConfig {

    /**
     *  RedisTemplate에서 key 직렬화에 대하여 사용
     */
    @Bean
    public StringRedisSerializer redisKeySerializer() {
        return new StringRedisSerializer();
    }

    /**
     *  RedisTemplate에서 value 직렬화에 대하여 사용
     */
    @Bean
    public RedisSerializer<Object> redisValueSerializer() {
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
                .allowIfSubType(Object.class)
                .build();

        return GenericJacksonJsonRedisSerializer.builder()
                .typeValidator(ptv)
                .enableDefaultTyping(ptv)
                .build();
    }

    /**
     *  RedisCacheConfiguration에서 key 직렬화 정책으로 사용
     */
    @Bean
    public RedisSerializationContext.SerializationPair<String> redisKeySerializationPair(StringRedisSerializer redisKeySerializer) {
        return RedisSerializationContext.SerializationPair.fromSerializer(redisKeySerializer);
    }

    /**
     *  RedisCacheConfiguration에서 value 직렬화 정책으로 사용
     */
    @Bean
    public RedisSerializationContext.SerializationPair<Object> redisValueSerializationPair(RedisSerializer<Object> redisValueSerializer) {
        return RedisSerializationContext.SerializationPair.fromSerializer(redisValueSerializer);
    }
}
