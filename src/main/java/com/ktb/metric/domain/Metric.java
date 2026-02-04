package com.ktb.metric.domain;

import com.ktb.common.domain.BaseUsableEntity;
import com.ktb.metric.exception.MetricNameInvalidLengthException;
import com.ktb.metric.exception.MetricRequiredNameException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "METRIC",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_metric_name", columnNames = "metric_nm")
        },
        indexes = {
                @Index(name = "idx_use_yn", columnList = "metric_use_yn"),
                @Index(name = "idx_created", columnList = "metric_created_at")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class Metric extends BaseUsableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "metric_id")
    private Long id;

    @Column(name = "metric_nm", nullable = false, length = MAX_METRIC_NAME_SIZE)
    private String name;

    @Column(name = "metric_desc", length = 255)
    private String description;

    private final static int MAX_METRIC_NAME_SIZE = 100;

    @Builder
    private Metric(String name, String description) {
        validateName(name);
        this.name = name;
        this.description = description;
    }

    public static Metric create(String name, String description) {
        return Metric.builder()
                .name(name)
                .description(description)
                .build();
    }

    public void updateDescription(String description) {
        this.description = description;
    }

    public void updateName(String name) {
        validateName(name);
        this.name = name;
    }

    private void validateName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new MetricRequiredNameException();
        }
        if (name.length() > MAX_METRIC_NAME_SIZE) {
            throw new MetricNameInvalidLengthException();
        }
    }
}
