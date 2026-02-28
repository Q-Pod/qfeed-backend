package com.ktb.file.service.impl;

import com.ktb.common.config.S3Config;
import com.ktb.file.domain.File;
import com.ktb.file.domain.FileCategory;
import com.ktb.file.domain.FileUploadStatus;
import com.ktb.file.domain.StorageType;
import com.ktb.file.dto.request.MultipartUploadCompleteRequest;
import com.ktb.file.dto.request.MultipartUploadedPartRequest;
import com.ktb.file.dto.request.PresignedUrlMethod;
import com.ktb.file.dto.request.PresignedUrlRequest;
import com.ktb.file.dto.response.FileUploadConfirmResponse;
import com.ktb.file.dto.response.MultipartPartPresignedUrlResponse;
import com.ktb.file.dto.response.MultipartUploadAbortResponse;
import com.ktb.file.dto.response.PresignedUrlResponse;
import com.ktb.file.exception.FileInvalidMetadataException;
import com.ktb.file.exception.FileNotFoundException;
import com.ktb.file.exception.FileSizeExceededException;
import com.ktb.file.exception.FileStorageMigrationException;
import com.ktb.file.repository.FileRepository;
import com.ktb.file.service.S3PresignedUrlService;
import com.ktb.file.util.SizeUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.AbortMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CompletedMultipartUpload;
import software.amazon.awssdk.services.s3.model.CompletedPart;
import software.amazon.awssdk.services.s3.model.CompleteMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadRequest;
import software.amazon.awssdk.services.s3.model.CreateMultipartUploadResponse;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedUploadPartRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.UploadPartPresignRequest;
import software.amazon.awssdk.services.s3.model.UploadPartRequest;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlServiceImpl implements S3PresignedUrlService {

    private static final int PRESIGNED_URL_EXPIRATION_SECONDS = 300; // 5분
    private static final int MULTIPART_PART_SIZE_BYTES = 8 * 1024 * 1024; // 8MB
    private static final String UPLOAD_METHOD = "PUT";
    private static final String ERROR_MESSAGE_EXTENSION_NOT_FOUND = "파일 확장자를 찾을 수 없습니다";
    private static final String ERROR_MESSAGE_FILE_NOT_UPLOADED = "S3에 파일이 업로드되지 않았습니다";
    private static final String ERROR_MESSAGE_MULTIPART_UPLOAD_ID_EMPTY = "multipart upload id 생성에 실패했습니다";
    private static final String ERROR_MESSAGE_MULTIPART_ONLY_VIDEO =
            "multipart 업로드는 VIDEO 카테고리에서만 지원됩니다";
    private static final String ERROR_MESSAGE_MULTIPART_NOT_STARTED =
            "multipart 업로드가 시작되지 않았습니다";
    private static final String ERROR_MESSAGE_PART_NUMBER_INVALID =
            "part_number는 1 이상이어야 합니다";
    private static final String ERROR_MESSAGE_VIDEO_CONFIRM_NOT_ALLOWED =
            "VIDEO 업로드는 confirm 대신 multipart complete API를 사용해야 합니다";
    private static final String ERROR_MESSAGE_MULTIPART_COMPLETE_FAILED =
            "multipart 업로드 완료 처리에 실패했습니다";
    private static final String ERROR_MESSAGE_MULTIPART_ABORT_FAILED =
            "multipart 업로드 중단 처리에 실패했습니다";
    private static final String ERROR_MESSAGE_MULTIPART_ABORT_ALREADY_UPLOADED =
            "이미 업로드 완료된 파일은 중단할 수 없습니다";
    private static final int MULTIPART_STALE_HOURS = 24;

    private final FileRepository fileRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final S3Config s3Config;

    @Override
    @Transactional
    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
        PresignedUrlMethod method = resolveMethod(request);
        log.info("Generate presigned url requested - method={}, category={}, fileId={}, fileSize={}",
                method, request.category(), request.fileId(), request.fileSize());
        if (method == PresignedUrlMethod.PUT) {
            if (request.category() == FileCategory.VIDEO) {
                return generateMultipartStartResponse(request);
            }
            return generateUploadPresignedUrl(request);
        }
        return generateReadPresignedUrl(request, method);
    }

    @Override
    @Transactional(readOnly = true)
    public MultipartPartPresignedUrlResponse generateMultipartPartPresignedUrl(Long fileId, int partNumber) {
        if (partNumber < 1) {
            log.warn("Invalid multipart part number - fileId={}, partNumber={}", fileId, partNumber);
            throw new FileInvalidMetadataException(ERROR_MESSAGE_PART_NUMBER_INVALID);
        }

        File file = getMultipartTargetFile(fileId);
        log.info("Generate multipart part presigned url requested - fileId={}, partNumber={}", fileId, partNumber);
        String presignedUrl = generateMultipartPartPresignedUrl(file.getPath(), file.getMultipartUploadId(), partNumber);
        return new MultipartPartPresignedUrlResponse(
                file.getId(),
                partNumber,
                presignedUrl,
                PRESIGNED_URL_EXPIRATION_SECONDS,
                UPLOAD_METHOD
        );
    }

    @Override
    @Transactional
    public FileUploadConfirmResponse completeMultipartUpload(Long fileId, MultipartUploadCompleteRequest request) {
        File file = getMultipartTargetFile(fileId);
        log.info("Complete multipart upload requested - fileId={}, partCount={}", fileId, request.parts().size());

        // 대용량 파일의 경우, partNumber 정렬비용 고려
        List<CompletedPart> completedParts = request.parts().stream()
                .sorted(Comparator.comparingInt(MultipartUploadedPartRequest::partNumber))
                .map(part -> CompletedPart.builder()
                        .partNumber(part.partNumber())
                        .eTag(part.etag())
                        .build())
                .collect(Collectors.toList());

        try {
            CompleteMultipartUploadRequest completeRequest = CompleteMultipartUploadRequest.builder()
                    .bucket(s3Config.getS3().getUploadBucketName())
                    .key(file.getPath())
                    .uploadId(file.getMultipartUploadId())
                    .multipartUpload(CompletedMultipartUpload.builder().parts(completedParts).build())
                    .build();
            s3Client.completeMultipartUpload(completeRequest);
        } catch (Exception e) {
            log.error("Failed to complete multipart upload - fileId={}, key={}, uploadId={}",
                    file.getId(), file.getPath(), file.getMultipartUploadId(), e);
            throw new FileStorageMigrationException(ERROR_MESSAGE_MULTIPART_COMPLETE_FAILED, e);
        }

        file.setUploadStatus(FileUploadStatus.UPLOADED);
        file.clearMultipartUpload();
        String cdnUrl = buildCdnUrl(file.getPath());
        file.updateUrl(cdnUrl);
        fileRepository.save(file);

        String presignedUrl = generateS3PresignedGetUrl(file.getPath());
        log.info("Multipart upload completed - fileId={}, key={}, partCount={}",
                file.getId(), file.getPath(), completedParts.size());
        return new FileUploadConfirmResponse(file.getId(), presignedUrl, FileUploadStatus.UPLOADED);
    }

    @Override
    @Transactional
    public MultipartUploadAbortResponse abortMultipartUpload(Long fileId) {
        File file = getVideoTargetFile(fileId);
        log.info("Abort multipart upload requested - fileId={}, key={}", fileId, file.getPath());
        if (file.getUploadStatus() == FileUploadStatus.UPLOADED) {
            log.warn("Abort multipart upload rejected - file already uploaded, fileId={}", fileId);
            throw new FileInvalidMetadataException(ERROR_MESSAGE_MULTIPART_ABORT_ALREADY_UPLOADED);
        }

        String uploadId = file.getMultipartUploadId();
        if (uploadId != null && !uploadId.isBlank()) {
            try {
                abortMultipartUpload(file.getPath(), uploadId);
            } catch (Exception e) {
                log.error("Failed to abort multipart upload - fileId={}, key={}, uploadId={}",
                        file.getId(), file.getPath(), uploadId, e);
                throw new FileStorageMigrationException(ERROR_MESSAGE_MULTIPART_ABORT_FAILED, e);
            }
        } else {
            log.info("Abort multipart upload skipped remote call - no uploadId, fileId={}", fileId);
        }

        file.setUploadStatus(FileUploadStatus.FAILED);
        file.clearMultipartUpload();
        fileRepository.save(file);
        log.info("Multipart upload aborted - fileId={}, status={}", file.getId(), file.getUploadStatus());
        return new MultipartUploadAbortResponse(file.getId(), file.getUploadStatus());
    }

    @Scheduled(fixedDelayString = "${file.multipart.cleanup-interval-ms}")
    @Transactional
    public void cleanupStaleMultipartUploads() {
        LocalDateTime threshold = LocalDateTime.now().minusHours(MULTIPART_STALE_HOURS);
        List<File> staleFiles = fileRepository.findByUploadStatusAndMultipartStartedAtBeforeAndMultipartUploadIdIsNotNull(
                FileUploadStatus.PENDING,
                threshold
        );
        if (staleFiles.isEmpty()) {
            log.debug("No stale multipart uploads found - threshold={}", threshold);
            return;
        }

        log.info("Stale multipart cleanup started - threshold={}, targetCount={}", threshold, staleFiles.size());
        int cleaned = 0;
        for (File file : staleFiles) {
            try {
                abortMultipartUpload(file.getPath(), file.getMultipartUploadId());
            } catch (Exception e) {
                log.warn("Failed to abort multipart upload - fileId={}, key={}, uploadId={}",
                        file.getId(), file.getPath(), file.getMultipartUploadId(), e);
            }

            file.setUploadStatus(FileUploadStatus.FAILED);
            file.clearMultipartUpload();
            fileRepository.save(file);
            cleaned++;
        }

        log.info("Cleaned stale multipart uploads - threshold={}, cleaned={}", threshold, cleaned);
    }

    @Override
    @Transactional
    public FileUploadConfirmResponse confirmUpload(Long fileId) {
        log.info("Confirm upload requested - fileId={}", fileId);
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
        if (file.getCategory() == FileCategory.VIDEO) {
            log.warn("Confirm upload rejected - VIDEO category must use multipart complete, fileId={}", fileId);
            throw new FileInvalidMetadataException(ERROR_MESSAGE_VIDEO_CONFIRM_NOT_ALLOWED);
        }

        // S3에서 파일 존재 확인
        boolean exists = isFileExistsInS3(file.getPath());

        if (exists) {
            file.setUploadStatus(FileUploadStatus.UPLOADED);
            file.clearMultipartUpload();
            String cdnUrl = buildCdnUrl(file.getPath());
            file.updateUrl(cdnUrl);
            fileRepository.save(file);

            log.info("Upload confirmed - File ID: {}, CDN URL: {}", file.getId(), cdnUrl);

            // 업로드 확인 응답은 조회용 Presigned GET URL을 반환
            String presignedUrl = generateS3PresignedGetUrl(file.getPath());
            return new FileUploadConfirmResponse(
                    file.getId(),
                    presignedUrl,
                    FileUploadStatus.UPLOADED
            );
        } else {
            file.setUploadStatus(FileUploadStatus.FAILED);
            fileRepository.save(file);

            log.warn("Upload confirmation failed - File ID: {}, S3 Key: {}", file.getId(), file.getPath());

            throw new FileStorageMigrationException(ERROR_MESSAGE_FILE_NOT_UPLOADED);
        }
    }

    @Override
    public boolean isFileExistsInS3(String s3Key) {
        try {
            HeadObjectRequest headObjectRequest = HeadObjectRequest.builder()
                .bucket(s3Config.getS3().getUploadBucketName())
                .key(s3Key)
                .build();
            s3Client.headObject(headObjectRequest);
            return true;
        } catch (NoSuchKeyException e) {
            log.debug("S3 object not found - key={}", s3Key);
            return false;
        } catch (Exception e) {
            log.error("Failed to check S3 object existence - key={}", s3Key, e);
            throw e;
        }
    }

    private String generateS3PresignedUrl(String s3Key, String contentType) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(s3Config.getS3().getUploadBucketName())
            .key(s3Key)
            .contentType(contentType)
            .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(Duration.ofSeconds(PRESIGNED_URL_EXPIRATION_SECONDS))
            .putObjectRequest(putObjectRequest)
            .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);

        return presignedRequest.url().toString();
    }

    private String generateS3PresignedGetUrl(String s3Key) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(s3Config.getS3().getUploadBucketName())
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(PRESIGNED_URL_EXPIRATION_SECONDS))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);

        return presignedRequest.url().toString();
    }

    private String generateMultipartPartPresignedUrl(String s3Key, String uploadId, int partNumber) {
        UploadPartRequest uploadPartRequest = UploadPartRequest.builder()
                .bucket(s3Config.getS3().getUploadBucketName())
                .key(s3Key)
                .uploadId(uploadId)
                .partNumber(partNumber)
                .build();

        UploadPartPresignRequest presignRequest = UploadPartPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(PRESIGNED_URL_EXPIRATION_SECONDS))
                .uploadPartRequest(uploadPartRequest)
                .build();

        PresignedUploadPartRequest presignedRequest = s3Presigner.presignUploadPart(presignRequest);
        return presignedRequest.url().toString();
    }


    private void validateMetadata(PresignedUrlRequest request) {
        FileCategory category = request.category();

        if (!category.isAllowedMimeType(request.mimeType())) {
            log.warn("Invalid file mime type - category={}, mimeType={}", category, request.mimeType());
            throw new FileInvalidMetadataException(
                    "허용되지 않은 파일 형식입니다. 허용 형식: " + category.getAllowedMimeTypesAsString()
            );
        }

        if (request.fileSize() > category.getMaxSizeBytes()) {
            log.warn("File size exceeded - category={}, fileSize={}, maxSize={}",
                    category, request.fileSize(), category.getMaxSizeBytes());
            throw new FileSizeExceededException(category, SizeUtil.getReadableSize(request.fileSize()));
        }

        String extension = extractExtension(request.fileName());
        if (!category.isAllowedExtension(extension)) {
            log.warn("Invalid file extension - category={}, extension={}", category, extension);
            throw new FileInvalidMetadataException(
                    "허용되지 않은 확장자입니다. 허용 확장자: " + category.getAllowedExtensionsAsString()
            );
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            log.warn("File extension not found - fileName={}", fileName);
            throw new FileInvalidMetadataException(ERROR_MESSAGE_EXTENSION_NOT_FOUND);
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String generateStoredName(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String buildS3Key(FileCategory category, String objectName) {
        String categoryPrefix = category.getS3Directory();
        return categoryPrefix + "/" + objectName;
    }

    private String buildCdnUrl(String s3Key) {
        return normalizeCdnPrefix() + "/" + s3Key;
    }

    private PresignedUrlResponse generateUploadPresignedUrl(PresignedUrlRequest request) {
        validateMetadata(request);

        String extension = extractExtension(request.fileName());
        String storedName = generateStoredName(extension);
        String s3Key = buildS3Key(request.category(), storedName);

        File fileEntity = File.builder()
                .originalName(request.fileName())
                .storedName(storedName)
                .path(s3Key)
                .extension(extension)
                .size(request.fileSize())
                .mimeType(request.mimeType())
                .storageType(StorageType.S3)
                .category(request.category())
                .uploadStatus(FileUploadStatus.PENDING)
                .build();

        File savedFile = fileRepository.save(fileEntity);

        String presignedUrl = generateS3PresignedUrl(s3Key, request.mimeType());

        log.info("Presigned URL generated - File ID: {}, S3 Key: {}, Category: {}, Expires in: {}s",
                savedFile.getId(), s3Key, request.category(), PRESIGNED_URL_EXPIRATION_SECONDS);

        return PresignedUrlResponse.single(
                savedFile.getId(),
                presignedUrl,
                PRESIGNED_URL_EXPIRATION_SECONDS,
                UPLOAD_METHOD
        );
    }

    private PresignedUrlResponse generateMultipartStartResponse(PresignedUrlRequest request) {
        validateMetadata(request);

        String extension = extractExtension(request.fileName());
        String storedName = generateStoredName(extension);
        String s3Key = buildS3Key(request.category(), storedName);

        File fileEntity = File.builder()
                .originalName(request.fileName())
                .storedName(storedName)
                .path(s3Key)
                .extension(extension)
                .size(request.fileSize())
                .mimeType(request.mimeType())
                .storageType(StorageType.S3)
                .category(request.category())
                .uploadStatus(FileUploadStatus.PENDING)
                .build();

        File savedFile = fileRepository.save(fileEntity);
        String uploadId = createMultipartUpload(savedFile.getPath(), request.mimeType());
        savedFile.startMultipartUpload(uploadId, LocalDateTime.now());
        fileRepository.save(savedFile);

        log.info("Multipart upload started - fileId={}, s3Key={}, uploadId={}, partSize={}",
                savedFile.getId(), s3Key, uploadId, MULTIPART_PART_SIZE_BYTES);

        return PresignedUrlResponse.multipart(
                savedFile.getId(),
                PRESIGNED_URL_EXPIRATION_SECONDS,
                MULTIPART_PART_SIZE_BYTES
        );
    }

    private PresignedUrlResponse generateReadPresignedUrl(PresignedUrlRequest request, PresignedUrlMethod method) {
        if (request.fileId() == null) {
            throw new FileInvalidMetadataException("file_id는 GET/HEAD 요청에서 필수입니다");
        }

        File file = fileRepository.findById(request.fileId())
                .orElseThrow(() -> new FileNotFoundException(request.fileId()));

        String presignedUrl = generateS3PresignedGetUrl(file.getPath());

        log.info("Presigned URL generated - File ID: {}, S3 Key: {}, Method: {}, Expires in: {}s",
                file.getId(), file.getPath(), method, PRESIGNED_URL_EXPIRATION_SECONDS);

        return PresignedUrlResponse.single(
                file.getId(),
                presignedUrl,
                PRESIGNED_URL_EXPIRATION_SECONDS,
                method.name()
        );
    }

    private PresignedUrlMethod resolveMethod(PresignedUrlRequest request) {
        return request.method() != null ? request.method() : PresignedUrlMethod.PUT;
    }

    private String normalizeCdnPrefix() {
        String cdnUrlPrefix = s3Config.getS3().getCdnUrlPrefix();
        return cdnUrlPrefix.endsWith("/") ? cdnUrlPrefix.substring(0, cdnUrlPrefix.length() - 1) : cdnUrlPrefix;
    }

    private String createMultipartUpload(String s3Key, String contentType) {
        CreateMultipartUploadRequest request = CreateMultipartUploadRequest.builder()
                .bucket(s3Config.getS3().getUploadBucketName())
                .key(s3Key)
                .contentType(contentType)
                .build();

        CreateMultipartUploadResponse response = s3Client.createMultipartUpload(request);
        String uploadId = response.uploadId();
        if (uploadId == null || uploadId.isBlank()) {
            log.error("Create multipart upload returned empty uploadId - key={}", s3Key);
            throw new FileStorageMigrationException(ERROR_MESSAGE_MULTIPART_UPLOAD_ID_EMPTY);
        }
        return uploadId;
    }

    private void abortMultipartUpload(String s3Key, String uploadId) {
        AbortMultipartUploadRequest request = AbortMultipartUploadRequest.builder()
                .bucket(s3Config.getS3().getUploadBucketName())
                .key(s3Key)
                .uploadId(uploadId)
                .build();
        s3Client.abortMultipartUpload(request);
    }

    private File getMultipartTargetFile(Long fileId) {
        File file = getVideoTargetFile(fileId);
        if (file.getMultipartUploadId() == null || file.getMultipartUploadId().isBlank()) {
            log.warn("Multipart upload not started - fileId={}", fileId);
            throw new FileInvalidMetadataException(ERROR_MESSAGE_MULTIPART_NOT_STARTED);
        }
        return file;
    }

    private File getVideoTargetFile(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));
        if (file.getCategory() != FileCategory.VIDEO) {
            log.warn("Multipart operation rejected - fileId={}, category={}", fileId, file.getCategory());
            throw new FileInvalidMetadataException(ERROR_MESSAGE_MULTIPART_ONLY_VIDEO);
        }
        return file;
    }
}
