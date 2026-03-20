package com.ktb.abuse.store.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisContentHashStore 단위 테스트")
class RedisContentHashStoreTest {

    private static final String EXACT_KEY_PREFIX = "abuse:hash:exact:";
    private static final String SIM_KEY_PREFIX = "abuse:hash:sim:";
    private static final long HASH_TTL_SECONDS = 7L * 24 * 60 * 60; // 604800
    private static final long SIM_HASH_MAX_SIZE = 100L;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private RedisContentHashStore store;

    @Nested
    @DisplayName("storeHash 테스트")
    class StoreHashTest {

        @Test
        @DisplayName("exactKey(Set)에 hash를 추가하고 TTL을 설정함")
        void storeHash_ShouldAddHashToExactSetWithTtl() {
            // Given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(redisTemplate.opsForList()).thenReturn(listOperations);

            // When
            store.storeHash(1L, 100L, "abc123", "sim123");

            // Then
            verify(setOperations).add(EXACT_KEY_PREFIX + "1:100", "abc123");
            verify(redisTemplate).expire(EXACT_KEY_PREFIX + "1:100", HASH_TTL_SECONDS, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("simKey(List)에 simHash를 leftPush하고 최대 100개로 trim 후 TTL 설정")
        void storeHash_ShouldLeftPushSimHashWithTrimAndTtl() {
            // Given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(redisTemplate.opsForList()).thenReturn(listOperations);

            // When
            store.storeHash(1L, 100L, "abc123", "sim123");

            // Then
            verify(listOperations).leftPush(SIM_KEY_PREFIX + "1:100", "sim123");
            verify(listOperations).trim(SIM_KEY_PREFIX + "1:100", 0, SIM_HASH_MAX_SIZE - 1);
            verify(redisTemplate).expire(SIM_KEY_PREFIX + "1:100", HASH_TTL_SECONDS, TimeUnit.SECONDS);
        }

        @Test
        @DisplayName("accountId·questionId가 다르면 서로 다른 key에 저장됨")
        void storeHash_DifferentAccountAndQuestion_ShouldUseDifferentKeys() {
            // Given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(redisTemplate.opsForList()).thenReturn(listOperations);

            // When
            store.storeHash(2L, 200L, "hashX", "simX");

            // Then — key가 계정·질문 조합으로 격리됨
            verify(setOperations).add(EXACT_KEY_PREFIX + "2:200", "hashX");
            verify(listOperations).leftPush(SIM_KEY_PREFIX + "2:200", "simX");
        }
    }

    @Nested
    @DisplayName("existsHash 테스트")
    class ExistsHashTest {

        @Test
        @DisplayName("isMember 가 TRUE 반환 → true")
        void existsHash_WhenIsMemberReturnsTrue_ShouldReturnTrue() {
            // Given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember(EXACT_KEY_PREFIX + "1:100", "abc123")).thenReturn(Boolean.TRUE);

            // When & Then
            assertThat(store.existsHash(1L, 100L, "abc123")).isTrue();
        }

        @Test
        @DisplayName("isMember 가 FALSE 반환 → false")
        void existsHash_WhenIsMemberReturnsFalse_ShouldReturnFalse() {
            // Given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember(EXACT_KEY_PREFIX + "1:100", "abc123")).thenReturn(Boolean.FALSE);

            // When & Then
            assertThat(store.existsHash(1L, 100L, "abc123")).isFalse();
        }

        @Test
        @DisplayName("isMember 가 null 반환 → false (Boolean.TRUE.equals(null) 안전)")
        void existsHash_WhenIsMemberReturnsNull_ShouldReturnFalse() {
            // Given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember(EXACT_KEY_PREFIX + "1:100", "abc123")).thenReturn(null);

            // When & Then
            assertThat(store.existsHash(1L, 100L, "abc123")).isFalse();
        }

        @Test
        @DisplayName("exactKey 형식은 'abuse:hash:exact:{accountId}:{questionId}'")
        void existsHash_ShouldUseCorrectExactKeyFormat() {
            // Given
            when(redisTemplate.opsForSet()).thenReturn(setOperations);
            when(setOperations.isMember(EXACT_KEY_PREFIX + "42:999", "h")).thenReturn(Boolean.TRUE);

            // When
            boolean result = store.existsHash(42L, 999L, "h");

            // Then
            assertThat(result).isTrue();
            verify(setOperations).isMember(EXACT_KEY_PREFIX + "42:999", "h");
        }
    }

    @Nested
    @DisplayName("getRecentSimHashes — limit 경계값 테스트")
    class GetRecentSimHashesLimitTest {

        @Test
        @DisplayName("limit = 0이면 emptyList 반환, Redis 호출 없음")
        void getRecentSimHashes_WhenLimitIsZero_ShouldReturnEmptyListWithoutRedisCall() {
            List<String> result = store.getRecentSimHashes(1L, 100L, 0);

            assertThat(result).isEmpty();
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("limit = -1이면 emptyList 반환, Redis 호출 없음")
        void getRecentSimHashes_WhenLimitIsNegative_ShouldReturnEmptyListWithoutRedisCall() {
            List<String> result = store.getRecentSimHashes(1L, 100L, -1);

            assertThat(result).isEmpty();
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("limit = 1이면 range(key, 0, 0) 호출")
        void getRecentSimHashes_WhenLimitIsOne_ShouldCallRangeZeroToZero() {
            // Given
            when(redisTemplate.opsForList()).thenReturn(listOperations);
            when(listOperations.range(SIM_KEY_PREFIX + "1:100", 0, 0)).thenReturn(List.of("sim1"));

            // When
            List<String> result = store.getRecentSimHashes(1L, 100L, 1);

            // Then
            assertThat(result).containsExactly("sim1");
            verify(listOperations).range(SIM_KEY_PREFIX + "1:100", 0, 0);
        }

        @Test
        @DisplayName("limit = 3이면 range(key, 0, 2) 호출하고 결과 그대로 반환")
        void getRecentSimHashes_WhenLimitIsThree_ShouldCallRangeWithCorrectEndIndex() {
            // Given
            String expectedKey = SIM_KEY_PREFIX + "1:100";
            List<String> redisResult = List.of("sim1", "sim2", "sim3");
            when(redisTemplate.opsForList()).thenReturn(listOperations);
            when(listOperations.range(expectedKey, 0, 2)).thenReturn(redisResult);

            // When
            List<String> result = store.getRecentSimHashes(1L, 100L, 3);

            // Then
            assertThat(result).containsExactly("sim1", "sim2", "sim3");
            verify(listOperations).range(expectedKey, 0, 2);
        }

        @Test
        @DisplayName("range() 가 null 반환 시 emptyList 반환 (null 방어)")
        void getRecentSimHashes_WhenRangeReturnsNull_ShouldReturnEmptyList() {
            // Given
            when(redisTemplate.opsForList()).thenReturn(listOperations);
            when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);

            // When
            List<String> result = store.getRecentSimHashes(1L, 100L, 5);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("range() 가 빈 리스트 반환 시 빈 리스트 그대로 반환")
        void getRecentSimHashes_WhenRangeReturnsEmptyList_ShouldReturnEmptyList() {
            // Given
            when(redisTemplate.opsForList()).thenReturn(listOperations);
            when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(List.of());

            // When
            List<String> result = store.getRecentSimHashes(1L, 100L, 5);

            // Then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("simKey 형식은 'abuse:hash:sim:{accountId}:{questionId}'")
        void getRecentSimHashes_ShouldUseCorrectSimKeyFormat() {
            // Given
            String expectedKey = SIM_KEY_PREFIX + "7:300";
            when(redisTemplate.opsForList()).thenReturn(listOperations);
            when(listOperations.range(expectedKey, 0, 4)).thenReturn(List.of());

            // When
            store.getRecentSimHashes(7L, 300L, 5);

            // Then
            verify(listOperations).range(expectedKey, 0, 4);
        }
    }
}
