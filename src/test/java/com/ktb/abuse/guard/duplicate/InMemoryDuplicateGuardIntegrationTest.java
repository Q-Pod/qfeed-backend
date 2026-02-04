package com.ktb.abuse.guard.duplicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.ktb.abuse.config.AbuseGuardProperties;
import com.ktb.abuse.core.AbuseCheckContext;
import com.ktb.abuse.core.AbuseGuardResult;
import com.ktb.abuse.store.inmemory.InMemoryContentHashStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("InMemoryDuplicateGuard 통합 테스트 (실제 Store 사용)")
class InMemoryDuplicateGuardIntegrationTest {

    private static final String CLIENT_IP = "127.0.0.1";

    private InMemoryDuplicateGuard guard;
    private InMemoryContentHashStore store;
    private AbuseGuardProperties properties;

    @BeforeEach
    void setUp() {
        store = new InMemoryContentHashStore();
        properties = new AbuseGuardProperties();
        properties.setSimilarityThreshold(0.9);
        guard = new InMemoryDuplicateGuard(store, properties);
    }

    @Nested
    @DisplayName("같은 사용자 + 같은 질문 중복 검증")
    class SameUserSameQuestionTest {

        @Test
        @DisplayName("동일 답변 2번 제출 시 두 번째는 REJECT")
        void duplicateAnswer_ShouldRejectSecond() {
            // Given
            Long accountId = 1L;
            Long questionId = 100L;
            String content = "이것은 동일한 답변입니다.";

            AbuseCheckContext firstContext = AbuseCheckContext.of(accountId, questionId, CLIENT_IP, content);
            AbuseCheckContext secondContext = AbuseCheckContext.of(accountId, questionId, CLIENT_IP, content);

            // When - 첫 번째 제출
            AbuseGuardResult firstResult = guard.check(firstContext);

            // Then - 첫 번째는 ACCEPT
            assertThat(firstResult.isAccepted()).isTrue();

            // When - 두 번째 제출 (동일 답변)
            AbuseGuardResult secondResult = guard.check(secondContext);

            // Then - 두 번째는 REJECT
            assertThat(secondResult.isRejected()).isTrue();
            assertThat(secondResult.getReason()).contains("동일한 답변이 이미 제출되었습니다");
        }

        @Test
        @DisplayName("유사 답변 제출 시 REJECT")
        void similarAnswer_ShouldReject() {
            // Given
            Long accountId = 1L;
            Long questionId = 100L;
            String originalContent = "Spring Boot를 사용한 REST API 개발 방법에 대해 설명하겠습니다.";
            String similarContent = "Spring Boot를 사용한 REST API 개발 방법에 대해 설명해 드리겠습니다.";

            AbuseCheckContext firstContext = AbuseCheckContext.of(accountId, questionId, CLIENT_IP, originalContent);
            AbuseCheckContext secondContext = AbuseCheckContext.of(accountId, questionId, CLIENT_IP, similarContent);

            // When - 첫 번째 제출
            AbuseGuardResult firstResult = guard.check(firstContext);
            assertThat(firstResult.isAccepted()).isTrue();

            // When - 두 번째 제출 (유사 답변)
            AbuseGuardResult secondResult = guard.check(secondContext);

            // Then - 유사도가 높으면 REJECT
            assertThat(secondResult.isRejected()).isTrue();
            assertThat(secondResult.getReason()).contains("이전 답변과 너무 유사합니다");
        }

