package com.ktb.redis;

import com.ktb.fixture.RedisFixture;
import com.ktb.redis.config.RedisCacheConfig;
import com.ktb.redis.config.RedisSerializationConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
@Import({
        RedisCacheConfig.class,
        RedisSerializationConfig.class
})
public class RedisCacheManagerTest extends AbstractRedisContainerTest{

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate redisCleaner;

    @BeforeEach
    void setUp() {
        this.stringRedisTemplate = redisCleaner;
        clearRedis();
    }

    @Test
    void cache_manager_should_store_and_get_value() {
        Cache cache = cacheManager.getCache("questionList");
        RedisFixture.RedisTestUser user = RedisFixture.createTestUser();

        cache.put("test:user:1", user);

        RedisFixture.RedisTestUser cached = cache.get("test:user:1", RedisFixture.RedisTestUser.class);

        assertThat(cached).isNotNull();
        assertThat(cached).isEqualTo(user);
    }
}
