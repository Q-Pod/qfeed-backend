package com.ktb.portfolio.domain;

import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.techstack.domain.TechStack;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
        name = "PROJECT_TECH_STACK",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_project_tech_stack", columnNames = {"project_id", "tech_stack_id"})
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTechStack extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @jakarta.persistence.Column(name = "project_tech_stack_id")
    private Long id;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = jakarta.persistence.FetchType.LAZY)
    @JoinColumn(name = "tech_stack_id", nullable = false)
    private TechStack techStack;

    @Builder
    private ProjectTechStack(TechStack techStack) {
        this.techStack = techStack;
    }

    public static ProjectTechStack create(TechStack techStack) {
        return ProjectTechStack.builder()
                .techStack(techStack)
                .build();
    }

    public void assignProject(Project project) {
        this.project = project;
    }
}
