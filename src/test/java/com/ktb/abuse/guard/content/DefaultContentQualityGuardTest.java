package com.ktb.abuse.guard.content;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DefaultContentQualityGuard 단위 테스트")
class DefaultContentQualityGuardTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final Long QUESTION_ID = 1L;
    private static final String CLIENT_IP = "127.0.0.1";

    @Mock
    private AbuseGuardProperties properties;

    private DefaultContentQualityGuard guard;

    @BeforeEach
    void setUp() {
        guard = new DefaultContentQualityGuard(properties);
    }

    private AbuseCheckContext createContext(String content) {
        return AbuseCheckContext.of(ACCOUNT_ID, QUESTION_ID, CLIENT_IP, content);
    }

    private void setupDefaultProperties() {
        when(properties.getMaxWhitespaceRatio()).thenReturn(0.4);
        when(properties.getMaxRepeatCharCount()).thenReturn(5);
        when(properties.getMinKoreanEnglishRatio()).thenReturn(0.5);
    }

    @Nested
    @DisplayName("빈 콘텐츠 검증")
    class EmptyContentValidation {

        @Test
        @DisplayName("null 콘텐츠 시 REJECT 반환")
        void nullContent_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext(null);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("비어있습니다");
        }

        @Test
        @DisplayName("빈 문자열 콘텐츠 시 REJECT 반환")
        void emptyContent_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext("");

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("비어있습니다");
        }

        @Test
        @DisplayName("공백만 있는 콘텐츠 시 REJECT 반환")
        void onlyWhitespace_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext("   \t\n  ");

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("비어있습니다");
        }
    }

    @Nested
    @DisplayName("최소 길이 검증")
    class MinimumLengthValidation {

        @Test
        @DisplayName("최소 길이(10자) 미만 시 REJECT 반환")
        void belowMinimumLength_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext("짧은답변");

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("너무 짧습니다");
            assertThat(result.getReason()).contains("최소 10자");
        }

        @Test
        @DisplayName("최소 길이 충족 시 길이 검증 통과")
        void meetsMinimumLength_ShouldPassLengthCheck() {
            // Given
            String validLengthContent = "이것은 충분히 긴 답변입니다";
            AbuseCheckContext context = createContext(validLengthContent);
            setupDefaultProperties();

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("공백 비율 검증")
    class WhitespaceRatioValidation {

        @Test
        @DisplayName("공백 비율 초과 시 REJECT 반환")
        void excessiveWhitespace_ShouldReject() {
            // Given
            String excessiveWhitespaceContent = "답   변    내    용   입   니   다";
            AbuseCheckContext context = createContext(excessiveWhitespaceContent);
            when(properties.getMaxWhitespaceRatio()).thenReturn(0.4);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("공백이 너무 많습니다");
        }

        @Test
        @DisplayName("적절한 공백 비율 시 ACCEPT 반환")
        void normalWhitespace_ShouldAccept() {
            // Given
            String normalContent = "이것은 정상적인 답변 내용입니다";
            AbuseCheckContext context = createContext(normalContent);
            setupDefaultProperties();

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("반복 문자 검증")
    class RepeatCharacterValidation {

        @Test
        @DisplayName("반복 문자 과다(ㅋㅋㅋㅋㅋㅋ) 시 REJECT 반환")
        void excessiveKoreanRepeat_ShouldReject() {
            // Given
            String repeatContent = "재밌다ㅋㅋㅋㅋㅋㅋㅋㅋ";
            AbuseCheckContext context = createContext(repeatContent);
            when(properties.getMaxWhitespaceRatio()).thenReturn(0.4);
            when(properties.getMaxRepeatCharCount()).thenReturn(5);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("동일 문자가 과도하게 반복");
        }

        @Test
        @DisplayName("영문 반복 문자 과다 시 REJECT 반환")
        void excessiveEnglishRepeat_ShouldReject() {
            // Given
            String repeatContent = "Helllllllllo World";
            AbuseCheckContext context = createContext(repeatContent);
            when(properties.getMaxWhitespaceRatio()).thenReturn(0.4);
            when(properties.getMaxRepeatCharCount()).thenReturn(5);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("동일 문자가 과도하게 반복");
        }

        @Test
        @DisplayName("허용 범위 내 반복 시 ACCEPT 반환")
        void acceptableRepeat_ShouldAccept() {
            // Given
            String normalContent = "이것은 정상적인 답변입니다ㅋㅋㅋ";
            AbuseCheckContext context = createContext(normalContent);
            setupDefaultProperties();

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("한글/영문 비율 검증")
    class KoreanEnglishRatioValidation {

        @Test
        @DisplayName("유효 문자 비율 미달 시 REJECT 반환")
        void lowValidCharRatio_ShouldReject() {
            // Given
            String invalidContent = "12345!@#$%67890^&*()";
            AbuseCheckContext context = createContext(invalidContent);
            when(properties.getMaxWhitespaceRatio()).thenReturn(0.4);
            when(properties.getMaxRepeatCharCount()).thenReturn(5);
            when(properties.getMinKoreanEnglishRatio()).thenReturn(0.5);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("유효한 문자(한글/영문) 비율이 너무 낮습니다");
        }

        @Test
        @DisplayName("적절한 유효 문자 비율 시 ACCEPT 반환")
        void validCharRatio_ShouldAccept() {
            // Given
            String validContent = "This is a valid answer";
            AbuseCheckContext context = createContext(validContent);
            setupDefaultProperties();

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("정상 콘텐츠 테스트")
    class ValidContentTest {

        @Test
        @DisplayName("모든 검증 통과 시 ACCEPT 반환")
        void validContent_ShouldAccept() {
            // Given
            String validContent = "이것은 정상적인 답변 내용입니다. 충분한 길이와 적절한 형식을 갖추고 있습니다.";
            AbuseCheckContext context = createContext(validContent);
            setupDefaultProperties();

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT);
        }

        @Test
        @DisplayName("한글과 영문이 혼합된 콘텐츠 ACCEPT 반환")
        void mixedKoreanEnglish_ShouldAccept() {
            // Given
            String mixedContent = "Spring Boot를 사용한 REST API 개발 방법입니다.";
            AbuseCheckContext context = createContext(mixedContent);
            setupDefaultProperties();

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("Guard 메타데이터 테스트")
    class GuardMetadata {

        @Test
        @DisplayName("Guard order는 2이다")
        void getOrder_ShouldReturnTwo() {
            assertThat(guard.getOrder()).isEqualTo(2);
        }

        @Test
        @DisplayName("Guard name은 ContentQualityGuard이다")
        void getName_ShouldReturnContentQualityGuard() {
            assertThat(guard.getName()).isEqualTo("ContentQualityGuard");
        }
    }
}
