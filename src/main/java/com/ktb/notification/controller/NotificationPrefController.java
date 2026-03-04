package com.ktb.notification.controller;

import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.common.dto.ApiResponse;
import com.ktb.notification.domain.enums.NotificationTypeCd;
import com.ktb.notification.dto.request.NotificationPrefUpdateRequest;
import com.ktb.notification.dto.response.NotificationPrefResponse;
import com.ktb.notification.service.NotificationPrefService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notification Preference API", description = "알림 수신 설정 API")
@RestController
@RequestMapping("/api/notifications/preferences")
@RequiredArgsConstructor
public class NotificationPrefController {

    private final NotificationPrefService notificationPrefService;

    @GetMapping
    @Operation(summary = "알림 수신 설정 조회", description = "내 알림 수신 설정 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<NotificationPrefResponse>>> getPreferences(
            @AuthenticationPrincipal SecurityUserAccount principal
    ) {
        List<NotificationPrefResponse> result = notificationPrefService.getPreferences(
                principal.getAccount().getId()
        );
        return ResponseEntity.ok(new ApiResponse<>("preferences_retrieval_success", result));
    }

    @PutMapping("/{type}")
    @Operation(summary = "알림 수신 설정 변경", description = "특정 알림 유형의 수신 설정을 변경합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<NotificationPrefResponse>> updatePreference(
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Parameter(description = "알림 유형", example = "NOTICE")
            @PathVariable NotificationTypeCd type,
            @Valid @RequestBody NotificationPrefUpdateRequest request
    ) {
        NotificationPrefResponse result = notificationPrefService.updatePreference(
                principal.getAccount().getId(),
                type,
                request.enabled()
        );
        return ResponseEntity.ok(new ApiResponse<>("preference_update_success", result));
    }

    @PutMapping("/admin/all")
    @Operation(summary = "모든 유저의 수신 설정 등록", description = "특정 알림 유형의 수신 설정을 변경합니다.")
    @ApiResponses({
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변경 성공"),
        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
            content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<NotificationPrefResponse>> insertAllUserPreference(
    ) {
        notificationPrefService.insertDefaultPreferencesForAllActiveUsers();
        return ResponseEntity.ok(new ApiResponse<>("preference_update_success", null));
    }
}
