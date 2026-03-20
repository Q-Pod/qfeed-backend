package com.ktb.abuse.store.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @InjectMocks
    private RedisQuotaStore store;

    @Nested
    @DisplayName("dailyKey — 고정 ZoneId 검증")
    class DailyKeyZoneTest {

        @Test
        @DisplayName("JVM 기본 타임존이 UTC여도 Asia/Seoul 기준 날짜가 key에 사용됨")
        void getDailyQuota_WithUtcDefaultZone_ShouldUseSeoulDate() {
            // Given — JVM 기본 타임존을 UTC로 변경
            java.util.TimeZone originalDefault = java.util.TimeZone.getDefault();
            try {
                java.util.TimeZone.setDefault(java.util.TimeZone.getTimeZone("UTC"));

                String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
                String expectedKey = "abuse:quota:1:" + seoulDate;

                when(redisTemplate.opsForValue()).thenReturn(valueOperations);
                when(valueOperations.get(expectedKey)).thenReturn("3");

                // When
                int quota = store.getDailyQuota(1L);

                // Then — Seoul 기준 날짜 key로 조회됨
                assertThat(quota).isEqualTo(3);
            } finally {
                java.util.TimeZone.setDefault(originalDefault);
            }
        }

        @Test
        @DisplayName("getDailyQuota key는 'abuse:quota:{accountId}:{yyyyMMdd(Seoul)}' 형식")
        void getDailyQuota_ShouldUseCorrectKeyFormat() {
            // Given
            Long accountId = 42L;
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            String expectedKey = "abuse:quota:" + accountId + ":" + seoulDate;

            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get(expectedKey)).thenReturn("5");

            // When
            int result = store.getDailyQuota(accountId);

            // Then
            assertThat(result).isEqualTo(5);
        }
    }

    @Nested
    @DisplayName("getDailyQuota 회귀 테스트")
    class GetDailyQuotaTest {

        @Test
        @DisplayName("Redis에 값이 있으면 해당 정수 반환")
        void getDailyQuota_WhenValueExists_ShouldReturnParsedInt() {
            // Given
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("abuse:quota:1:" + seoulDate)).thenReturn("7");

            // When
            int result = store.getDailyQuota(1L);

            // Then
            assertThat(result).isEqualTo(7);
        }

        @Test
        @DisplayName("Redis에 값이 없으면 0 반환")
        void getDailyQuota_WhenNoValue_ShouldReturnZero() {
            // Given
            String seoulDate = LocalDate.now(SEOUL_ZONE).format(DATE_FORMAT);
            when(redisTemplate.opsForValue()).thenReturn(valueOperations);
            when(valueOperations.get("abuse:quota:1:" + seoulDate)).thenReturn(null);

            // When
            int result = store.getDailyQuota(1L);

            // Then
            assertThat(result).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("incrementDailyQuota 회귀 테스트")
    class IncrementDailyQuotaTest {

        @Test
        @DisplayName("Lua 스크립트 실행 결과가 null이면 1 반환")
        void incrementDailyQuota_WhenScriptReturnsNull_ShouldReturnOne() {
            // Given
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(null);

            // When
            int result = store.incrementDailyQuota(1L);

            // Then
            assertThat(result).isEqualTo(1);
        }

        @Test
        @DisplayName("Lua 스크립트 실행 결과가 3이면 3 반환")
        void incrementDailyQuota_WhenScriptReturnsThree_ShouldReturnThree() {
            // Given
            when(redisTemplate.execute(
                ArgumentMatchers.<RedisScript<Long>>any(),
                anyList(),
                anyString()
            )).thenReturn(3L);

            // When
            int result = store.incrementDailyQuota(1L);

            // Then
            assertThat(result).isEqualTo(3);
        }
    }
}
