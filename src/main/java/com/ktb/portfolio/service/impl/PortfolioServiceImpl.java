package com.ktb.portfolio.service.impl;

import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.hashtag.domain.Hashtag;
import com.ktb.hashtag.domain.ProjectTechStack;
import com.ktb.hashtag.exception.HashtagNotFoundException;
import com.ktb.hashtag.repository.HashtagRepository;
import com.ktb.portfolio.domain.Portfolio;
import com.ktb.portfolio.domain.Project;
import com.ktb.portfolio.dto.request.PortfolioProjectRequest;
import com.ktb.portfolio.dto.request.PortfolioUpsertRequest;
import com.ktb.portfolio.dto.response.PortfolioProjectResponse;
import com.ktb.portfolio.dto.response.PortfolioResponse;
import com.ktb.portfolio.dto.response.PortfolioTechStackResponse;
import com.ktb.portfolio.exception.PortfolioNotFoundException;
import com.ktb.portfolio.repository.PortfolioRepository;
import com.ktb.portfolio.service.PortfolioService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserAccountRepository userAccountRepository;
    private final HashtagRepository hashtagRepository;

    @Override
    @Transactional(readOnly = true)
    public PortfolioResponse getMyPortfolio(Long accountId) {
        Portfolio portfolio = portfolioRepository.findWithProjectsByAccount_IdAndDeletedAtIsNull(accountId)
                .orElseThrow(PortfolioNotFoundException::new);
        return toResponse(portfolio);
    }

    @Override
    @Transactional
    public PortfolioResponse upsertMyPortfolio(Long accountId, PortfolioUpsertRequest request) {
        Portfolio portfolio = portfolioRepository.findWithProjectsByAccount_Id(accountId)
                .map(this::restoreIfDeleted)
                .orElseGet(() -> portfolioRepository.save(Portfolio.create(
                        userAccountRepository.findById(accountId)
                                .orElseThrow(() -> new AccountNotFoundException(accountId))
                )));

        portfolio.clearProjects();

        for (PortfolioProjectRequest projectRequest : request.projects()) {
            Project project = Project.create(
                    projectRequest.projectName(),
                    projectRequest.content(),
                    projectRequest.architectureImageUrl()
            );

            List<Hashtag> hashtags = hashtagRepository.findAllById(projectRequest.techStackTagIds());

            if (hashtags.size() != projectRequest.techStackTagIds().size()) {
                throw new HashtagNotFoundException();
            }

            hashtags.forEach(hashtag ->
                    project.addTechStack(ProjectTechStack.create(hashtag))
            );
            portfolio.addProject(project);
        }
        return toResponse(portfolio);
    }

    @Override
    @Transactional
    public Long deleteMyPortfolio(Long accountId) {
        Portfolio portfolio = portfolioRepository.findByAccount_IdAndDeletedAtIsNull(accountId)
                .orElseThrow(PortfolioNotFoundException::new);

        portfolio.softDelete();
        return portfolio.getId();
    }

    private Portfolio restoreIfDeleted(Portfolio portfolio) {
        if (portfolio.isDeleted()) {
            portfolio.restore();
        }
        return portfolio;
    }

    private PortfolioResponse toResponse(Portfolio portfolio) {
        List<PortfolioProjectResponse> projects = portfolio.getProjects().stream()
                .map(project -> new PortfolioProjectResponse(
                        project.getId(),
                        project.getProjectName(),
                        project.getContent(),
                        project.getArchitectureImageUrl(),
                        project.getStacks().stream()
                                .map(stack -> new PortfolioTechStackResponse(
                                        stack.getHashtag().getId(),
                                        stack.getHashtag().getName()
                                ))
                                .toList()
                ))
                .toList();

        return new PortfolioResponse(
                portfolio.getId(),
                projects
        );
    }
}
