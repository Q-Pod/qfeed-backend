package com.ktb.portfolio.domain;

import com.ktb.common.domain.BaseSoftDeleteEntity;
import com.ktb.hashtag.domain.ProjectTechStack;
import com.ktb.portfolio.exception.TechStackRequiredException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "PROJECT")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Project extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "portfolio_id", nullable = false)
    private Portfolio portfolio;

    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "architecture_image_url")
    private String architectureImageUrl;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTechStack> stacks = new ArrayList<>();

    @Builder
    private Project(String projectName, String content, String architectureImageUrl) {
        this.projectName = projectName;
        this.content = content;
        this.architectureImageUrl = architectureImageUrl;

    }

    public static Project create(String projectName, String content, String architectureImageUrl) {
        return Project.builder()
                .projectName(projectName)
                .content(content)
                .architectureImageUrl(architectureImageUrl)
                .build();
    }

    public void assignPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateArchitectureImageUrl(String architectureImageUrl) {
        this.architectureImageUrl = architectureImageUrl;
    }

    public void addTechStack(ProjectTechStack techStack) {
        if (techStack == null) {
            throw new TechStackRequiredException();
        }
        this.stacks.add(techStack);
        techStack.assignProject(this);
    }

    public void clearTechStacks() {
        for (ProjectTechStack stack : stacks) {
            stack.assignProject(null);
        }

        stacks.clear();
    }
}
