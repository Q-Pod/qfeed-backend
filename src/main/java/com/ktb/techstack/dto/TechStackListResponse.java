package com.ktb.techstack.dto;

import java.util.List;

public record TechStackListResponse(
        List<TechStackResponse> techStacks,
        TechStackPaginationResponse pagination
) {
}
