package com.ktb.portfolio.dto.response;

import java.util.List;

public record PortfolioResponse(
        Long portfolioId,
        List<PortfolioProjectResponse> projects
) {
}
