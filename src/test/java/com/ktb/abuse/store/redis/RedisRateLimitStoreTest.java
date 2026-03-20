package com.ktb.abuse.store.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.Optional;
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
@DisplayName("RedisRateLimitStore 단위 테스트")
class RedisRateLimitStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisRateLimitStore store;

    @Nested
    @DisplayName("incrementAndGet 테스트")
    class IncrementAndGetTest {

        @Test
        @DisplayName("Lua 스크립트로 'abuse:rate:{key}'에 INCR+PEXPIRE 실행되고 결과 반환")
        void incrementAndGet_ShouldExecuteLuaScriptWithPrefixedKey() {
            // Given
            Duration window = Duration.ofSeconds(60);
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(3L);

            // When
            long result = store.incrementAndGet("account:1", window);

            // Then
            assertThat(result).isEqualTo(3L);
            verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                eq(java.util.List.of("abuse:rate:account:1")),
                eq("60000")
            );
        }

        @Test
        @DisplayName("Lua 스크립트가 null 반환 시 1L 반환")
        void incrementAndGet_WhenScriptReturnsNull_ShouldReturnOne() {
            // Given
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(null);

            // When
            long result = store.incrementAndGet("account:1", Duration.ofSeconds(10));

            // Then
            assertThat(result).isEqualTo(1L);
        }

        @Test
        @DisplayName("window 크기가 밀리초로 변환되어 전달됨")
        void incrementAndGet_ShouldConvertWindowToMillis() {
            // Given
            Duration window = Duration.ofMinutes(1);
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(1L);

            // When
            store.incrementAndGet("some:key", window);

            // Then — 60초 = 60000ms
            verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                eq("60000")
            );
        }
    }

    @Nested
    @DisplayName("get 테스트")
    class GetTest {

        @Test
        @DisplayName("값이 있으면 Optional.of(Long) 반환")
        void get_WhenValueExists_ShouldReturnOptionalLong() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("abuse:rate:account:1")).thenReturn("5");

            // When
            Optional<Long> result = store.get("account:1");

            // Then
            assertThat(result).isPresent().contains(5L);
        }

        @Test
        @DisplayName("값이 없으면 Optional.empty() 반환")
        void get_WhenNoValue_ShouldReturnEmpty() {
            // Given
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("abuse:rate:account:1")).thenReturn(null);

            // When
            Optional<Long> result = store.get("account:1");

            // Then
            assertThat(result).isEmpty();
        }
    }
}
