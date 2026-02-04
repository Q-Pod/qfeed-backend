package com.ktb.abuse.guard.duplicate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.abuse.store.ContentHashStore;
import com.ktb.abuse.util.SimHashCalculator;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("InMemoryDuplicateGuard 단위 테스트")
class InMemoryDuplicateGuardTest {

    private static final Long ACCOUNT_ID = 1L;
    private static final Long QUESTION_ID = 1L;
    private static final String CLIENT_IP = "127.0.0.1";

    @Mock
    private ContentHashStore contentHashStore;

    @Mock
    private AbuseGuardProperties properties;

    private InMemoryDuplicateGuard guard;

    @BeforeEach
    void setUp() {
        guard = new InMemoryDuplicateGuard(contentHashStore, properties);
    }

    private AbuseCheckContext createContext(String content) {
        return AbuseCheckContext.of(ACCOUNT_ID, QUESTION_ID, CLIENT_IP, content);
    }

    @Nested
    @DisplayName("빈 콘텐츠 처리")
    class EmptyContentHandling {

        @Test
        @DisplayName("null 콘텐츠 시 ACCEPT 반환 (다른 Guard에서 처리)")
        void nullContent_ShouldAccept() {
            // Given
            AbuseCheckContext context = createContext(null);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("빈 문자열 콘텐츠 시 ACCEPT 반환")
        void emptyContent_ShouldAccept() {
            // Given
            AbuseCheckContext context = createContext("");

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("공백만 있는 콘텐츠 시 ACCEPT 반환")
        void onlyWhitespace_ShouldAccept() {
            // Given
            AbuseCheckContext context = createContext("   ");

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("완전 동일 콘텐츠 중복 검증")
    class ExactDuplicateValidation {

        @Test
        @DisplayName("완전 동일 콘텐츠 제출 시 REJECT 반환")
        void exactDuplicate_ShouldReject() {
            // Given
            String content = "이것은 테스트 답변입니다.";
            AbuseCheckContext context = createContext(content);
            when(contentHashStore.existsHash(eq(ACCOUNT_ID), eq(QUESTION_ID), anyString()))
                    .thenReturn(true);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("동일한 답변이 이미 제출되었습니다");
        }

        @Test
        @DisplayName("새로운 콘텐츠 제출 시 중복 검사 통과")
        void newContent_ShouldPassDuplicateCheck() {
            // Given
            String content = "이것은 새로운 답변입니다.";
            AbuseCheckContext context = createContext(content);
            when(contentHashStore.existsHash(eq(ACCOUNT_ID), eq(QUESTION_ID), anyString()))
                    .thenReturn(false);
            when(contentHashStore.getRecentSimHashes(anyLong(), anyLong(), anyInt()))
                    .thenReturn(Collections.emptyList());

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("유사도 기반 중복 검증")
    class SimilarityValidation {

        @Test
        @DisplayName("유사도 임계값 초과 시 REJECT 반환")
        void highSimilarity_ShouldReject() {
            // Given
            String content = "이것은 테스트 답변입니다.";
            AbuseCheckContext context = createContext(content);

            when(contentHashStore.existsHash(eq(ACCOUNT_ID), eq(QUESTION_ID), anyString()))
                    .thenReturn(false);

            // 동일한 콘텐츠의 SimHash를 직접 계산하여 유사도 1.0이 되도록 함
            String sameSimHash = SimHashCalculator.calculateSimHash(content);
            when(contentHashStore.getRecentSimHashes(eq(ACCOUNT_ID), eq(QUESTION_ID), anyInt()))
                    .thenReturn(List.of(sameSimHash));
            when(properties.getSimilarityThreshold()).thenReturn(0.9);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isRejected()).isTrue();
            assertThat(result.getReason()).contains("이전 답변과 너무 유사합니다");
        }

        @Test
        @DisplayName("유사도 임계값 미만 시 ACCEPT 반환")
        void lowSimilarity_ShouldAccept() {
            // Given
            String content = "이것은 완전히 다른 새로운 답변입니다.";
            AbuseCheckContext context = createContext(content);

            when(contentHashStore.existsHash(eq(ACCOUNT_ID), eq(QUESTION_ID), anyString()))
                    .thenReturn(false);

            // 완전히 다른 콘텐츠의 SimHash 계산
            String differentSimHash = SimHashCalculator.calculateSimHash("전혀 다른 내용의 텍스트입니다. ABC XYZ 123");
            when(contentHashStore.getRecentSimHashes(eq(ACCOUNT_ID), eq(QUESTION_ID), anyInt()))
                    .thenReturn(List.of(differentSimHash));
            when(properties.getSimilarityThreshold()).thenReturn(0.9);

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("이전 제출 기록이 없는 경우 ACCEPT 반환")
        void noRecentSubmissions_ShouldAccept() {
            // Given
            String content = "첫 번째 답변입니다.";
            AbuseCheckContext context = createContext(content);

            when(contentHashStore.existsHash(eq(ACCOUNT_ID), eq(QUESTION_ID), anyString()))
                    .thenReturn(false);
            when(contentHashStore.getRecentSimHashes(eq(ACCOUNT_ID), eq(QUESTION_ID), anyInt()))
                    .thenReturn(Collections.emptyList());

            // When
            AbuseGuardResult result = guard.check(context);

            // Then
            assertThat(result.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("해시 저장 테스트")
    class HashStorageTest {

        @Test
        @DisplayName("검증 통과 시 해시 저장")
        void checkPassed_ShouldStoreHash() {
            // Given
            String content = "새로운 답변 내용입니다.";
            AbuseCheckContext context = createContext(content);

            when(contentHashStore.existsHash(eq(ACCOUNT_ID), eq(QUESTION_ID), anyString()))
                    .thenReturn(false);
            when(contentHashStore.getRecentSimHashes(eq(ACCOUNT_ID), eq(QUESTION_ID), anyInt()))
                    .thenReturn(Collections.emptyList());

            // When
            guard.check(context);

            // Then
            verify(contentHashStore).storeHash(eq(ACCOUNT_ID), eq(QUESTION_ID), anyString(), anyString());
        }

        @Test
        @DisplayName("완전 중복 감지 시 해시 저장하지 않음")
        void exactDuplicateDetected_ShouldNotStoreHash() {
            // Given
            String content = "중복된 답변입니다.";
            AbuseCheckContext context = createContext(content);

            when(contentHashStore.existsHash(eq(ACCOUNT_ID), eq(QUESTION_ID), anyString()))
                    .thenReturn(true);

            // When
            guard.check(context);

            // Then
            verify(contentHashStore, never()).storeHash(any(), any(), any(), any());
        }
    }

    @Nested
    @DisplayName("Guard 메타데이터 테스트")
    class GuardMetadata {

        @Test
        @DisplayName("Guard order는 3이다")
        void getOrder_ShouldReturnThree() {
            assertThat(guard.getOrder()).isEqualTo(3);
        }

        @Test
        @DisplayName("Guard name은 DuplicateGuard이다")
        void getName_ShouldReturnDuplicateGuard() {
            assertThat(guard.getName()).isEqualTo("DuplicateGuard");
        }
    }
}
