package com.ktb.abuse.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.ktb.abuse.exception.DailyQuotaExceededException;
import com.ktb.abuse.exception.DuplicateContentException;
import com.ktb.abuse.exception.LowQualityContentException;
import com.ktb.abuse.exception.RateLimitExceededException;
import com.ktb.abuse.guard.Guard;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AbuseGuard 통합 테스트")
class AbuseGuardTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final Long QUESTION_ID = 1L;
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String VALID_CONTENT = "정상적인 답변 내용입니다. 충분한 길이를 가진 텍스트입니다.";

    @Mock
    private Guard rateLimitGuard;

    @Mock
    private Guard contentQualityGuard;

    @Mock
    private Guard duplicateGuard;

    @Mock
    private Guard quotaGuard;

    private AbuseGuard abuseGuard;

    @BeforeEach
    void setUp() {
        when(rateLimitGuard.getOrder()).thenReturn(1);
        when(rateLimitGuard.getName()).thenReturn("RateLimitGuard");

        when(contentQualityGuard.getOrder()).thenReturn(2);
        when(contentQualityGuard.getName()).thenReturn("ContentQualityGuard");

        when(duplicateGuard.getOrder()).thenReturn(3);
        when(duplicateGuard.getName()).thenReturn("DuplicateGuard");

        when(quotaGuard.getOrder()).thenReturn(5);
        when(quotaGuard.getName()).thenReturn("QuotaGuard");

        abuseGuard = new AbuseGuard(List.of(rateLimitGuard, contentQualityGuard, duplicateGuard, quotaGuard));
    }

    private AbuseCheckContext createContext() {
        return AbuseCheckContext.of(ACCOUNT_ID, QUESTION_ID, CLIENT_IP, VALID_CONTENT);
    }

    @Nested
    @DisplayName("모든 Guard 통과 케이스")
    class AllGuardsPass {

        @Test
        @DisplayName("모든 Guard가 ACCEPT 반환 시 ACCEPT 반환")
        void allGuardsAccept_ShouldReturnAccept() {
            // Given
            AbuseCheckContext context = createContext();
            when(rateLimitGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(contentQualityGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(duplicateGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(quotaGuard.check(any())).thenReturn(AbuseGuardResult.accept());

            // When
            AbuseGuardResult result = abuseGuard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT);
            assertThat(result.shouldProvideFeedback()).isTrue();
        }

        @Test
        @DisplayName("ACCEPT와 ACCEPT_NO_FEEDBACK 혼합 시 ACCEPT_NO_FEEDBACK 반환")
        void mixedAcceptStatuses_ShouldReturnAcceptNoFeedback() {
            // Given
            AbuseCheckContext context = createContext();
            when(rateLimitGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(contentQualityGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(duplicateGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(quotaGuard.check(any())).thenReturn(
                    AbuseGuardResult.acceptNoFeedback("QuotaGuard", "Soft 한도 초과", 50));

            // When
            AbuseGuardResult result = abuseGuard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT_NO_FEEDBACK);
            assertThat(result.shouldProvideFeedback()).isFalse();
        }

        @Test
        @DisplayName("품질 점수는 최소값으로 결정")
        void multipleAccepts_ShouldUseMinimumQualityScore() {
            // Given
            AbuseCheckContext context = createContext();
            when(rateLimitGuard.check(any())).thenReturn(AbuseGuardResult.accept(80));
            when(contentQualityGuard.check(any())).thenReturn(AbuseGuardResult.accept(60));
            when(duplicateGuard.check(any())).thenReturn(AbuseGuardResult.accept(90));
            when(quotaGuard.check(any())).thenReturn(AbuseGuardResult.accept(70));

            // When
            AbuseGuardResult result = abuseGuard.check(context);

            // Then
            assertThat(result.getQualityScore()).isEqualTo(60);
        }
    }

    @Nested
    @DisplayName("RateLimitGuard 실패 케이스")
    class RateLimitGuardFails {

        @Test
        @DisplayName("RateLimitGuard REJECT 시 RateLimitExceededException 발생")
        void rateLimitReject_ShouldThrowRateLimitExceededException() {
            // Given
            AbuseCheckContext context = createContext();
            when(rateLimitGuard.check(any())).thenReturn(
                    AbuseGuardResult.reject("RateLimitGuard", "분당 제출 횟수 초과"));

            // When & Then
            assertThatThrownBy(() -> abuseGuard.check(context))
                    .isInstanceOf(RateLimitExceededException.class);
        }
    }

    @Nested
    @DisplayName("ContentQualityGuard 실패 케이스")
    class ContentQualityGuardFails {

        @Test
        @DisplayName("ContentQualityGuard REJECT 시 LowQualityContentException 발생")
        void contentQualityReject_ShouldThrowLowQualityContentException() {
            // Given
            AbuseCheckContext context = createContext();
            when(rateLimitGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(contentQualityGuard.check(any())).thenReturn(
                    AbuseGuardResult.reject("ContentQualityGuard", "답변 내용이 너무 짧습니다"));

            // When & Then
            assertThatThrownBy(() -> abuseGuard.check(context))
                    .isInstanceOf(LowQualityContentException.class);
        }
    }

    @Nested
    @DisplayName("DuplicateGuard 실패 케이스")
    class DuplicateGuardFails {

        @Test
        @DisplayName("DuplicateGuard REJECT 시 DuplicateContentException 발생")
        void duplicateReject_ShouldThrowDuplicateContentException() {
            // Given
            AbuseCheckContext context = createContext();
            when(rateLimitGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(contentQualityGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(duplicateGuard.check(any())).thenReturn(
                    AbuseGuardResult.reject("DuplicateGuard", "동일한 답변이 이미 제출되었습니다"));

            // When & Then
            assertThatThrownBy(() -> abuseGuard.check(context))
                    .isInstanceOf(DuplicateContentException.class);
        }
    }

    @Nested
    @DisplayName("QuotaGuard 실패 케이스")
    class QuotaGuardFails {

        @Test
        @DisplayName("QuotaGuard REJECT 시 DailyQuotaExceededException 발생")
        void quotaReject_ShouldThrowDailyQuotaExceededException() {
            // Given
            AbuseCheckContext context = createContext();
            when(rateLimitGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(contentQualityGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(duplicateGuard.check(any())).thenReturn(AbuseGuardResult.accept());
            when(quotaGuard.check(any())).thenReturn(
                    AbuseGuardResult.reject("QuotaGuard", "일일 답변 제출 한도를 초과했습니다"));

            // When & Then
            assertThatThrownBy(() -> abuseGuard.check(context))
                    .isInstanceOf(DailyQuotaExceededException.class);
        }
    }

    @Nested
    @DisplayName("Guard 순서 테스트")
    class GuardOrderTest {

        @Test
        @DisplayName("Guard는 order 순서대로 실행된다")
        void guardsExecuteInOrder() {
            // Given
            AbuseCheckContext context = createContext();

            // RateLimitGuard (order=1)가 먼저 REJECT되면 뒤의 Guard는 실행되지 않음
            when(rateLimitGuard.check(any())).thenReturn(
                    AbuseGuardResult.reject("RateLimitGuard", "제한 초과"));

            // When & Then
            assertThatThrownBy(() -> abuseGuard.check(context))
                    .isInstanceOf(RateLimitExceededException.class);
        }
    }

    @Nested
    @DisplayName("getActiveGuards 테스트")
    class GetActiveGuardsTest {

        @Test
        @DisplayName("활성화된 Guard 목록을 반환한다")
        void getActiveGuards_ShouldReturnGuardNames() {
            // When
            List<String> activeGuards = abuseGuard.getActiveGuards();

            // Then
            assertThat(activeGuards).containsExactly(
                    "RateLimitGuard",
                    "ContentQualityGuard",
                    "DuplicateGuard",
                    "QuotaGuard"
            );
        }
    }
}
