package com.ktb.portfolio.domain;

import com.ktb.common.domain.BaseSoftDeleteEntity;
import com.ktb.file.domain.File;
import com.ktb.portfolio.exception.TechStackRequiredException;
import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
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

    @Size(max = 100)
    @Column(name = "project_name", nullable = false, length = 100)
    private String projectName;

    @Size(max = 1000)
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "architecture_file_id")
    private File architectureImage;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectTechStack> stacks = new ArrayList<>();

    @Builder
    private Project(String projectName, String content, File architectureImage) {
        this.projectName = projectName;
        this.content = content;
        this.architectureImage = architectureImage;
    }

    public static Project create(String projectName, String content, File architectureImage) {
        return Project.builder()
                .projectName(projectName)
                .content(content)
                .architectureImage(architectureImage)
                .build();
    }

    public void assignPortfolio(Portfolio portfolio) {
        this.portfolio = portfolio;
    }

    public void updateContent(String content) {
        this.content = content;
    }

    public void updateArchitectureImage(File architectureImage) {
        this.architectureImage = architectureImage;
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