        @Test
        @DisplayName("완전히 다른 답변은 ACCEPT")
        void differentAnswer_ShouldAccept() {
            // Given
            Long accountId = 1L;
            Long questionId = 100L;
            // 완전히 다른 주제와 단어로 구성된 답변
            String answer1 = "자바스크립트 비동기 처리는 콜백, 프로미스, async/await 세 가지 방식이 있습니다.";
            String answer2 = "쿠버네티스 클러스터 배포 시 노드 리소스 할당량 설정이 중요합니다.";

            // When
            AbuseGuardResult result1 = guard.check(
                    AbuseCheckContext.of(accountId, questionId, CLIENT_IP, answer1));
            AbuseGuardResult result2 = guard.check(
                    AbuseCheckContext.of(accountId, questionId, CLIENT_IP, answer2));

            // Then - 서로 다른 내용이므로 모두 ACCEPT
            assertThat(result1.isAccepted()).isTrue();
            assertThat(result2.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("3번째 답변이 첫 번째와 유사하면 REJECT")
        void thirdAnswerSimilarToFirst_ShouldReject() {
            // Given
            Long accountId = 1L;
            Long questionId = 100L;
            // 완전히 다른 주제의 답변들 사용
            String answer1 = "데이터베이스 인덱싱은 검색 성능을 향상시키는 핵심 기술입니다.";
            String answer2 = "클라우드 컴퓨팅은 인터넷을 통해 컴퓨팅 리소스를 제공합니다.";
            String answer3 = "데이터베이스 인덱싱은 검색 성능을 개선하는 핵심 기술입니다."; // answer1과 유사

            // When - 첫 번째, 두 번째 답변 제출
            AbuseGuardResult result1 = guard.check(
                    AbuseCheckContext.of(accountId, questionId, CLIENT_IP, answer1));
            AbuseGuardResult result2 = guard.check(
                    AbuseCheckContext.of(accountId, questionId, CLIENT_IP, answer2));

            // Then - 둘 다 ACCEPT (서로 다른 내용)
            assertThat(result1.isAccepted()).isTrue();
            assertThat(result2.isAccepted()).isTrue();

            // When - 세 번째 답변 (첫 번째와 유사)
            AbuseGuardResult result3 = guard.check(
                    AbuseCheckContext.of(accountId, questionId, CLIENT_IP, answer3));

            // Then - answer1과 유사하므로 REJECT
            assertThat(result3.isRejected()).isTrue();
            assertThat(result3.getReason()).contains("이전 답변과 너무 유사합니다");
        }
    }

    @Nested
    @DisplayName("키 격리 검증 (accountId + questionId)")
    class KeyIsolationTest {

        @Test
        @DisplayName("같은 사용자가 다른 질문에는 동일 답변 가능")
        void sameUserDifferentQuestion_SameAnswer_ShouldAccept() {
            // Given
            Long accountId = 1L;
            Long questionId1 = 100L;
            Long questionId2 = 200L;
            String content = "동일한 답변 내용입니다.";

            // When - 질문1에 제출
            AbuseGuardResult result1 = guard.check(
                    AbuseCheckContext.of(accountId, questionId1, CLIENT_IP, content));

            // When - 다른 질문에 동일 답변 제출
            AbuseGuardResult result2 = guard.check(
                    AbuseCheckContext.of(accountId, questionId2, CLIENT_IP, content));

            // Then - 다른 질문이므로 모두 ACCEPT
            assertThat(result1.isAccepted()).isTrue();
            assertThat(result2.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("다른 사용자는 같은 질문에 동일 답변 가능")
        void differentUserSameQuestion_SameAnswer_ShouldAccept() {
            // Given
            Long accountId1 = 1L;
            Long accountId2 = 2L;
            Long questionId = 100L;
            String content = "동일한 답변 내용입니다.";

            // When - 사용자1이 제출
            AbuseGuardResult result1 = guard.check(
                    AbuseCheckContext.of(accountId1, questionId, CLIENT_IP, content));

            // When - 다른 사용자가 동일 답변 제출
            AbuseGuardResult result2 = guard.check(
                    AbuseCheckContext.of(accountId2, questionId, CLIENT_IP, content));

            // Then - 다른 사용자이므로 모두 ACCEPT
            assertThat(result1.isAccepted()).isTrue();
            assertThat(result2.isAccepted()).isTrue();
        }

        @Test
        @DisplayName("다른 사용자 + 다른 질문 → 완전히 독립")
        void differentUserDifferentQuestion_ShouldBeIndependent() {
            // Given
            String content = "동일한 답변 내용입니다.";

            // When - 각기 다른 사용자가 각기 다른 질문에 동일 답변
            AbuseGuardResult result1 = guard.check(
                    AbuseCheckContext.of(1L, 100L, CLIENT_IP, content));
            AbuseGuardResult result2 = guard.check(
                    AbuseCheckContext.of(2L, 200L, CLIENT_IP, content));
            AbuseGuardResult result3 = guard.check(
                    AbuseCheckContext.of(3L, 300L, CLIENT_IP, content));

            // Then - 모두 독립적이므로 ACCEPT
            assertThat(result1.isAccepted()).isTrue();
            assertThat(result2.isAccepted()).isTrue();
            assertThat(result3.isAccepted()).isTrue();
        }
    }

    @Nested
    @DisplayName("복합 시나리오 테스트")
    class ComplexScenarioTest {

        @Test
        @DisplayName("여러 사용자가 여러 질문에 답변 시 올바른 격리")
        void multipleUsersMultipleQuestions_ShouldIsolateCorrectly() {
            // Given
            String commonContent = "공통된 답변 내용입니다.";

            // User1 - Question100에 답변
            AbuseGuardResult u1q100 = guard.check(
                    AbuseCheckContext.of(1L, 100L, CLIENT_IP, commonContent));

            // User1 - Question200에 동일 답변 (다른 질문이므로 OK)
            AbuseGuardResult u1q200 = guard.check(
                    AbuseCheckContext.of(1L, 200L, CLIENT_IP, commonContent));

            // User2 - Question100에 동일 답변 (다른 사용자이므로 OK)
            AbuseGuardResult u2q100 = guard.check(
                    AbuseCheckContext.of(2L, 100L, CLIENT_IP, commonContent));

            // User1 - Question100에 다시 동일 답변 (같은 사용자, 같은 질문이므로 REJECT)
            AbuseGuardResult u1q100Again = guard.check(
                    AbuseCheckContext.of(1L, 100L, CLIENT_IP, commonContent));

            // Then
            assertThat(u1q100.isAccepted()).isTrue();
            assertThat(u1q200.isAccepted()).isTrue();
            assertThat(u2q100.isAccepted()).isTrue();
            assertThat(u1q100Again.isRejected()).isTrue();
        }

        @Test
        @DisplayName("동일 사용자가 여러 질문에 각각 다른 답변 후, 특정 질문에만 중복 발생")
        void sameUserMultipleQuestions_DuplicateOnlyInSpecificQuestion() {
            // Given
            Long accountId = 1L;
            // 완전히 다른 구조와 내용의 답변들
            String answerForQ100 = "AAAA BBBB CCCC DDDD EEEE FFFF GGGG";
            String answerForQ200 = "1111 2222 3333 4444 5555 6666 7777";
            String answerForQ300 = "xxxx yyyy zzzz wwww vvvv uuuu tttt";

            // When - 각 질문에 최초 답변
            guard.check(AbuseCheckContext.of(accountId, 100L, CLIENT_IP, answerForQ100));
            guard.check(AbuseCheckContext.of(accountId, 200L, CLIENT_IP, answerForQ200));
            guard.check(AbuseCheckContext.of(accountId, 300L, CLIENT_IP, answerForQ300));

            // When - Q100에만 중복 답변 시도
            AbuseGuardResult duplicateOnQ100 = guard.check(
                    AbuseCheckContext.of(accountId, 100L, CLIENT_IP, answerForQ100));

            // Q200, Q300에는 여전히 새로운 답변 가능 (완전히 다른 구조)
            AbuseGuardResult newOnQ200 = guard.check(
                    AbuseCheckContext.of(accountId, 200L, CLIENT_IP,
                            "qqqq rrrr ssss pppp oooo nnnn mmmm"));
            AbuseGuardResult newOnQ300 = guard.check(
                    AbuseCheckContext.of(accountId, 300L, CLIENT_IP,
                            "8888 9999 0000 #### @@@@ $$$$ %%%%"));

            // Then
            assertThat(duplicateOnQ100.isRejected()).isTrue();
            assertThat(newOnQ200.isAccepted()).isTrue();
            assertThat(newOnQ300.isAccepted()).isTrue();
        }
    }
}
