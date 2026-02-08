package com.ktb.answer.dto.response.list;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "페이지네이션 정보")
public record PaginationInfo(
    @Schema(description = "페이지 크기", example = "10")
    int limit,

    @Schema(description = "다음 페이지 존재 여부", example = "true")
    boolean hasNext,

    @Schema(description = "다음 페이지 커서 (hasNext=true 시에만 제공)",
        example = "eyJsYXN0X2NyZWF0ZWRfYXQiOiIyMDI2LTAxLTIyVDEwOjAwOjAwIiwibGFzdF9hbnN3ZXJfaWQiOjEwfQ==")
    String nextCursor
) {
}
