package com.ktb.abuse.store.inmemory;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
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
            Long accountId = 1L;
            Long questionId = 100L;
            String hash = "abc123";
            String simHash = "sim123";
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
            Long questionId1 = 100L;
            Long questionId2 = 200L;
            store.storeHash(accountId, questionId1, "hash1", "sim1");

            // When & Then - 다른 질문에서는 찾을 수 없음
            assertThat(store.existsHash(accountId, questionId2, "hash1")).isFalse();
            assertThat(store.getRecentSimHashes(accountId, questionId2, 10)).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자 + 같은 질문 → 서로 영향 없음")
        void differentAccountSameQuestion_ShouldBeIsolated() {
            // Given
            Long accountId1 = 1L;
            Long accountId2 = 2L;
            Long questionId = 100L;
            store.storeHash(accountId1, questionId, "hash1", "sim1");

            // When & Then - 다른 사용자에게는 찾을 수 없음
            assertThat(store.existsHash(accountId2, questionId, "hash1")).isFalse();
            assertThat(store.getRecentSimHashes(accountId2, questionId, 10)).isEmpty();
        }

        @Test
        @DisplayName("다른 사용자 + 다른 질문 → 완전히 독립된 저장소")
        void differentAccountDifferentQuestion_ShouldBeCompletelyIsolated() {
            // Given
            store.storeHash(1L, 100L, "hash1", "sim1");
            store.storeHash(2L, 200L, "hash2", "sim2");

            // When & Then
            assertThat(store.existsHash(1L, 100L, "hash1")).isTrue();
            assertThat(store.existsHash(2L, 200L, "hash2")).isTrue();
            assertThat(store.existsHash(1L, 100L, "hash2")).isFalse();
            assertThat(store.existsHash(2L, 200L, "hash1")).isFalse();
        }
    }

    @Nested
    @DisplayName("SimHash 조회 테스트")
    class SimHashRetrievalTest {

        @Test
        @DisplayName("최근 N개 SimHash만 조회됨")
        void getRecentSimHashes_ShouldReturnLimitedResults() {
            // Given
            Long accountId = 1L;
            Long questionId = 100L;
            for (int i = 1; i <= 10; i++) {
                store.storeHash(accountId, questionId, "hash" + i, "sim" + i);
            }

            // When - 최근 5개만 요청
            List<String> recentHashes = store.getRecentSimHashes(accountId, questionId, 5);

            // Then - 가장 최근 5개만 반환 (sim6 ~ sim10)
            assertThat(recentHashes).hasSize(5);
            assertThat(recentHashes).containsExactly("sim6", "sim7", "sim8", "sim9", "sim10");
        }

        @Test
        @DisplayName("저장된 해시가 limit보다 적으면 전체 반환")
        void getRecentSimHashes_WhenLessThanLimit_ShouldReturnAll() {
            // Given
            Long accountId = 1L;
            Long questionId = 100L;
            store.storeHash(accountId, questionId, "hash1", "sim1");
            store.storeHash(accountId, questionId, "hash2", "sim2");

            // When - 10개 요청했지만 2개만 존재
            List<String> recentHashes = store.getRecentSimHashes(accountId, questionId, 10);

            // Then
            assertThat(recentHashes).hasSize(2);
            assertThat(recentHashes).containsExactly("sim1", "sim2");
        }

        @Test
        @DisplayName("저장된 해시가 없으면 빈 리스트 반환")
        void getRecentSimHashes_WhenEmpty_ShouldReturnEmptyList() {
            // When
            List<String> recentHashes = store.getRecentSimHashes(1L, 100L, 10);

            // Then
            assertThat(recentHashes).isEmpty();
        }
    }

    @Nested
    @DisplayName("해시 존재 여부 테스트")
    class HashExistenceTest {

        @Test
        @DisplayName("존재하는 해시 조회 시 true 반환")
        void existsHash_WhenExists_ShouldReturnTrue() {
            // Given
            store.storeHash(1L, 100L, "existingHash", "sim1");

            // When & Then
            assertThat(store.existsHash(1L, 100L, "existingHash")).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 해시 조회 시 false 반환")
        void existsHash_WhenNotExists_ShouldReturnFalse() {
            // Given
            store.storeHash(1L, 100L, "existingHash", "sim1");

            // When & Then
            assertThat(store.existsHash(1L, 100L, "nonExistingHash")).isFalse();
        }

        @Test
        @DisplayName("동일 해시를 다른 키로 저장해도 독립적으로 관리됨")
        void existsHash_SameHashDifferentKey_ShouldBeIndependent() {
            // Given - 같은 해시값을 다른 accountId+questionId 조합에 저장
            String sameHash = "duplicateHash";
            store.storeHash(1L, 100L, sameHash, "sim1");
            store.storeHash(2L, 100L, sameHash, "sim2");

            // When & Then - 각각 독립적으로 존재
            assertThat(store.existsHash(1L, 100L, sameHash)).isTrue();
            assertThat(store.existsHash(2L, 100L, sameHash)).isTrue();
            assertThat(store.existsHash(3L, 100L, sameHash)).isFalse();
        }
    }

    @Nested
    @DisplayName("여러 답변 저장 시나리오")
    class MultipleAnswerScenarioTest {

        @Test
        @DisplayName("같은 사용자가 같은 질문에 여러 답변 저장 시 모두 추적됨")
        void sameUserSameQuestion_MultipleAnswers_ShouldTrackAll() {
            // Given
            Long accountId = 1L;
            Long questionId = 100L;

            // 사용자가 같은 질문에 3번 답변
            store.storeHash(accountId, questionId, "answer1Hash", "answer1Sim");
            store.storeHash(accountId, questionId, "answer2Hash", "answer2Sim");
            store.storeHash(accountId, questionId, "answer3Hash", "answer3Sim");

            // When
            List<String> simHashes = store.getRecentSimHashes(accountId, questionId, 10);

            // Then - 모든 답변이 추적됨
            assertThat(simHashes).hasSize(3);
            assertThat(simHashes).containsExactly("answer1Sim", "answer2Sim", "answer3Sim");
            assertThat(store.existsHash(accountId, questionId, "answer1Hash")).isTrue();
            assertThat(store.existsHash(accountId, questionId, "answer2Hash")).isTrue();
            assertThat(store.existsHash(accountId, questionId, "answer3Hash")).isTrue();
        }

        @Test
        @DisplayName("같은 사용자가 다른 질문들에 답변 시 질문별로 독립 관리")
        void sameUserDifferentQuestions_ShouldManageIndependently() {
            // Given
            Long accountId = 1L;

            // 사용자가 여러 질문에 답변
            store.storeHash(accountId, 100L, "q100Hash", "q100Sim");
            store.storeHash(accountId, 200L, "q200Hash", "q200Sim");
            store.storeHash(accountId, 300L, "q300Hash", "q300Sim");

            // When & Then - 각 질문별로 독립적으로 조회됨
            assertThat(store.getRecentSimHashes(accountId, 100L, 10))
                    .containsExactly("q100Sim");
            assertThat(store.getRecentSimHashes(accountId, 200L, 10))
                    .containsExactly("q200Sim");
            assertThat(store.getRecentSimHashes(accountId, 300L, 10))
                    .containsExactly("q300Sim");
        }

        @Test
        @DisplayName("여러 사용자가 같은 질문에 답변 시 사용자별로 독립 관리")
        void differentUsersSameQuestion_ShouldManageIndependently() {
            // Given
            Long questionId = 100L;

            // 여러 사용자가 같은 질문에 답변
            store.storeHash(1L, questionId, "user1Hash", "user1Sim");
            store.storeHash(2L, questionId, "user2Hash", "user2Sim");
            store.storeHash(3L, questionId, "user3Hash", "user3Sim");

            // When & Then - 각 사용자별로 독립적으로 조회됨
            assertThat(store.getRecentSimHashes(1L, questionId, 10))
                    .containsExactly("user1Sim");
            assertThat(store.getRecentSimHashes(2L, questionId, 10))
                    .containsExactly("user2Sim");
            assertThat(store.getRecentSimHashes(3L, questionId, 10))
                    .containsExactly("user3Sim");
        }
    }
}
