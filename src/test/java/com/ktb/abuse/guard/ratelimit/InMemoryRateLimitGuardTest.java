package com.ktb.abuse.guard.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.abuse.store.CooldownStore;
import com.ktb.abuse.store.RateLimitStore;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("InMemoryRateLimitGuard 단위 테스트")
class InMemoryRateLimitGuardTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final Long QUESTION_ID = 1L;
    private static final String CLIENT_IP = "127.0.0.1";
    private static final String VALID_CONTENT = "정상적인 답변 내용입니다.";

    @Mock
    private RateLimitStore rateLimitStore;

    @Mock
    private CooldownStore cooldownStore;

    @Mock
    private AbuseGuardProperties properties;

    private InMemoryRateLimitGuard guard;

    @BeforeEach
    void setUp() {
        guard = new InMemoryRateLimitGuard(rateLimitStore, cooldownStore, properties);
    }

    private AbuseCheckContext createContext() {
        return AbuseCheckContext.of(ACCOUNT_ID, QUESTION_ID, CLIENT_IP, VALID_CONTENT);
    }

    private AbuseCheckContext createContextWithoutIp() {
        return AbuseCheckContext.of(ACCOUNT_ID, QUESTION_ID, null, VALID_CONTENT);
    }

    private void setupDefaultLimits() {
        when(properties.getUserPerMinute()).thenReturn(3);
        when(properties.getUserPerHour()).thenReturn(30);
        when(properties.getIpPerMinute()).thenReturn(15);
        when(properties.getQuestionCooldownSeconds()).thenReturn(60);
        when(properties.getQuestionConsecutiveLimit()).thenReturn(3);
    }

    @Nested
    @DisplayName("분당 제한 테스트")
    class UserPerMinuteLimit {

        @Test
        @DisplayName("분당 제한 미만 요청 시 ACCEPT 반환")
        void withinLimit_ShouldAccept() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultLimits();
            when(cooldownStore.getLastSubmitTime(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(cooldownStore.getConsecutiveCount(anyLong(), anyLong())).thenReturn(0);
            when(rateLimitStore.incrementAndGet(anyString(), any(Duration.class))).thenReturn(1L);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            assertThat(result.getStatus()).isEqualTo(AbuseGuardResult.Status.ACCEPT);
        }

        @Test
        @DisplayName("분당 제한 초과 시 REJECT 반환")
        void exceedsLimit_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultLimits();
            when(cooldownStore.getLastSubmitTime(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(cooldownStore.getConsecutiveCount(anyLong(), anyLong())).thenReturn(0);
            when(rateLimitStore.incrementAndGet(eq("user:minute:" + ACCOUNT_ID), any(Duration.class)))
                    .thenReturn(4L);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("분당 제출 횟수");
        }
    }

    @Nested
    @DisplayName("시간당 제한 테스트")
    class UserPerHourLimit {

        @Test
        @DisplayName("시간당 제한 초과 시 REJECT 반환")
        void exceedsHourLimit_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultLimits();
            when(cooldownStore.getLastSubmitTime(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(cooldownStore.getConsecutiveCount(anyLong(), anyLong())).thenReturn(0);
            when(rateLimitStore.incrementAndGet(eq("user:minute:" + ACCOUNT_ID), any(Duration.class)))
                    .thenReturn(1L);
            when(rateLimitStore.incrementAndGet(eq("user:hour:" + ACCOUNT_ID), any(Duration.class)))
                    .thenReturn(31L);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("시간당 제출 횟수");
        }
    }

    @Nested
    @DisplayName("IP당 분당 제한 테스트")
    class IpPerMinuteLimit {

        @Test
        @DisplayName("IP당 분당 제한 초과 시 REJECT 반환")
        void exceedsIpLimit_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultLimits();
            when(cooldownStore.getLastSubmitTime(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(cooldownStore.getConsecutiveCount(anyLong(), anyLong())).thenReturn(0);
            when(rateLimitStore.incrementAndGet(eq("user:minute:" + ACCOUNT_ID), any(Duration.class)))
                    .thenReturn(1L);
            when(rateLimitStore.incrementAndGet(eq("user:hour:" + ACCOUNT_ID), any(Duration.class)))
                    .thenReturn(1L);
            when(rateLimitStore.incrementAndGet(eq("ip:minute:" + CLIENT_IP), any(Duration.class)))
                    .thenReturn(16L);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("IP당 분당 제출 횟수");
        }

        @Test
        @DisplayName("IP가 null인 경우 IP 제한 검사 건너뛰기")
        void nullIp_ShouldSkipIpCheck() {
            // Given
            AbuseCheckContext context = createContextWithoutIp();
            setupDefaultLimits();
            when(cooldownStore.getLastSubmitTime(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(cooldownStore.getConsecutiveCount(anyLong(), anyLong())).thenReturn(0);
            when(rateLimitStore.incrementAndGet(eq("user:minute:" + ACCOUNT_ID), any(Duration.class)))
                    .thenReturn(1L);
            when(rateLimitStore.incrementAndGet(eq("user:hour:" + ACCOUNT_ID), any(Duration.class)))
                    .thenReturn(1L);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
            verify(rateLimitStore, never()).incrementAndGet(eq("ip:minute:null"), any(Duration.class));
        }
    }

    @Nested
    @DisplayName("질문 쿨다운 테스트")
    class QuestionCooldown {

        @Test
        @DisplayName("쿨다운 시간 내 재시도 시 REJECT 반환")
        void withinCooldown_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext();
            long currentTime = System.currentTimeMillis();
            long lastSubmitTime = currentTime - 30_000; // 30초 전

            when(properties.getQuestionCooldownSeconds()).thenReturn(60);
            when(cooldownStore.getLastSubmitTime(ACCOUNT_ID, QUESTION_ID))
                    .thenReturn(Optional.of(lastSubmitTime));

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("대기 시간");
        }

        @Test
        @DisplayName("쿨다운 경과 후 재시도 시 ACCEPT 반환")
        void afterCooldown_ShouldAccept() {
            // Given
            AbuseCheckContext context = createContext();
            long currentTime = System.currentTimeMillis();
            long lastSubmitTime = currentTime - 70_000; // 70초 전

            setupDefaultLimits();
            when(cooldownStore.getLastSubmitTime(ACCOUNT_ID, QUESTION_ID))
                    .thenReturn(Optional.of(lastSubmitTime));
            when(cooldownStore.getConsecutiveCount(anyLong(), anyLong())).thenReturn(0);
            when(rateLimitStore.incrementAndGet(anyString(), any(Duration.class))).thenReturn(1L);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("연속 제출 제한 테스트")
    class ConsecutiveSubmitLimit {

        @Test
        @DisplayName("연속 제출 횟수 초과 시 REJECT 반환")
        void exceedsConsecutiveLimit_ShouldReject() {
            // Given
            AbuseCheckContext context = createContext();
            when(properties.getQuestionCooldownSeconds()).thenReturn(60);
            when(properties.getQuestionConsecutiveLimit()).thenReturn(3);
            when(cooldownStore.getLastSubmitTime(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(cooldownStore.getConsecutiveCount(ACCOUNT_ID, QUESTION_ID)).thenReturn(3);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("연속 제출 횟수");
        }

        @Test
        @DisplayName("연속 제출 횟수 미만 시 ACCEPT 반환")
        void withinConsecutiveLimit_ShouldAccept() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultLimits();
            when(cooldownStore.getLastSubmitTime(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(cooldownStore.getConsecutiveCount(ACCOUNT_ID, QUESTION_ID)).thenReturn(2);
            when(rateLimitStore.incrementAndGet(anyString(), any(Duration.class))).thenReturn(1L);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("통과 후 업데이트 테스트")
    class AfterCheckUpdate {

        @Test
        @DisplayName("모든 검사 통과 시 쿨다운 정보 업데이트")
        void allChecksPassed_ShouldUpdateCooldownInfo() {
            // Given
            AbuseCheckContext context = createContext();
            setupDefaultLimits();
            when(cooldownStore.getLastSubmitTime(anyLong(), anyLong())).thenReturn(Optional.empty());
            when(cooldownStore.getConsecutiveCount(anyLong(), anyLong())).thenReturn(0);
            when(rateLimitStore.incrementAndGet(anyString(), any(Duration.class))).thenReturn(1L);

            // When
            guard.check(context);

            // Then
            verify(cooldownStore).setLastSubmitTime(eq(ACCOUNT_ID), eq(QUESTION_ID), anyLong());
            verify(cooldownStore).incrementConsecutiveCount(ACCOUNT_ID, QUESTION_ID);
        }
    }

    @Nested
    @DisplayName("Guard 메타데이터 테스트")
    class GuardMetadata {

        @Test
        @DisplayName("Guard order는 1이다")
        void getOrder_ShouldReturnOne() {
            assertThat(guard.getOrder()).isEqualTo(1);
        }

        @Test
        @DisplayName("Guard name은 RateLimitGuard이다")
        void getName_ShouldReturnRateLimitGuard() {
            assertThat(guard.getName()).isEqualTo("RateLimitGuard");
        }
    }
}
