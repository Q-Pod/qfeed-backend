package com.ktb.hashtag.domain;

import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.portfolio.domain.Project;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "TECH_STACK",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tech_stack_project_tag", columnNames = {"project_id", "tag_id"})
        })

@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProjectTechStack extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tag_id", nullable = false)
    private Hashtag hashtag;

    @Builder
    private ProjectTechStack(Hashtag hashtag) {
        this.hashtag = hashtag;
    }

    public static ProjectTechStack create(Hashtag hashtag) {
        return ProjectTechStack.builder()
                .hashtag(hashtag)
                .build();
    }

    public void assignProject(Project project) {
        this.project = project;
    }
}
