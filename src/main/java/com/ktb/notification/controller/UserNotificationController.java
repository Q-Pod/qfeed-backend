package com.ktb.notification.controller;

import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.common.dto.ApiResponse;
import com.ktb.notification.dto.response.UnreadCountResponse;
import com.ktb.notification.dto.response.UserNotificationResponse;
import com.ktb.notification.service.UserNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "User Notification API", description = "사용자 알림 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class UserNotificationController {

    private final UserNotificationService userNotificationService;

    @GetMapping
    @Operation(summary = "알림 목록 조회", description = "내 알림 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Page<UserNotificationResponse>>> getNotifications(
            @AuthenticationPrincipal SecurityUserAccount principal,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<UserNotificationResponse> result = userNotificationService.getNotifications(
                principal.getAccount().getId(),
                pageable
        );
        return ResponseEntity.ok(new ApiResponse<>("notifications_retrieval_success", result));
    }

    @GetMapping("/unread")
    @Operation(summary = "안읽은 알림 여부 조회", description = "안읽은 알림 여부를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount(
            @AuthenticationPrincipal SecurityUserAccount principal
    ) {
        UnreadCountResponse result = userNotificationService.getUnreadCount(
                principal.getAccount().getId()
        );
        return ResponseEntity.ok(new ApiResponse<>("unread_count_retrieval_success", result));
    }

    @PatchMapping("/{notificationId}")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "알림 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<UserNotificationResponse>> markAsRead(
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Parameter(description = "알림 ID", example = "1")
            @PathVariable Long notificationId
    ) {
        UserNotificationResponse result = userNotificationService.markAsRead(
                principal.getAccount().getId(),
                notificationId
        );
        return ResponseEntity.ok(new ApiResponse<>("notification_read_success", result));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "전체 알림 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "읽음 처리 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Integer>> markAllAsRead(
            @AuthenticationPrincipal SecurityUserAccount principal
    ) {
        int count = userNotificationService.markAllAsRead(principal.getAccount().getId());
        return ResponseEntity.ok(new ApiResponse<>("all_notifications_read_success", count));
    }
}
