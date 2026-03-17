package com.ktb.techstack.dto;

public record TechStackPaginationResponse(
        Long nextCursor,
        boolean hasNext,
        int size
) {
}
