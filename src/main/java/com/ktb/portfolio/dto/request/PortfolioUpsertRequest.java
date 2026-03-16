package com.ktb.portfolio.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record PortfolioUpsertRequest(
        @NotNull
        @Size(max = 3)
        List<@Valid PortfolioProjectRequest> projects
) {
}
