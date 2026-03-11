package com.ktb.fixture;

public class RedisFixture {

    public record RedisTestUser(Long id, String name) {}

    public static RedisTestUser createTestUser() {
        return new RedisTestUser(1L, "name");
    }
}
