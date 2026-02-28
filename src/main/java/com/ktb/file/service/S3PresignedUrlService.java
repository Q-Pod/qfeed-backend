package com.ktb.file.service;

import com.ktb.file.dto.request.PresignedUrlRequest;
import com.ktb.file.dto.response.FileUploadConfirmResponse;
import com.ktb.file.dto.request.MultipartUploadCompleteRequest;
import com.ktb.file.dto.response.MultipartUploadAbortResponse;
import com.ktb.file.dto.response.MultipartPartPresignedUrlResponse;
import com.ktb.file.dto.response.PresignedUrlResponse;

/**
 * S3 Presigned URL 서비스
 */
public interface S3PresignedUrlService {

    /**
     * S3 Presigned URL을 생성합니다.
     *
     * @param request Presigned URL 생성 요청 (파일명, 크기, MIME 타입, 카테고리)
     * @return Presigned URL 응답 (fileId, presignedUrl, expiresIn, uploadMethod)
     * @throws com.ktb.file.exception.FileInvalidMetadataException 파일 메타데이터가 유효하지 않을 때
     * @throws com.ktb.file.exception.FileSizeExceededException 파일 크기가 제한을 초과할 때
     */
    PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request);

    /**
     * multipart 업로드용 파트 Presigned URL을 생성합니다.
     *
     * @param fileId 파일 ID
     * @param partNumber 파트 번호(1-base)
     * @return 파트 업로드 URL 응답
     */
    MultipartPartPresignedUrlResponse generateMultipartPartPresignedUrl(Long fileId, int partNumber);

    /**
     * multipart 업로드를 완료 처리합니다.
     *
     * @param fileId 파일 ID
     * @param request 완료 요청(parts)
     * @return 업로드 완료 응답
     */
    FileUploadConfirmResponse completeMultipartUpload(Long fileId, MultipartUploadCompleteRequest request);

    /**
     * multipart 업로드를 중단합니다.
     *
     * @param fileId 파일 ID
     * @return 업로드 중단 응답
     */
    MultipartUploadAbortResponse abortMultipartUpload(Long fileId);

    /**
     * S3 파일 업로드 완료를 확인하고 상태를 업데이트합니다.
     *
     * @param fileId 파일 ID
     * @return 업로드 확인 응답 (fileId, fileUrl, status)
     * @throws com.ktb.file.exception.FileNotFoundException 파일을 찾을 수 없을 때
     * @throws com.ktb.file.exception.FileStorageMigrationException S3에 파일이 없을 때
     */
    FileUploadConfirmResponse confirmUpload(Long fileId);

    /**
     * S3에서 파일 존재 여부를 확인합니다.
     *
     * @param s3Key S3 객체 키
     * @return 파일 존재 여부
     */
    boolean isFileExistsInS3(String s3Key);
}
