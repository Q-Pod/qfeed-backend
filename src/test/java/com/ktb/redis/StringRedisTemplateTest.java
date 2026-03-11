package com.ktb.redis;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
public class StringRedisTemplateTest extends AbstractRedisContainerTest{

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void setUp() {
        this.stringRedisTemplate = redisTemplate;
        clearRedis();
    }

    @Test
    void string_redis_template_should_set_and_get_value() {
        String key = "test:string:key";
        String value = "hello redis";

        stringRedisTemplate.opsForValue().set(key, value, Duration.ofSeconds(30));

        String result = stringRedisTemplate.opsForValue().get(key);

        assertThat(result).isEqualTo(value);
    }
}
