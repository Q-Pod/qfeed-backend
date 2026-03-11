package com.ktb.redis.config;

import com.ktb.redis.policy.CachePolicy;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializationContext;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    /**
     *
     * Default 캐시 정책
     */
    @Bean
    public RedisCacheConfiguration defaultRedisCacheConfiguration(
            RedisSerializationContext.SerializationPair<String> redisKeySerializer,
            RedisSerializationContext.SerializationPair<Object> redisValueSerializer
    ) {
        return RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(redisKeySerializer)
                .serializeValuesWith(redisValueSerializer)
                .entryTtl(Duration.ofMinutes(10))
                .disableCachingNullValues();
    }

    /**
     * Enum 기반 개별 캐시 정책
     */
    @Bean
    public Map<String, RedisCacheConfiguration> redisCacheConfigurations(RedisCacheConfiguration defaultRedisCacheConfiguration) {
        return Arrays.stream(CachePolicy.values())
                .collect(Collectors.toMap(
                        CachePolicy::getCacheName,
                        policy -> defaultRedisCacheConfiguration.entryTtl(policy.getTtl())
                ));
    }

    /**
     * 실제 Redis CacheManager
     */
    @Bean
    public RedisCacheManager redisCacheManager(
            RedisConnectionFactory connectionFactory,
            RedisCacheConfiguration defaultRedisCacheConfiguration,
            Map<String, RedisCacheConfiguration> redisCacheConfigurations
    ) {
        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultRedisCacheConfiguration)
                .withInitialCacheConfigurations(redisCacheConfigurations)
                .transactionAware()
                .build();
    }
}
