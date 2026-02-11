package com.ktb.notification.controller;

import com.ktb.common.dto.ApiResponse;
import com.ktb.notification.dto.request.CampaignCreateRequest;
import com.ktb.notification.dto.response.CampaignResponse;
import com.ktb.notification.service.CampaignService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Campaign API", description = "캠페인 관리 API (관리자 전용)")
@RestController
@RequestMapping("/api/admin/campaigns")
@RequiredArgsConstructor
public class CampaignController {

    private final CampaignService campaignService;

    @GetMapping
    @Operation(summary = "캠페인 목록 조회", description = "전체 캠페인 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Page<CampaignResponse>>> getCampaigns(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CampaignResponse> result = campaignService.getAllCampaigns(pageable);
        return ResponseEntity.ok(new ApiResponse<>("campaigns_retrieval_success", result));
    }

    @GetMapping("/pending")
    @Operation(summary = "대기 중인 캠페인 조회", description = "대기 중이거나 실행 중인 캠페인 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<List<CampaignResponse>>> getPendingCampaigns() {
        List<CampaignResponse> result = campaignService.getPendingCampaigns();
        return ResponseEntity.ok(new ApiResponse<>("pending_campaigns_retrieval_success", result));
    }

    @GetMapping("/{campaignId}")
    @Operation(summary = "캠페인 상세 조회", description = "캠페인 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "캠페인 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<CampaignResponse>> getCampaign(
            @Parameter(description = "캠페인 ID", example = "1")
            @PathVariable Long campaignId
    ) {
        CampaignResponse result = campaignService.getCampaign(campaignId);
        return ResponseEntity.ok(new ApiResponse<>("campaign_retrieval_success", result));
    }

    @PostMapping
    @Operation(summary = "캠페인 생성", description = "새 캠페인을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "중복된 캠페인 키",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<CampaignResponse>> createCampaign(
            @Valid @RequestBody CampaignCreateRequest request
    ) {
        CampaignResponse result = campaignService.createCampaign(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("campaign_created_success", result));
    }

    @PostMapping("/{campaignId}/start")
    @Operation(summary = "캠페인 시작", description = "캠페인을 시작합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "시작 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 전이",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "캠페인 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<CampaignResponse>> startCampaign(
            @Parameter(description = "캠페인 ID", example = "1")
            @PathVariable Long campaignId
    ) {
        CampaignResponse result = campaignService.startCampaign(campaignId);
        return ResponseEntity.ok(new ApiResponse<>("campaign_started_success", result));
    }

    @PostMapping("/{campaignId}/complete")
    @Operation(summary = "캠페인 완료", description = "캠페인을 완료 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 전이",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "캠페인 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<CampaignResponse>> completeCampaign(
            @Parameter(description = "캠페인 ID", example = "1")
            @PathVariable Long campaignId
    ) {
        CampaignResponse result = campaignService.completeCampaign(campaignId);
        return ResponseEntity.ok(new ApiResponse<>("campaign_completed_success", result));
    }

    @PostMapping("/{campaignId}/cancel")
    @Operation(summary = "캠페인 취소", description = "캠페인을 취소합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "취소 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 전이",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "캠페인 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<CampaignResponse>> cancelCampaign(
            @Parameter(description = "캠페인 ID", example = "1")
            @PathVariable Long campaignId
    ) {
        CampaignResponse result = campaignService.cancelCampaign(campaignId);
        return ResponseEntity.ok(new ApiResponse<>("campaign_cancelled_success", result));
    }
}
