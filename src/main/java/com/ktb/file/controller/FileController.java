package com.ktb.file.controller;

import com.ktb.common.dto.ApiResponse;
import com.ktb.file.dto.request.MultipartPartPresignedUrlRequest;
import com.ktb.file.dto.request.MultipartUploadCompleteRequest;
import com.ktb.file.dto.request.PresignedUrlRequest;
import com.ktb.file.dto.response.FileUploadConfirmResponse;
import com.ktb.file.dto.response.MultipartPartPresignedUrlResponse;
import com.ktb.file.dto.response.MultipartUploadAbortResponse;
import com.ktb.file.dto.response.PresignedUrlResponse;
import com.ktb.file.service.S3PresignedUrlService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "File API", description = "파일 업로드 관리 API")
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Slf4j
public class FileController {

    private static final String MESSAGE_PRESIGNED_URL_GENERATED = "presigned_url_generated";
    private static final String MESSAGE_MULTIPART_PART_URL_GENERATED = "multipart_part_presigned_url_generated";
    private static final String MESSAGE_MULTIPART_UPLOAD_COMPLETED = "multipart_upload_completed";
    private static final String MESSAGE_MULTIPART_UPLOAD_ABORTED = "multipart_upload_aborted";
    private static final String MESSAGE_UPLOAD_CONFIRMED = "upload_confirmed";

    private final S3PresignedUrlService s3PresignedUrlService;

    @PostMapping("/presigned-url")
    @Operation(
            summary = "Presigned URL 생성",
            description = "카테고리 기반으로 S3 업로드용 URL/메타를 발급합니다. " +
                    "VIDEO는 multipart 시작 정보를, 그 외 카테고리는 단일 PUT presigned URL을 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "Presigned URL 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (파일 크기 초과, 허용되지 않은 파일 형식 등)",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<PresignedUrlResponse>> generatePresignedUrl(
            @Valid @RequestBody PresignedUrlRequest request
    ) {
        log.info("POST /api/files/presigned-url - method={}, category={}, fileId={}, fileSize={}",
                request.method(), request.category(), request.fileId(), request.fileSize());
        PresignedUrlResponse response = s3PresignedUrlService.generatePresignedUrl(request);
        return ResponseEntity.ok(
                new ApiResponse<>(MESSAGE_PRESIGNED_URL_GENERATED, response)
        );
    }

    @PostMapping("/{fileId}/multipart/parts")
    @Operation(
            summary = "Multipart 파트 Presigned URL 생성",
            description = "VIDEO multipart 업로드의 특정 part_number에 대한 업로드 URL을 생성합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (part_number 오류, multipart 시작 전 등)",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파일을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<MultipartPartPresignedUrlResponse>> generateMultipartPartPresignedUrl(
            @PathVariable Long fileId,
            @Valid @RequestBody MultipartPartPresignedUrlRequest request
    ) {
        log.info("POST /api/files/{}/multipart/parts - partNumber={}", fileId, request.partNumber());
        MultipartPartPresignedUrlResponse response = s3PresignedUrlService.generateMultipartPartPresignedUrl(
                fileId,
                request.partNumber()
        );
        return ResponseEntity.ok(new ApiResponse<>(MESSAGE_MULTIPART_PART_URL_GENERATED, response));
    }

    @PostMapping("/{fileId}/multipart/complete")
    @Operation(
            summary = "Multipart 업로드 완료",
            description = "업로드된 parts를 서버에서 part_number 기준으로 정렬해 S3 multipart complete를 수행합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "완료 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파일을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "S3 complete 실패",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<FileUploadConfirmResponse>> completeMultipartUpload(
            @PathVariable Long fileId,
            @Valid @RequestBody MultipartUploadCompleteRequest request
    ) {
        log.info("POST /api/files/{}/multipart/complete - partCount={}", fileId, request.parts().size());
        FileUploadConfirmResponse response = s3PresignedUrlService.completeMultipartUpload(fileId, request);
        return ResponseEntity.ok(new ApiResponse<>(MESSAGE_MULTIPART_UPLOAD_COMPLETED, response));
    }

    @PostMapping("/{fileId}/multipart/abort")
    @Operation(
            summary = "Multipart 업로드 중단",
            description = "진행 중인 VIDEO multipart 업로드를 중단(abort)하고 파일 상태를 FAILED로 갱신합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "중단 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청 (VIDEO가 아니거나 이미 완료됨)",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파일을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "S3 abort 실패",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<MultipartUploadAbortResponse>> abortMultipartUpload(
            @PathVariable Long fileId
    ) {
        log.info("POST /api/files/{}/multipart/abort", fileId);
        MultipartUploadAbortResponse response = s3PresignedUrlService.abortMultipartUpload(fileId);
        return ResponseEntity.ok(new ApiResponse<>(MESSAGE_MULTIPART_UPLOAD_ABORTED, response));
    }

    @PostMapping("/{fileId}/confirm")
    @Operation(
            summary = "파일 업로드 완료 확인",
            description = "단일 업로드(AUDIO/IMAGE 계열) 완료 확인 API입니다. VIDEO는 multipart complete API를 사용합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "업로드 확인 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "404",
                    description = "파일을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "422",
                    description = "S3에 파일이 없음 (업로드 실패)",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            )
    })
    public ResponseEntity<ApiResponse<FileUploadConfirmResponse>> confirmUpload(
            @Parameter(description = "파일 ID", example = "123")
            @PathVariable Long fileId
    ) {
        log.info("POST /api/files/{}/confirm", fileId);
        FileUploadConfirmResponse response = s3PresignedUrlService.confirmUpload(fileId);
        return ResponseEntity.ok(
                new ApiResponse<>(MESSAGE_UPLOAD_CONFIRMED, response)
        );
    }
}
