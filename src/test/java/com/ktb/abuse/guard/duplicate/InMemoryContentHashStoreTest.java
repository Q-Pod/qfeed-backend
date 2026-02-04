package com.ktb.abuse.guard.duplicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.ktb.abuse.store.inmemory.InMemoryContentHashStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryContentHashStore 단위 테스트")
class InMemoryContentHashStoreTest {

    private InMemoryContentHashStore store;

    @BeforeEach
    void setUp() {
        store = new InMemoryContentHashStore();
    }

    @Nested
    @DisplayName("키 격리 테스트")
    class KeyIsolationTest {

        @Test
        @DisplayName("같은 사용자 + 같은 질문 → 해시 존재 확인됨")
        void sameAccountSameQuestion_ShouldFindHash() {
            // Given
            Long accountId = 1L, questionId = 100L;
            String hash = "abc123", simHash = "sim123";
            store.storeHash(accountId, questionId, hash, simHash);

            // When & Then
            assertThat(store.existsHash(accountId, questionId, hash)).isTrue();
            assertThat(store.getRecentSimHashes(accountId, questionId, 10)).contains(simHash);
        }

        @Test
        @DisplayName("같은 사용자 + 다른 질문 → 서로 영향 없음")
        void sameAccountDifferentQuestion_ShouldBeIsolated() {
            // Given
            Long accountId = 1L;
            store.storeHash(accountId, 100L, "hash1", "sim1");

            // When & Then - 다른 질문에서는 찾을 수 없음
            assertThat(store.existsHash(accountId, 200L, "hash1")).isFalse();
            assertThat(store.getRecentSimHashes(accountId, 200L, 10)).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자 + 같은 질문 → 서로 영향 없음")
        void differentAccountSameQuestion_ShouldBeIsolated() {
            // Given
            Long questionId = 100L;
            store.storeHash(1L, questionId, "hash1", "sim1");

            // When & Then - 다른 사용자에게는 찾을 수 없음
            assertThat(store.existsHash(2L, questionId, "hash1")).isFalse();
            assertThat(store.getRecentSimHashes(2L, questionId, 10)).isEmpty();
        }
    }
}

