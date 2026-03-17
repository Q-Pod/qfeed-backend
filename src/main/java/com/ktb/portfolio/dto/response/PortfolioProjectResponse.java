package com.ktb.portfolio.dto.response;

import java.util.List;

public record PortfolioProjectResponse(
        Long projectId,
        String projectName,
        String content,
        Long architectureImageFileId,
        String architectureImageUrl,
        List<PortfolioTechStackResponse> techStacks
) {
}
