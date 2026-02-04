package com.ktb.abuse.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SimHashCalculator 단위 테스트")
class SimHashCalculatorTest {

    @Nested
    @DisplayName("calculateSha256")
    class CalculateSha256 {

        @Test
        @DisplayName("동일한 입력은 동일한 해시를 반환한다")
        void sameInput_SameHash() {
            String content = "테스트 텍스트입니다";
            String hash1 = SimHashCalculator.calculateSha256(content);
            String hash2 = SimHashCalculator.calculateSha256(content);
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("다른 입력은 다른 해시를 반환한다")
        void differentInput_DifferentHash() {
            String hash1 = SimHashCalculator.calculateSha256("텍스트1");
            String hash2 = SimHashCalculator.calculateSha256("텍스트2");
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("해시 길이는 64자이다 (SHA-256)")
        void hashLength_Is64() {
            String hash = SimHashCalculator.calculateSha256("테스트");
            assertThat(hash).hasSize(64);
        }
    }

    @Nested
    @DisplayName("calculateSimHash")
    class CalculateSimHash {

        @Test
        @DisplayName("동일한 입력은 동일한 SimHash를 반환한다")
        void sameInput_SameSimHash() {
            String content = "이것은 테스트 문장입니다";
            String hash1 = SimHashCalculator.calculateSimHash(content);
            String hash2 = SimHashCalculator.calculateSimHash(content);
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("유사한 입력은 유사한 SimHash를 반환한다")
        void similarInput_SimilarSimHash() {
            String content1 = "이것은 테스트 문장입니다";
            String content2 = "이것은 테스트 문장이에요";

            String hash1 = SimHashCalculator.calculateSimHash(content1);
            String hash2 = SimHashCalculator.calculateSimHash(content2);

            double similarity = SimHashCalculator.calculateSimilarity(hash1, hash2);
            assertThat(similarity).isGreaterThan(0.5);  // 유사도가 50% 이상
        }

        @Test
        @DisplayName("완전히 다른 입력은 낮은 유사도를 가진다")
        void differentInput_LowSimilarity() {
            String content1 = "자바 프로그래밍 언어";
            String content2 = "맛있는 김치찌개 레시피";

            String hash1 = SimHashCalculator.calculateSimHash(content1);
            String hash2 = SimHashCalculator.calculateSimHash(content2);

            double similarity = SimHashCalculator.calculateSimilarity(hash1, hash2);
            assertThat(similarity).isLessThan(0.9);  // 유사도가 90% 미만
        }

        @Test
        @DisplayName("빈 문자열은 0 해시를 반환한다")
        void emptyInput_ReturnsZeroHash() {
            String hash = SimHashCalculator.calculateSimHash("");
            assertThat(hash).isEqualTo("0".repeat(16));
        }

        @Test
        @DisplayName("null 입력은 0 해시를 반환한다")
        void nullInput_ReturnsZeroHash() {
            String hash = SimHashCalculator.calculateSimHash(null);
            assertThat(hash).isEqualTo("0".repeat(16));
        }
    }

    @Nested
    @DisplayName("calculateSimilarity")
    class CalculateSimilarity {

        @Test
        @DisplayName("동일한 해시는 1.0 유사도를 반환한다")
        void sameHash_ReturnsOne() {
            String hash = SimHashCalculator.calculateSimHash("테스트");
            double similarity = SimHashCalculator.calculateSimilarity(hash, hash);
            assertThat(similarity).isEqualTo(1.0);
        }

        @Test
        @DisplayName("null 해시는 0.0 유사도를 반환한다")
        void nullHash_ReturnsZero() {
            String hash = SimHashCalculator.calculateSimHash("테스트");
            double similarity = SimHashCalculator.calculateSimilarity(hash, null);
            assertThat(similarity).isEqualTo(0.0);
        }

        @Test
        @DisplayName("유사도는 0과 1 사이이다")
        void similarity_IsBetweenZeroAndOne() {
            String hash1 = SimHashCalculator.calculateSimHash("테스트1");
            String hash2 = SimHashCalculator.calculateSimHash("테스트2");
            double similarity = SimHashCalculator.calculateSimilarity(hash1, hash2);
            assertThat(similarity).isBetween(0.0, 1.0);
        }
    }
}
