package com.ktb.redis;

import com.ktb.fixture.RedisFixture;
import com.ktb.redis.config.RedisSerializationConfig;
import com.ktb.redis.config.RedisTemplateConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;


@DataRedisTest
@Import({
        RedisSerializationConfig.class,
        RedisTemplateConfig.class
})
public class RedisTemplateTest extends AbstractRedisContainerTest{


    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private StringRedisTemplate redisCleaner;

    @BeforeEach
    void setUp() {
        this.stringRedisTemplate = redisCleaner;
        clearRedis();
    }

    @Test
    void redis_template_should_set_and_get_object() {
        String key = "test:object:key";
        RedisFixture.RedisTestUser user = RedisFixture.createTestUser();

        redisTemplate.opsForValue().set(key, user, Duration.ofSeconds(30));

        Object result = redisTemplate.opsForValue().get(key);

        assertThat(result).isInstanceOf(RedisFixture.RedisTestUser.class);
        assertThat((RedisFixture.RedisTestUser)result).isEqualTo(user);
    }
}
