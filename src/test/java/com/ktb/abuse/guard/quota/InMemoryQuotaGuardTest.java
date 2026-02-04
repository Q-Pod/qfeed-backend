package com.ktb.abuse.guard.quota;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.abuse.store.QuotaStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryQuotaGuard 단위 테스트")
class InMemoryQuotaGuardTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final Long QUESTION_ID = 1L;
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String VALID_CONTENT = "정상적인 답변 내용입니다.";

    private static final int DAILY_QUOTA_LIMIT = 15;
    private static final int SOFT_QUOTA_LIMIT = 10;

    @Mock
    private QuotaStore quotaStore;

    @Mock
    private AbuseGuardProperties properties;

    private InMemoryQuotaGuard guard;

    @BeforeEach
    void setUp() {
        guard = new InMemoryQuotaGuard(quotaStore, properties);
    }

    private AbuseCheckContext createContext() {
        return AbuseCheckContext.of(ACCOUNT_ID, QUESTION_ID, CLIENT_IP, VALID_CONTENT);
    }

    private void setupDefaultQuotaLimits() {
        when(properties.getDailyQuotaLimit()).thenReturn(DAILY_QUOTA_LIMIT);
        when(properties.getSoftQuotaLimit()).thenReturn(SOFT_QUOTA_LIMIT);
    }

    @Nested
    @DisplayName("일일 할당량 검증")
    class DailyQuotaValidation {

        @Test
        @DisplayName("일일 할당량 미만 시 ACCEPT 반환")
        void withinDailyLimit_ShouldAccept() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(5);
            when(quotaStore.incrementDailyQuota(ACCOUNT_ID)).thenReturn(6);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT);
            assertThat(result.shouldProvideFeedback()).isTrue();
        }

        @Test
        @DisplayName("일일 할당량(Hard Limit) 초과 시 REJECT 반환")
        void exceedsDailyLimit_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(15);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("일일 답변 제출 한도");
            assertThat(result.getReason()).contains("15회");
        }

        @Test
        @DisplayName("할당량 경계값(Hard Limit 도달) 시 REJECT 반환")
        void atDailyLimit_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(DAILY_QUOTA_LIMIT);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
        }
    }

    @Nested
    @DisplayName("Soft 할당량 검증")
    class SoftQuotaValidation {

        @Test
        @DisplayName("Soft 할당량 미만 시 ACCEPT (피드백 포함) 반환")
        void withinSoftLimit_ShouldAcceptWithFeedback() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(8);
            when(quotaStore.incrementDailyQuota(ACCOUNT_ID)).thenReturn(9);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT);
            assertThat(result.shouldProvideFeedback()).isTrue();
        }

        @Test
        @DisplayName("Soft 할당량 경계값 시 ACCEPT (피드백 포함) 반환")
        void atSoftLimit_ShouldAcceptWithFeedback() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(9);
            when(quotaStore.incrementDailyQuota(ACCOUNT_ID)).thenReturn(10);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT);
            assertThat(result.shouldProvideFeedback()).isTrue();
        }

        @Test
        @DisplayName("Soft 할당량 초과 시 ACCEPT_NO_FEEDBACK 반환")
        void exceedsSoftLimit_ShouldAcceptNoFeedback() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(10);
            when(quotaStore.incrementDailyQuota(ACCOUNT_ID)).thenReturn(11);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT_NO_FEEDBACK);
            assertThat(result.shouldProvideFeedback()).isFalse();
            assertThat(result.getReason()).contains("피드백 제공 한도");
            assertThat(result.getQualityScore()).isEqualTo(50);
        }

        @Test
        @DisplayName("Soft 할당량 초과 후 Hard 할당량 미만 시 ACCEPT_NO_FEEDBACK 유지")
        void betweenSoftAndHardLimit_ShouldAcceptNoFeedback() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(13);
            when(quotaStore.incrementDailyQuota(ACCOUNT_ID)).thenReturn(14);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT_NO_FEEDBACK);
            assertThat(result.shouldProvideFeedback()).isFalse();
        }
    }

    @Nested
    @DisplayName("할당량 증가 테스트")
    class QuotaIncrementTest {

        @Test
        @DisplayName("검증 통과 시 할당량 증가")
        void checkPassed_ShouldIncrementQuota() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(5);
            when(quotaStore.incrementDailyQuota(ACCOUNT_ID)).thenReturn(6);

            // When
            guard.check(context);

            // Then
            verify(quotaStore).incrementDailyQuota(ACCOUNT_ID);
        }

        @Test
        @DisplayName("Hard Limit 초과 시 할당량 증가하지 않음")
        void hardLimitExceeded_ShouldNotIncrementQuota() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultQuotaLimits();
            when(quotaStore.getDailyQuota(ACCOUNT_ID)).thenReturn(DAILY_QUOTA_LIMIT);

            // When
            guard.check(context);

            // Then
            verify(quotaStore, never()).incrementDailyQuota(ACCOUNT_ID);
        }
    }

    @Nested
    @DisplayName("Guard 메타데이터 테스트")
    class GuardMetadata {

        @Test
        @DisplayName("Guard order는 5이다")
        void getOrder_ShouldReturnFive() {
            assertThat(guard.getOrder()).isEqualTo(5);
        }

        @Test
        @DisplayName("Guard name은 QuotaGuard이다")
        void getName_ShouldReturnQuotaGuard() {
            assertThat(guard.getName()).isEqualTo("QuotaGuard");
        }
    }
}
