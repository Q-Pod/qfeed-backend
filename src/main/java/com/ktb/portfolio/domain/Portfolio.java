package com.ktb.portfolio.domain;

import com.ktb.auth.domain.UserAccount;
import com.ktb.common.domain.BaseSoftDeleteEntity;
import com.ktb.portfolio.exception.ProjectRequiredException;
import com.ktb.portfolio.exception.PortfolioProjectLimitExceededException;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "PORTFOLIO"
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Portfolio extends BaseSoftDeleteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private UserAccount account;

    @OneToMany(mappedBy = "portfolio", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Project> projects = new ArrayList<>();


    public void addProject(Project project) {
        if (project == null) {
            throw new ProjectRequiredException();
        }
        if (this.projects.size() >= 3) {
            throw new PortfolioProjectLimitExceededException();
        }
        this.projects.add(project);
        project.assignPortfolio(this);
    }

    public static Portfolio create(UserAccount userAccount) {
        Portfolio portfolio = new Portfolio();
        portfolio.account = userAccount;
        return portfolio;
    }

    public void clearProjects() {
        for (Project project : projects) {
            project.assignPortfolio(null);
        }

        projects.clear();
    }
}
