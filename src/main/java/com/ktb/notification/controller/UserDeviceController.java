package com.ktb.notification.controller;

import com.ktb.auth.security.abstraction.AuthenticatedUser;
import com.ktb.common.dto.ApiResponse;
import com.ktb.notification.dto.request.DeviceRegisterRequest;
import com.ktb.notification.dto.response.UserDeviceResponse;
import com.ktb.notification.service.UserDeviceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Device API", description = "디바이스 관리 API")
@RestController
@RequestMapping("/api/devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    @PostMapping
    @Operation(summary = "디바이스 등록", description = "디바이스 플랫폼을 등록합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<UserDeviceResponse>> registerDevice(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Valid @RequestBody DeviceRegisterRequest request
    ) {
        UserDeviceResponse result = userDeviceService.registerDevice(user.getUserId(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("device_registered_success", result));
    }

    @GetMapping
    @Operation(summary = "내 디바이스 목록", description = "내 디바이스 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<List<UserDeviceResponse>>> getMyDevices(
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        List<UserDeviceResponse> result = userDeviceService.getMyDevices(user.getUserId());
        return ResponseEntity.ok(new ApiResponse<>("devices_retrieval_success", result));
    }

    @DeleteMapping("/{deviceId}")
    @Operation(summary = "디바이스 삭제", description = "디바이스를 비활성화합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "디바이스 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> deactivateDevice(
            @AuthenticationPrincipal AuthenticatedUser user,
            @Parameter(description = "디바이스 ID", example = "1")
            @PathVariable Long deviceId
    ) {
        userDeviceService.deactivateDevice(user.getUserId(), deviceId);
        return ResponseEntity.ok(new ApiResponse<>("device_deactivated_success", null));
    }
}
