package com.ktb.abuse.store.redis;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;

@ExtendWith(MockitoExtension.class)
@DisplayName("RedisContentHashStore 단위 테스트")
class RedisContentHashStoreTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;

    @InjectMocks
    private RedisContentHashStore store;

    @Nested
    @DisplayName("getRecentSimHashes — limit 경계값 테스트")
    class GetRecentSimHashesLimitTest {

        @Test
        @DisplayName("limit = 0이면 emptyList 반환, range() 호출 없음")
        void getRecentSimHashes_WhenLimitIsZero_ShouldReturnEmptyListWithoutRedisCall() {
            // When
            List<String> result = store.getRecentSimHashes(1L, 100L, 0);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("limit = -1이면 emptyList 반환, range() 호출 없음")
        void getRecentSimHashes_WhenLimitIsNegative_ShouldReturnEmptyListWithoutRedisCall() {
            // When
            List<String> result = store.getRecentSimHashes(1L, 100L, -1);

            // Then
            assertThat(result).isEmpty();
            verifyNoInteractions(redisTemplate);
        }

        @Test
        @DisplayName("limit = 3이면 range(key, 0, 2) 호출하고 결과 반환")
        void getRecentSimHashes_WhenLimitIsThree_ShouldCallRangeWithCorrectArgs() {
            // Given
            String expectedKey = "abuse:hash:sim:1:100";
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
        @DisplayName("range() 가 null 반환 시 emptyList 반환")
        void getRecentSimHashes_WhenRangeReturnsNull_ShouldReturnEmptyList() {
            // Given
            when(redisTemplate.opsForList()).thenReturn(listOperations);
            when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(null);

            // When
            List<String> result = store.getRecentSimHashes(1L, 100L, 5);

            // Then
            assertThat(result).isEmpty();
        }
    }
}
