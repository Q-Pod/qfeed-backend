package com.ktb.redis;


import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.redis.test.autoconfigure.DataRedisTest;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;

@DataRedisTest
public class RedisConnectionTest extends AbstractRedisContainerTest{

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Test
    void redis_connection_should_work() {
        try (RedisConnection connection = redisConnectionFactory.getConnection()) {
            String pong = connection.ping();
            assertThat(pong).isEqualTo("PONG");
        }
    }
}
