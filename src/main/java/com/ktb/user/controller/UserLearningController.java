package com.ktb.user.controller;

import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.common.dto.ApiResponse;
import com.ktb.user.dto.response.LearningStatsResponse;
import com.ktb.user.dto.response.WeeklyStatsResponse;
import com.ktb.user.service.LearningRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "사용자 학습 API", description = "내 학습 통계 조회 API")
@RestController
@RequestMapping("/api/users/me")
@RequiredArgsConstructor
public class UserLearningController {

    private static final String MESSAGE_STATS_RETRIEVED = "stats_retrieval_success";
    private static final String MESSAGE_WEEKLY_STATS_RETRIEVED = "weekly_stats_retrieval_success";
    private static final String MESSAGE_UNAUTHORIZED = "unauthorized_request";

    private final LearningRecordService learningRecordService;

    @Operation(summary = "학습 통계 요약 조회", description = "내 학습 통계 요약 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    @GetMapping("/stats")
    public ResponseEntity<@NonNull ApiResponse<LearningStatsResponse>> getStats(
            @AuthenticationPrincipal SecurityUserAccount principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(MESSAGE_UNAUTHORIZED, null));
        }

        Long accountId = principal.getAccount().getId();
        LearningStatsResponse response = learningRecordService.getStats(accountId);

        return ResponseEntity.ok(new ApiResponse<>(MESSAGE_STATS_RETRIEVED, response));
    }

    @Operation(summary = "주간 학습 통계 조회", description = "이번 주(월~일) 일별 학습 통계를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    @GetMapping("/stats/weekly")
    public ResponseEntity<@NonNull ApiResponse<WeeklyStatsResponse>> getWeeklyStats(
            @AuthenticationPrincipal SecurityUserAccount principal
    ) {
        if (principal == null) {
            return ResponseEntity.status(401)
                    .body(new ApiResponse<>(MESSAGE_UNAUTHORIZED, null));
        }

        Long accountId = principal.getAccount().getId();
        WeeklyStatsResponse response = learningRecordService.getWeeklyStats(accountId);

        return ResponseEntity.ok(new ApiResponse<>(MESSAGE_WEEKLY_STATS_RETRIEVED, response));
    }
}
