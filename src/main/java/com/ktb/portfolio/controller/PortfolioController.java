package com.ktb.portfolio.controller;

import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.common.dto.ApiResponse;
import com.ktb.common.dto.CommonErrorResponse;
import com.ktb.portfolio.dto.request.PortfolioUpsertRequest;
import com.ktb.portfolio.dto.response.PortfolioDeleteResponse;
import com.ktb.portfolio.dto.response.PortfolioResponse;
import com.ktb.portfolio.service.PortfolioService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Portfolio API", description = "포트폴리오/프로젝트 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/portfolio")
@Validated
@Slf4j
public class PortfolioController {

    private final PortfolioService portfolioService;

    private static final String PORTFOLIO_RETRIEVED = "Portfolio retrieved successfully.";
    private static final String PORTFOLIO_UPDATED = "Portfolio updated successfully.";
    private static final String PORTFOLIO_DELETED = "Portfolio deleted successfully";

    @GetMapping("/me")
    @Operation(summary = "내 포트폴리오 조회", description = "JWT의 accountId 기준으로 내 포트폴리오를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "포트폴리오를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<PortfolioResponse>> getMyPortfolio(
            @AuthenticationPrincipal SecurityUserAccount principal
    ) {
        Long accountId = principal.getAccount().getId();
        log.info("GET /api/portfolio/me - accountId: {}", accountId);
        PortfolioResponse response = portfolioService.getMyPortfolio(accountId);
        log.info("GET /api/portfolio/me success - accountId: {}, projectCount: {}",
                accountId, response.projects().size());
        return ResponseEntity.ok(
                new ApiResponse<>(PORTFOLIO_RETRIEVED, response)
        );
    }

    @PutMapping("/me")
    @Operation(summary = "내 포트폴리오 전체 교체", description = "JWT의 accountId 기준으로 내 포트폴리오 프로젝트 목록 전체를 교체합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "교체 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 또는 프로젝트 개수 초과",
                    content = @Content(schema = @Schema(implementation = CommonErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "존재하지 않는 기술 스택 또는 계정을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<PortfolioResponse>> upsertMyPortfolio(
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Valid @RequestBody PortfolioUpsertRequest request
    ) {
        Long accountId = principal.getAccount().getId();
        log.info("PUT /api/portfolio/me - accountId: {}, projectCount: {}",
                accountId, request.projects().size());
        PortfolioResponse response = portfolioService.upsertMyPortfolio(accountId, request);
        log.info("PUT /api/portfolio/me success - accountId: {}, portfolioId: {}, projectCount: {}",
                accountId, response.portfolioId(), response.projects().size());
        return ResponseEntity.ok(
                new ApiResponse<>(PORTFOLIO_UPDATED, response)
        );
    }

    @DeleteMapping("/me")
    @Operation(summary = "내 포트폴리오 삭제", description = "JWT의 accountId 기준으로 내 포트폴리오를 soft delete 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "포트폴리오를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<PortfolioDeleteResponse>> deleteMyPortfolio(
            @AuthenticationPrincipal SecurityUserAccount principal
    ) {
        Long accountId = principal.getAccount().getId();
        log.info("DELETE /api/portfolio/me - accountId: {}", accountId);
        Long portfolioId = portfolioService.deleteMyPortfolio(accountId);
        log.info("DELETE /api/portfolio/me success - accountId: {}, portfolioId: {}",
                accountId, portfolioId);
        return ResponseEntity.ok(
                new ApiResponse<>(PORTFOLIO_DELETED, new PortfolioDeleteResponse(portfolioId))
        );
    }
}
