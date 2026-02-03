package com.ktb.hashtag.domain;

import com.ktb.hashtag.exception.HashtagNameInvalidLengthException;
import com.ktb.hashtag.exception.HashtagNameRequiredException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Hashtag 도메인 테스트")
class HashtagTest {

    private static final int MAX_NAME_LENGTH = 100;

    @Nested
    @DisplayName("Hashtag 생성 테스트")
    class CreateTest {

        @Test
        @DisplayName("유효한 이름으로 Hashtag 생성 성공")
        void create_WithValidName_ShouldSucceed() {
            // Given
            String name = "자바";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag).isNotNull();
            assertThat(hashtag.getName()).isEqualTo("자바");
            assertThat(hashtag.getDescription()).isNull();
        }

        @Test
        @DisplayName("설명과 함께 Hashtag 생성 성공")
        void createWithDescription_ShouldSucceed() {
            // Given
            String name = "자바";
            String description = "자바 프로그래밍 언어";

            // When
            Hashtag hashtag = Hashtag.createWithDescription(name, description);

            // Then
            assertThat(hashtag.getName()).isEqualTo("자바");
            assertThat(hashtag.getDescription()).isEqualTo(description);
        }

        @Test
        @DisplayName("대문자 이름은 소문자로 정규화")
        void create_WithUpperCase_ShouldNormalizeToLowerCase() {
            // Given
            String name = "JAVA";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("java");
        }

        @Test
        @DisplayName("혼합 대소문자 이름은 소문자로 정규화")
        void create_WithMixedCase_ShouldNormalizeToLowerCase() {
            // Given
            String name = "JavaScript";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("javascript");
        }

        @Test
        @DisplayName("1자 이름으로 생성 성공")
        void create_WithOneCharacter_ShouldSucceed() {
            // Given
            String name = "A";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("a");
        }

        @Test
        @DisplayName("100자 이름으로 생성 성공")
        void create_WithMaxLength_ShouldSucceed() {
            // Given
            String name = "A".repeat(MAX_NAME_LENGTH);

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).hasSize(MAX_NAME_LENGTH);
        }

        @Test
        @DisplayName("101자 이름으로 생성 시 HashtagNameInvalidLengthException 발생")
        void create_WithExceedingMaxLength_ShouldThrowException() {
            // Given
            String name = "A".repeat(MAX_NAME_LENGTH + 1);

            // When & Then
            assertThatThrownBy(() -> Hashtag.create(name))
                    .isInstanceOf(HashtagNameInvalidLengthException.class);
        }

        @Test
        @DisplayName("null 이름으로 생성 시 HashtagNameRequiredException 발생")
        void create_WithNull_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> Hashtag.create(null))
                    .isInstanceOf(HashtagNameRequiredException.class);
        }

        @Test
        @DisplayName("빈 문자열 이름으로 생성 시 HashtagNameRequiredException 발생")
        void create_WithEmptyString_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> Hashtag.create(""))
                    .isInstanceOf(HashtagNameRequiredException.class);
        }

        @Test
        @DisplayName("공백만 있는 이름으로 생성 시 HashtagNameRequiredException 발생")
        void create_WithOnlySpaces_ShouldThrowException() {
            // When & Then
            assertThatThrownBy(() -> Hashtag.create("   "))
                    .isInstanceOf(HashtagNameRequiredException.class);
        }
    }

    @Nested
    @DisplayName("특수 문자 및 다국어 테스트")
    class SpecialCharacterTest {

        @Test
        @DisplayName("특수문자가 포함된 이름 생성 가능")
        void create_WithSpecialCharacters_ShouldSucceed() {
            // Given
            String name = "C++";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("c++");
        }

        @Test
        @DisplayName("하이픈이 포함된 이름 생성 가능")
        void create_WithHyphen_ShouldSucceed() {
            // Given
            String name = "spring-boot";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("spring-boot");
        }

        @Test
        @DisplayName("언더스코어가 포함된 이름 생성 가능")
        void create_WithUnderscore_ShouldSucceed() {
            // Given
            String name = "snake_case";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("snake_case");
        }

        @Test
        @DisplayName("숫자가 포함된 이름 생성 가능")
        void create_WithNumbers_ShouldSucceed() {
            // Given
            String name = "Java8";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("java8");
        }

        @Test
        @DisplayName("한글과 영어가 혼합된 이름 생성 가능")
        void create_WithKoreanAndEnglish_ShouldSucceed() {
            // Given
            String name = "자바JAVA";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("자바java");
        }

        @Test
        @DisplayName("한글만 있는 이름 생성 가능")
        void create_WithKoreanOnly_ShouldSucceed() {
            // Given
            String name = "자바프로그래밍";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("자바프로그래밍");
        }

        @Test
        @DisplayName("일본어 이름 생성 가능")
        void create_WithJapanese_ShouldSucceed() {
            // Given
            String name = "プログラミング";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("プログラミング");
        }
    }

    @Nested
    @DisplayName("설명 업데이트 테스트")
    class UpdateDescriptionTest {

        @Test
        @DisplayName("설명 업데이트 성공")
        void updateDescription_WithValidDescription_ShouldSucceed() {
            // Given
            Hashtag hashtag = Hashtag.create("java");
            String newDescription = "자바 프로그래밍 언어";

            // When
            hashtag.updateDescription(newDescription);

            // Then
            assertThat(hashtag.getDescription()).isEqualTo(newDescription);
        }

        @Test
        @DisplayName("설명을 null로 업데이트 가능")
        void updateDescription_WithNull_ShouldSucceed() {
            // Given
            Hashtag hashtag = Hashtag.createWithDescription("java", "기존 설명");

            // When
            hashtag.updateDescription(null);

            // Then
            assertThat(hashtag.getDescription()).isNull();
        }

        @Test
        @DisplayName("설명을 빈 문자열로 업데이트 가능")
        void updateDescription_WithEmptyString_ShouldSucceed() {
            // Given
            Hashtag hashtag = Hashtag.createWithDescription("java", "기존 설명");

            // When
            hashtag.updateDescription("");

            // Then
            assertThat(hashtag.getDescription()).isEmpty();
        }

        @Test
        @DisplayName("255자 설명 업데이트 가능")
        void updateDescription_WithMaxLength_ShouldSucceed() {
            // Given
            Hashtag hashtag = Hashtag.create("java");
            String longDescription = "A".repeat(255);

            // When
            hashtag.updateDescription(longDescription);

            // Then
            assertThat(hashtag.getDescription()).hasSize(255);
        }
    }

    @Nested
    @DisplayName("정규화 테스트")
    class NormalizationTest {

        @Test
        @DisplayName("동일한 이름(대소문자 다름)은 정규화 후 같은 값")
        void normalize_WithDifferentCase_ShouldBeSameAfterNormalization() {
            // Given
            String name1 = "Java";
            String name2 = "JAVA";
            String name3 = "java";

            // When
            Hashtag hashtag1 = Hashtag.create(name1);
            Hashtag hashtag2 = Hashtag.create(name2);
            Hashtag hashtag3 = Hashtag.create(name3);

            // Then
            assertThat(hashtag1.getName())
                    .isEqualTo(hashtag2.getName())
                    .isEqualTo(hashtag3.getName())
                    .isEqualTo("java");
        }

        @Test
        @DisplayName("소문자 변환 확인")
        void normalize_ShouldConvertToLowerCase() {
            // Given
            String name = "JavaScript";

            // When
            Hashtag hashtag = Hashtag.create(name);

            // Then
            assertThat(hashtag.getName()).isEqualTo("javascript");
        }
    }
}
