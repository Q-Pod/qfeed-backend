package com.ktb.notification.controller;

import com.ktb.common.dto.ApiResponse;
import com.ktb.notification.dto.request.NoticeCreateRequest;
import com.ktb.notification.dto.request.NoticeUpdateRequest;
import com.ktb.notification.dto.response.NoticeResponse;
import com.ktb.notification.service.NoticeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Notice API", description = "공지사항 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping("/notices")
    @Operation(summary = "공지사항 목록 조회", description = "발행된 공지사항 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<Page<NoticeResponse>>> getPublishedNotices(
            @PageableDefault(size = 10, sort = "publishedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<NoticeResponse> result = noticeService.getPublishedNotices(pageable);
        return ResponseEntity.ok(new ApiResponse<>("notices_retrieval_success", result));
    }

    @GetMapping("/notices/{noticeId}")
    @Operation(summary = "공지사항 상세 조회", description = "공지사항 상세 정보를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<NoticeResponse>> getNotice(
            @Parameter(description = "공지사항 ID", example = "1")
            @PathVariable Long noticeId
    ) {
        NoticeResponse result = noticeService.getNotice(noticeId);
        return ResponseEntity.ok(new ApiResponse<>("notice_retrieval_success", result));
    }

    @GetMapping("/admin/notices")
    @Operation(summary = "[관리자] 전체 공지사항 조회", description = "모든 공지사항(초안 포함)을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Page<NoticeResponse>>> getAllNotices(
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<NoticeResponse> result = noticeService.getAllNotices(pageable);
        return ResponseEntity.ok(new ApiResponse<>("notices_retrieval_success", result));
    }

    @PostMapping("/admin/notices")
    @Operation(summary = "[관리자] 공지사항 작성", description = "새 공지사항을 작성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "작성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<NoticeResponse>> createNotice(
            @Valid @RequestBody NoticeCreateRequest request
    ) {
        NoticeResponse result = noticeService.createNotice(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("notice_created_success", result));
    }

    @PutMapping("/admin/notices/{noticeId}")
    @Operation(summary = "[관리자] 공지사항 수정", description = "공지사항을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<NoticeResponse>> updateNotice(
            @Parameter(description = "공지사항 ID", example = "1")
            @PathVariable Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request
    ) {
        NoticeResponse result = noticeService.updateNotice(noticeId, request);
        return ResponseEntity.ok(new ApiResponse<>("notice_updated_success", result));
    }

    @PostMapping("/admin/notices/{noticeId}/publish")
    @Operation(summary = "[관리자] 공지사항 발행", description = "공지사항을 발행합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "발행 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 상태 전이",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<NoticeResponse>> publishNotice(
            @Parameter(description = "공지사항 ID", example = "1")
            @PathVariable Long noticeId
    ) {
        NoticeResponse result = noticeService.publishNotice(noticeId);
        return ResponseEntity.ok(new ApiResponse<>("notice_published_success", result));
    }

    @DeleteMapping("/admin/notices/{noticeId}")
    @Operation(summary = "[관리자] 공지사항 삭제", description = "공지사항을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "삭제 불가",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "공지사항 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @Parameter(description = "공지사항 ID", example = "1")
            @PathVariable Long noticeId
    ) {
        noticeService.deleteNotice(noticeId);
        return ResponseEntity.ok(new ApiResponse<>("notice_deleted_success", null));
    }
}
