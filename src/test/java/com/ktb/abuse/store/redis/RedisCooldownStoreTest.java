package com.ktb.abuse.store.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisCooldownStore 단위 테스트")
class RedisCooldownStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisCooldownStore store;

    @Nested
    @DisplayName("setLastSubmitTime / getLastSubmitTime 테스트")
    class LastSubmitTimeTest {

        @Test
        @DisplayName("setLastSubmitTime — TTL과 함께 올바른 key로 저장됨")
        void setLastSubmitTime_ShouldStoreWithTtl() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            long timestamp = 1700000000L;

            // When
            store.setLastSubmitTime(1L, 100L, timestamp);

            // Then
            verify(valueOperations).set(
                eq("abuse:cooldown:last:1:100"),
                eq("1700000000"),
                eq(86400L),
                eq(TimeUnit.SECONDS)
            );
        }

        @Test
        @DisplayName("getLastSubmitTime — 값이 있으면 Optional.of(Long) 반환")
        void getLastSubmitTime_WhenValueExists_ShouldReturnOptionalLong() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("abuse:cooldown:last:1:100")).thenReturn("1700000000");

            // When
            Optional<Long> result = store.getLastSubmitTime(1L, 100L);

            // Then
            assertThat(result).isPresent().contains(1700000000L);
        }

        @Test
        @DisplayName("getLastSubmitTime — 값이 없으면 Optional.empty() 반환")
        void getLastSubmitTime_WhenNoValue_ShouldReturnEmpty() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("abuse:cooldown:last:1:100")).thenReturn(null);

            // When
            Optional<Long> result = store.getLastSubmitTime(1L, 100L);

            // Then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("incrementConsecutiveCount 테스트")
    class IncrementConsecutiveCountTest {

        @Test
        @DisplayName("Lua 스크립트로 consecKey에 INCR+EXPIRE 실행됨")
        void incrementConsecutiveCount_ShouldExecuteLuaScript() {
            // Given
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(1L);

            // When
            store.incrementConsecutiveCount(1L, 100L);

            // Then
            verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(java.util.List.of("abuse:cooldown:consec:1:100")),
                eq("86400")
            );
        }
    }

    @Nested
    @DisplayName("getConsecutiveCount 테스트")
    class GetConsecutiveCountTest {

        @Test
        @DisplayName("값이 있으면 정수로 반환")
        void getConsecutiveCount_WhenValueExists_ShouldReturnInt() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("abuse:cooldown:consec:1:100")).thenReturn("4");

            // When
            int result = store.getConsecutiveCount(1L, 100L);

            // Then
            assertThat(result).isEqualTo(4);
        }

        @Test
        @DisplayName("값이 없으면 0 반환")
        void getConsecutiveCount_WhenNoValue_ShouldReturnZero() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("abuse:cooldown:consec:1:100")).thenReturn(null);

            // When
            int result = store.getConsecutiveCount(1L, 100L);

            // Then
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("resetConsecutiveCount 테스트")
    class ResetConsecutiveCountTest {

        @Test
        @DisplayName("consecKey를 삭제함")
        void resetConsecutiveCount_ShouldDeleteConsecKey() {
            // When
            store.resetConsecutiveCount(1L, 100L);

            // Then
            verify(redisTemplate).delete("abuse:cooldown:consec:1:100");
        }
    }
}
