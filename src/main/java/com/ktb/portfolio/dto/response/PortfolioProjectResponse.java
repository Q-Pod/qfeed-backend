package com.ktb.portfolio.dto.response;

import java.util.List;

public record PortfolioProjectResponse(
        Long projectId,
        String projectName,
        String content,
        String architectureImageUrl,
        List<PortfolioTechStackResponse> techStacks
) {
}
