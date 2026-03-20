package com.ktb.abuse.store.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
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
@DisplayName("RedisQuotaStore 단위 테스트")
class RedisQuotaStoreTest {

    private static final ZoneId SEOUL_ZONE = ZoneId.of("Asia/Seoul");
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final String KEY_PREFIX = "abuse:quota:";
    private static final long QUOTA_TTL_SECONDS = 48L * 60 * 60; // 172800

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisQuotaStore store;

    @Nested
    @DisplayName("dailyKey — ZoneId 및 key 형식 검증")
    class DailyKeyTest {

        @Test
        @DisplayName("key 형식은 'abuse:quota:{accountId}:{yyyyMMdd(Seoul)}'")
        void dailyKey_ShouldUseCorrectKeyFormat() {
            // Given
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            String expectedKey = KEY_PREFIX + "42:" + seoulDate;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(expectedKey)).thenReturn("5");

            // When
            int result = store.getDailyQuota(42L);

            // Then
            assertThat(result).isEqualTo(5);
            verify(valueOperations).get(expectedKey);
        }

        @Test
        @DisplayName("JVM 기본 타임존이 UTC여도 Asia/Seoul 기준 날짜가 key에 사용됨")
        void dailyKey_WithJvmTimezoneUtc_ShouldStillUseSeoulDate() {
            // Given — JVM 기본 타임존을 UTC로 교체
            java.util.TimeZone originalDefault = java.util.TimeZone.getDefault();
            try {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

                String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
                String expectedKey = KEY_PREFIX + "1:" + seoulDate;
                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get(expectedKey)).thenReturn("3");

                // When
                int quota = store.getDailyQuota(1L);

                // Then — Seoul 날짜 기준으로 조회됨
                assertThat(quota).isEqualTo(3);
                verify(valueOperations).get(expectedKey);
            } finally {
                java.util.TimeZone.setDefault(originalDefault);
            }
        }

        @Test
        @DisplayName("accountId가 다르면 서로 다른 dailyKey가 사용됨 (key 격리)")
        void dailyKey_DifferentAccountIds_ShouldUseDifferentKeys() {
            // Given
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            String keyFor1 = KEY_PREFIX + "1:" + seoulDate;
            String keyFor2 = KEY_PREFIX + "2:" + seoulDate;
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(keyFor1)).thenReturn("3");
            when(valueOperations.get(keyFor2)).thenReturn("7");

            // When
            int quotaFor1 = store.getDailyQuota(1L);
            int quotaFor2 = store.getDailyQuota(2L);

            // Then
            assertThat(quotaFor1).isEqualTo(3);
            assertThat(quotaFor2).isEqualTo(7);
        }
    }

    @Nested
    @DisplayName("getDailyQuota 테스트")
    class GetDailyQuotaTest {

        @Test
        @DisplayName("Redis에 값이 있으면 정수로 파싱하여 반환")
        void getDailyQuota_WhenValueExists_ShouldReturnParsedInt() {
            // Given
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(KEY_PREFIX + "1:" + seoulDate)).thenReturn("7");

            // When & Then
            assertThat(store.getDailyQuota(1L)).isEqualTo(7);
        }

        @Test
        @DisplayName("Redis에 값이 없으면 0 반환")
        void getDailyQuota_WhenNoValue_ShouldReturnZero() {
            // Given
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(KEY_PREFIX + "1:" + seoulDate)).thenReturn(null);

            // When & Then
            assertThat(store.getDailyQuota(1L)).isEqualTo(0);
        }

        @Test
        @DisplayName("quota 값이 1이면 1 반환 (최솟값 경계)")
        void getDailyQuota_WhenValueIsOne_ShouldReturnOne() {
            // Given
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(KEY_PREFIX + "1:" + seoulDate)).thenReturn("1");

            // When & Then
            assertThat(store.getDailyQuota(1L)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("incrementDailyQuota 테스트")
    class IncrementDailyQuotaTest {

        @Test
        @DisplayName("Lua 스크립트 호출 시 올바른 dailyKey가 전달됨")
        void incrementDailyQuota_ShouldPassCorrectKeyToScript() {
            // Given
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            String expectedKey = KEY_PREFIX + "1:" + seoulDate;
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(1L);

            // When
            store.incrementDailyQuota(1L);

            // Then — key 리스트로 정확한 dailyKey 전달
            verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                org.mockito.ArgumentMatchers.eq(List.of(expectedKey)),
                anyString()
            );
        }

        @Test
        @DisplayName("Lua 스크립트 호출 시 TTL 값 '172800'이 전달됨")
        void incrementDailyQuota_ShouldPassCorrectTtlToScript() {
            // Given
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(1L);

            // When
            store.incrementDailyQuota(1L);

            // Then — QUOTA_TTL_SECONDS = 48 * 3600 = 172800
            verify(redisTemplate).execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                org.mockito.ArgumentMatchers.eq(String.valueOf(QUOTA_TTL_SECONDS))
            );
        }

        @Test
        @DisplayName("스크립트 결과가 null이면 1 반환")
        void incrementDailyQuota_WhenScriptReturnsNull_ShouldReturnOne() {
            // Given
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(null);

            // When & Then
            assertThat(store.incrementDailyQuota(1L)).isEqualTo(1);
        }

        @Test
        @DisplayName("스크립트 결과가 3이면 3 반환")
        void incrementDailyQuota_WhenScriptReturnsThree_ShouldReturnThree() {
            // Given
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(3L);

            // When & Then
            assertThat(store.incrementDailyQuota(1L)).isEqualTo(3);
        }

        @Test
        @DisplayName("첫 번째 호출(v==1)이면 1 반환 (일별 첫 사용)")
        void incrementDailyQuota_FirstCallOfDay_ShouldReturnOne() {
            // Given
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(1L);

            // When & Then
            assertThat(store.incrementDailyQuota(1L)).isEqualTo(1);
        }
    }
}
