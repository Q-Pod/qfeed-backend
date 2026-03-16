package com.ktb.portfolio.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.Set;

public record PortfolioProjectRequest(
        @NotBlank
        @Size(max = 100)
        String projectName,

        @NotBlank
        String content,

        @Size(max = 255)
        String architectureImageUrl,

        @NotEmpty
        Set<@NotNull @Positive Long> techStackTagIds
) {
}
