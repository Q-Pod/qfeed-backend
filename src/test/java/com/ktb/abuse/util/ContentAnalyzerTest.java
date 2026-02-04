package com.ktb.abuse.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("ContentAnalyzer 단위 테스트")
class ContentAnalyzerTest {

    @Nested
    @DisplayName("calculateWhitespaceRatio")
    class CalculateWhitespaceRatio {

        @Test
        @DisplayName("공백이 없는 텍스트는 0을 반환한다")
        void noWhitespace_ReturnsZero() {
            String content = "안녕하세요반갑습니다";
            double ratio = ContentAnalyzer.calculateWhitespaceRatio(content);
            assertThat(ratio).isEqualTo(0.0);
        }

        @Test
        @DisplayName("공백만 있는 텍스트는 1을 반환한다")
        void onlyWhitespace_ReturnsOne() {
            String content = "     ";
            double ratio = ContentAnalyzer.calculateWhitespaceRatio(content);
            assertThat(ratio).isEqualTo(1.0);
        }

        @Test
        @DisplayName("공백 비율을 정확히 계산한다")
        void mixedContent_ReturnsCorrectRatio() {
            String content = "Hello World";  // 11자 중 1개 공백
            double ratio = ContentAnalyzer.calculateWhitespaceRatio(content);
            assertThat(ratio).isCloseTo(1.0 / 11, org.assertj.core.api.Assertions.within(0.01));
        }

        @Test
        @DisplayName("null 입력은 0을 반환한다")
        void nullInput_ReturnsZero() {
            double ratio = ContentAnalyzer.calculateWhitespaceRatio(null);
            assertThat(ratio).isEqualTo(0.0);
        }

        @Test
        @DisplayName("빈 문자열은 0을 반환한다")
        void emptyString_ReturnsZero() {
            double ratio = ContentAnalyzer.calculateWhitespaceRatio("");
            assertThat(ratio).isEqualTo(0.0);
        }
    }

    @Nested
    @DisplayName("findMaxRepeatCharCount")
    class FindMaxRepeatCharCount {

        @Test
        @DisplayName("반복 문자가 없으면 1을 반환한다")
        void noRepeat_ReturnsOne() {
            String content = "abcdefg";
            int maxRepeat = ContentAnalyzer.findMaxRepeatCharCount(content);
            assertThat(maxRepeat).isEqualTo(1);
        }

        @Test
        @DisplayName("연속 반복 문자의 최대 개수를 반환한다")
        void hasRepeat_ReturnsMaxCount() {
            String content = "Hellooooo World";  // 'o' 5번 연속
            int maxRepeat = ContentAnalyzer.findMaxRepeatCharCount(content);
            assertThat(maxRepeat).isEqualTo(5);
        }

        @Test
        @DisplayName("ㅋㅋㅋㅋ 같은 반복을 감지한다")
        void koreanRepeat_Detected() {
            String content = "재밌다ㅋㅋㅋㅋㅋㅋ";
            int maxRepeat = ContentAnalyzer.findMaxRepeatCharCount(content);
            assertThat(maxRepeat).isEqualTo(6);
        }

        @Test
        @DisplayName("null 입력은 0을 반환한다")
        void nullInput_ReturnsZero() {
            int maxRepeat = ContentAnalyzer.findMaxRepeatCharCount(null);
            assertThat(maxRepeat).isEqualTo(0);
        }
    }

    @Nested
    @DisplayName("calculateKoreanEnglishRatio")
    class CalculateKoreanEnglishRatio {

        @Test
        @DisplayName("한글만 있으면 1에 가까운 값을 반환한다")
        void onlyKorean_ReturnsHigh() {
            String content = "안녕하세요반갑습니다";
            double ratio = ContentAnalyzer.calculateKoreanEnglishRatio(content);
            assertThat(ratio).isEqualTo(1.0);
        }

        @Test
        @DisplayName("영문만 있으면 1에 가까운 값을 반환한다")
        void onlyEnglish_ReturnsHigh() {
            String content = "HelloWorld";
            double ratio = ContentAnalyzer.calculateKoreanEnglishRatio(content);
            assertThat(ratio).isEqualTo(1.0);
        }

        @Test
        @DisplayName("숫자와 특수문자만 있으면 0을 반환한다")
        void onlyNumbersAndSymbols_ReturnsZero() {
            String content = "12345!@#$%";
            double ratio = ContentAnalyzer.calculateKoreanEnglishRatio(content);
            assertThat(ratio).isEqualTo(0.0);
        }

        @Test
        @DisplayName("혼합 텍스트의 비율을 정확히 계산한다")
        void mixedContent_ReturnsCorrectRatio() {
            String content = "Hello123";  // 5 영문 + 3 숫자 = 8자 중 5개 유효
            double ratio = ContentAnalyzer.calculateKoreanEnglishRatio(content);
            assertThat(ratio).isCloseTo(5.0 / 8, org.assertj.core.api.Assertions.within(0.01));
        }
    }

    @Nested
    @DisplayName("calculateWordRepeatRatio")
    class CalculateWordRepeatRatio {

        @Test
        @DisplayName("반복 단어가 없으면 0을 반환한다")
        void noRepeat_ReturnsZero() {
            String content = "안녕하세요 반갑습니다 좋은하루";
            double ratio = ContentAnalyzer.calculateWordRepeatRatio(content);
            assertThat(ratio).isEqualTo(0.0);
        }

        @Test
        @DisplayName("반복 단어가 있으면 비율을 반환한다")
        void hasRepeat_ReturnsRatio() {
            String content = "안녕 안녕 안녕 반가워";  // '안녕' 3번 반복
            double ratio = ContentAnalyzer.calculateWordRepeatRatio(content);
            assertThat(ratio).isGreaterThan(0.0);
        }
    }

    @Nested
    @DisplayName("hasMinimumLength")
    class HasMinimumLength {

        @Test
        @DisplayName("최소 길이 이상이면 true를 반환한다")
        void meetsMinimum_ReturnsTrue() {
            String content = "이것은 최소 길이를 충족하는 텍스트입니다";
            boolean result = ContentAnalyzer.hasMinimumLength(content, 10);
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("최소 길이 미만이면 false를 반환한다")
        void belowMinimum_ReturnsFalse() {
            String content = "짧음";
            boolean result = ContentAnalyzer.hasMinimumLength(content, 10);
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("앞뒤 공백은 제거하고 계산한다")
        void trimsWhitespace() {
            String content = "   abc   ";
            boolean result = ContentAnalyzer.hasMinimumLength(content, 3);
            assertThat(result).isTrue();

            boolean result2 = ContentAnalyzer.hasMinimumLength(content, 4);
            assertThat(result2).isFalse();
        }
    }
}
