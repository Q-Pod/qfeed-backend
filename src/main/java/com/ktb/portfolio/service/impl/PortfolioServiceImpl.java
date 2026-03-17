package com.ktb.portfolio.service.impl;

import com.ktb.auth.exception.account.AccountNotFoundException;
import com.ktb.auth.repository.UserAccountRepository;
import com.ktb.common.domain.ErrorCode;
import com.ktb.file.domain.File;
import com.ktb.file.domain.FileCategory;
import com.ktb.file.domain.FileUploadStatus;
import com.ktb.file.dto.request.PresignedUrlMethod;
import com.ktb.file.dto.request.PresignedUrlRequest;
import com.ktb.file.exception.FileInvalidMetadataException;
import com.ktb.file.exception.FileNotFoundException;
import com.ktb.file.repository.FileRepository;
import com.ktb.file.service.S3PresignedUrlService;
import com.ktb.portfolio.domain.Portfolio;
import com.ktb.portfolio.domain.Project;
import com.ktb.portfolio.domain.ProjectTechStack;
import com.ktb.portfolio.dto.request.PortfolioProjectRequest;
import com.ktb.portfolio.dto.request.PortfolioUpsertRequest;
import com.ktb.portfolio.dto.response.PortfolioProjectResponse;
import com.ktb.portfolio.dto.response.PortfolioResponse;
import com.ktb.portfolio.dto.response.PortfolioTechStackResponse;
import com.ktb.portfolio.exception.PortfolioNotFoundException;
import com.ktb.portfolio.repository.PortfolioRepository;
import com.ktb.portfolio.service.PortfolioService;
import com.ktb.techstack.domain.TechStack;
import com.ktb.techstack.exception.TechStackNotFoundException;
import com.ktb.techstack.repository.TechStackRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PortfolioServiceImpl implements PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final UserAccountRepository userAccountRepository;
    private final FileRepository fileRepository;
    private final S3PresignedUrlService s3PresignedUrlService;
    private final TechStackRepository techStackRepository;

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
            File architectureImage = resolveArchitectureImage(projectRequest.architectureImageFileId());
            Project project = Project.create(
                    projectRequest.projectName(),
                    projectRequest.content(),
                    architectureImage
            );

            List<TechStack> techStacks = techStackRepository.findAllByIdInAndUseYnTrue(projectRequest.techStackIds());

            if (techStacks.size() != projectRequest.techStackIds().size()) {
                throw new TechStackNotFoundException();
            }

            techStacks.forEach(techStack ->
                    project.addTechStack(ProjectTechStack.create(techStack))
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
                        project.getArchitectureImage() == null ? null : project.getArchitectureImage().getId(),
                        resolveArchitectureImageUrl(project.getArchitectureImage()),
                        project.getStacks().stream()
                                .map(stack -> new PortfolioTechStackResponse(
                                        stack.getTechStack().getId(),
                                        stack.getTechStack().getName()
                                ))
                                .toList()
                ))
                .toList();

        return new PortfolioResponse(
                portfolio.getId(),
                projects
        );
    }

    private File resolveArchitectureImage(Long architectureImageFileId) {
        if (architectureImageFileId == null) {
            return null;
        }

        File file = fileRepository.findById(architectureImageFileId)
                .orElseThrow(() -> new FileNotFoundException(architectureImageFileId));

        if (file.isDeleted()) {
            throw new FileInvalidMetadataException("삭제된 파일은 아키텍처 이미지로 사용할 수 없습니다.");
        }
        if (file.getUploadStatus() != FileUploadStatus.UPLOADED) {
            throw new FileInvalidMetadataException(ErrorCode.FILE_NOT_UPLOADED);
        }
        if (file.getCategory() != FileCategory.ARCHITECTURE) {
            throw new FileInvalidMetadataException("ARCHITECTURE 카테고리 파일만 사용할 수 있습니다.");
        }
        return file;
    }

    private String resolveArchitectureImageUrl(File architectureImage) {
        if (architectureImage == null) {
            return null;
        }

        return s3PresignedUrlService.generatePresignedUrl(
                new PresignedUrlRequest(
                        null,
                        null,
                        null,
                        null,
                        PresignedUrlMethod.GET,
                        architectureImage.getId()
                )
        ).presignedUrl();
    }
}
