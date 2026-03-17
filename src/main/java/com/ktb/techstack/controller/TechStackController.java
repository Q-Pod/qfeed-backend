package com.ktb.techstack.controller;

import com.ktb.common.dto.ApiResponse;
import com.ktb.common.dto.CommonErrorResponse;
import com.ktb.techstack.dto.TechStackListResponse;
import com.ktb.techstack.service.TechStackService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Tech Stack API", description = "기술 스택 마스터 조회 API")
@RestController
@RequestMapping("/api/tech-stacks")
@RequiredArgsConstructor
@Validated
@Slf4j
public class TechStackController {

    private static final String TECH_STACKS_RETRIEVED = "tech_stacks_retrieval_success";

    private final TechStackService techStackService;

    @GetMapping
    @Operation(summary = "기술 스택 검색", description = "포트폴리오 입력에 사용할 활성 기술 스택 목록을 검색하거나 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<TechStackListResponse>> getTechStacks(
            @Parameter(description = "기술 스택 검색어") @RequestParam(required = false, name = "q") String keyword,
            @Parameter(description = "커서") @RequestParam(required = false) Long cursor,
            @Parameter(description = "사이즈", example = "20")
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size
    ) {
        log.info("GET /api/tech-stacks - keyword: {}, cursor: {}, size: {}", keyword, cursor, size);
        TechStackListResponse response = techStackService.getTechStacks(keyword, cursor, size);
        log.info("GET /api/tech-stacks success - count: {}, hasNext: {}",
                response.techStacks().size(), response.pagination().hasNext());
        return ResponseEntity.ok(new ApiResponse<>(TECH_STACKS_RETRIEVED, response));
    }
}
