package com.ktb.hashtag.domain;

import com.ktb.common.domain.BaseUsableEntity;
import com.ktb.hashtag.exception.HashtagNameInvalidLengthException;
import com.ktb.hashtag.exception.HashtagNameRequiredException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "HASHTAG",
        indexes = {
                @Index(name = "uk_tag_nm", columnList = "tag_nm", unique = true),
                @Index(name = "idx_use_yn", columnList = "use_yn"),
                @Index(name = "idx_created_at", columnList = "created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Hashtag extends BaseUsableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tag_id")
    private Long id;

    @Column(name = "tag_nm", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "tag_desc", length = 255)
    private String description;

    @Builder
    private Hashtag(String name, String description) {
        validateName(name);
        this.name = normalizeHashtagName(name);
        this.description = description;
    }

    public static Hashtag create(String name) {
        return Hashtag.builder()
                .name(name)
                .build();
    }

    public static Hashtag createWithDescription(String name, String description) {
        return Hashtag.builder()
                .name(name)
                .description(description)
                .build();
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    private String normalizeHashtagName(String name) {
        return name.trim().toLowerCase();
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new HashtagNameRequiredException();
        }
        if (name.trim().length() > 100) {
            throw new HashtagNameInvalidLengthException();
        }
    }
}
