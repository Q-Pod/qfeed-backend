package com.ktb.techstack.domain;

import com.ktb.common.domain.BaseUsableEntity;
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

@Entity
@Table(
        name = "TECH_STACK",
        indexes = {
                @Index(name = "uk_tech_stack_nm", columnList = "tech_stack_nm", unique = true),
                @Index(name = "idx_tech_stack_use_yn", columnList = "use_yn")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TechStack extends BaseUsableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "tech_stack_id")
    private Long id;

    @Column(name = "tech_stack_nm", nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "tech_stack_desc", length = 255)
    private String description;

    @Builder
    private TechStack(String name, String description) {
        this.name = name;
        this.description = description;
    }
}
