package com.ktb.file.service.impl;

import com.ktb.common.config.S3Config;
import com.ktb.common.domain.ErrorCode;
import com.ktb.file.constant.S3PresignedUrlConstants;
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
import java.time.format.DateTimeParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlServiceImpl implements S3PresignedUrlService {

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
            throw new FileInvalidMetadataException(ErrorCode.FILE_PART_NUMBER_INVALID);
        }

        File file = getMultipartTargetFile(fileId);
        log.info("Generate multipart part presigned url requested - fileId={}, partNumber={}", fileId, partNumber);
        String presignedUrl = generateMultipartPartPresignedUrl(file.getPath(), file.getMultipartUploadId(), partNumber);
        log.debug("Multipart part presigned url generated - fileId={}, partNumber={}, expiresIn={}",
                fileId, partNumber, S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS);
        return new MultipartPartPresignedUrlResponse(
                file.getId(),
                partNumber,
                presignedUrl,
                S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS,
                S3PresignedUrlConstants.UPLOAD_METHOD
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
            throw new FileStorageMigrationException(ErrorCode.FILE_MULTIPART_COMPLETE_FAILED, e);
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
            throw new FileInvalidMetadataException(ErrorCode.FILE_MULTIPART_ABORT_ALREADY_UPLOADED);
        }

        String uploadId = file.getMultipartUploadId();
        if (uploadId != null && !uploadId.isBlank()) {
            try {
                abortMultipartUpload(file.getPath(), uploadId);
            } catch (Exception e) {
                log.error("Failed to abort multipart upload - fileId={}, key={}, uploadId={}",
                        file.getId(), file.getPath(), uploadId, e);
                throw new FileStorageMigrationException(ErrorCode.FILE_MULTIPART_ABORT_FAILED, e);
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
        LocalDateTime threshold = LocalDateTime.now().minusHours(S3PresignedUrlConstants.MULTIPART_STALE_HOURS);
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
            log.debug("Marked stale multipart file as FAILED - fileId={}", file.getId());
            cleaned++;
        }

        log.info("Cleaned stale multipart uploads - threshold={}, cleaned={}", threshold, cleaned);
    }

    @Override
    @Transactional
    public FileUploadConfirmResponse confirmUpload(Long fileId) {
        log.info("Confirm upload requested - fileId={}", fileId);
        File file = findFileById(fileId);
        if (file.getCategory() == FileCategory.VIDEO) {
            log.warn("Confirm upload rejected - VIDEO category must use multipart complete, fileId={}", fileId);
            throw new FileInvalidMetadataException(ErrorCode.FILE_VIDEO_CONFIRM_NOT_ALLOWED);
        }

        // S3에서 파일 존재 확인
        boolean exists = isFileExistsInS3(file.getPath());
        log.info("Confirm upload S3 existence checked - fileId={}, exists={}", fileId, exists);

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

            throw new FileStorageMigrationException(ErrorCode.FILE_NOT_UPLOADED);
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
            log.debug("S3 object exists - key={}", s3Key);
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
            .signatureDuration(Duration.ofSeconds(S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS))
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
                .signatureDuration(Duration.ofSeconds(S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS))
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
                .signatureDuration(Duration.ofSeconds(S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS))
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

        if (category == FileCategory.AUDIO || category == FileCategory.VIDEO) {
            String extensionByMime = resolveExtensionByMimeType(category, request.mimeType());
            if (!extensionByMime.equals(extension)) {
                log.warn("File extension and mime mismatch - category={}, extension={}, mimeType={}, expectedExtension={}",
                        category, extension, request.mimeType(), extensionByMime);
                throw new FileInvalidMetadataException(
                        ErrorCode.FILE_EXTENSION_MIME_MISMATCH,
                        "(expected: ." + extensionByMime + ")"
                );
            }
        }

        if (category == FileCategory.AUDIO || category == FileCategory.VIDEO || category == FileCategory.ARCHITECTURE) {
            validateClientFileNamePattern(category, request.fileName());
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            log.warn("Invalid file name extension - fileName={}", fileName);
            throw new FileInvalidMetadataException(ErrorCode.FILE_NAME_EXTENSION_INVALID);
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String generateStoredName(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String resolveStoredName(PresignedUrlRequest request) {
        FileCategory category = request.category();
        String extension = resolveStorageExtension(category, request);

        if (category == FileCategory.AUDIO) {
            AudioClientFileName audioName = parseAudioClientFileName(request.fileName());
            return audioName.timestamp()
                    + S3PresignedUrlConstants.FILE_NAME_SEPARATOR
                    + audioName.mode()
                    + S3PresignedUrlConstants.FILE_NAME_SEPARATOR
                    + S3PresignedUrlConstants.FILE_TYPE_STT
                    + S3PresignedUrlConstants.FILE_NAME_SEPARATOR
                    + UUID.randomUUID()
                    + "."
                    + extension;
        }

        if (category == FileCategory.VIDEO) {
            String timestamp = parseVideoClientFileNameTimestamp(request.fileName());
            return timestamp
                    + S3PresignedUrlConstants.FILE_NAME_SEPARATOR
                    + S3PresignedUrlConstants.FILE_TYPE_REAL_VIDEO
                    + S3PresignedUrlConstants.FILE_NAME_SEPARATOR
                    + UUID.randomUUID()
                    + "."
                    + extension;
        }

        if (category == FileCategory.ARCHITECTURE) {
            String timestamp = parseArchitectureClientFileNameTimestamp(request.fileName());
            return timestamp
                    + S3PresignedUrlConstants.FILE_NAME_SEPARATOR
                    + S3PresignedUrlConstants.FILE_TYPE_ARCHITECTURE
                    + S3PresignedUrlConstants.FILE_NAME_SEPARATOR
                    + UUID.randomUUID()
                    + "."
                    + extension;
        }

        return generateStoredName(extension);
    }

    private String resolveStorageExtension(FileCategory category, PresignedUrlRequest request) {
        if (category == FileCategory.AUDIO || category == FileCategory.VIDEO) {
            return resolveExtensionByMimeType(category, request.mimeType());
        }
        return extractExtension(request.fileName());
    }

    private String resolveExtensionByMimeType(FileCategory category, String mimeType) {
        String normalizedMimeType = mimeType == null ? "" : mimeType.toLowerCase(Locale.ROOT);
        if (category == FileCategory.AUDIO) {
            return switch (normalizedMimeType) {
                case "audio/webm" -> "webm";
                case "audio/mpeg" -> "mp3";
                case "audio/x-m4a", "audio/mp4" -> "m4a";
                case "audio/wav" -> "wav";
                case "audio/ogg" -> "ogg";
                default -> throwUnsupportedMimeExtension(category, mimeType);
            };
        }

        if (category == FileCategory.VIDEO) {
            return switch (normalizedMimeType) {
                case "video/webm" -> "webm";
                case "video/mp4" -> "mp4";
                default -> throwUnsupportedMimeExtension(category, mimeType);
            };
        }

        throw new FileInvalidMetadataException(ErrorCode.FILE_UNSUPPORTED_MIME_EXTENSION);
    }

    private String throwUnsupportedMimeExtension(FileCategory category, String mimeType) {
        log.warn("Unsupported mime type for extension mapping - category={}, mimeType={}", category, mimeType);
        throw new FileInvalidMetadataException(
                ErrorCode.FILE_UNSUPPORTED_MIME_EXTENSION,
                "(category=" + category + ", mimeType=" + mimeType + ")"
        );
    }

    private void validateClientFileNamePattern(FileCategory category, String fileName) {
        if (category == FileCategory.AUDIO) {
            parseAudioClientFileName(fileName);
            return;
        }
        if (category == FileCategory.VIDEO) {
            parseVideoClientFileNameTimestamp(fileName);
            return;
        }
        if (category == FileCategory.ARCHITECTURE) {
            parseArchitectureClientFileNameTimestamp(fileName);
        }
    }

    private AudioClientFileName parseAudioClientFileName(String fileName) {
        Matcher matcher = S3PresignedUrlConstants.AUDIO_FILE_NAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            log.warn("Invalid AUDIO file_name pattern - fileName={}", fileName);
            throw new FileInvalidMetadataException(ErrorCode.FILE_AUDIO_FILE_NAME_PATTERN_INVALID);
        }
        String timestamp = validateAndNormalizeClientTimestamp(matcher.group(1));
        String mode = matcher.group(2).toUpperCase(Locale.ROOT);
        return new AudioClientFileName(timestamp, mode);
    }

    private String parseVideoClientFileNameTimestamp(String fileName) {
        Matcher matcher = S3PresignedUrlConstants.VIDEO_FILE_NAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            log.warn("Invalid VIDEO file_name pattern - fileName={}", fileName);
            throw new FileInvalidMetadataException(ErrorCode.FILE_VIDEO_FILE_NAME_PATTERN_INVALID);
        }
        return validateAndNormalizeClientTimestamp(matcher.group(1));
    }

    private String parseArchitectureClientFileNameTimestamp(String fileName) {
        Matcher matcher = S3PresignedUrlConstants.ARCHITECTURE_FILE_NAME_PATTERN.matcher(fileName);
        if (!matcher.matches()) {
            log.warn("Invalid ARCHITECTURE file_name pattern - fileName={}", fileName);
            throw new FileInvalidMetadataException(ErrorCode.FILE_ARCHITECTURE_FILE_NAME_PATTERN_INVALID);
        }
        return validateAndNormalizeClientTimestamp(matcher.group(1));
    }

    private String validateAndNormalizeClientTimestamp(String timestampText) {
        try {
            LocalDateTime.parse(timestampText, S3PresignedUrlConstants.CLIENT_TIMESTAMP_FORMATTER);
            return timestampText;
        } catch (DateTimeParseException e) {
            log.warn("Invalid client timestamp in file_name - timestamp={}", timestampText);
            throw new FileInvalidMetadataException(ErrorCode.FILE_CLIENT_TIMESTAMP_INVALID);
        }
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

        String storedName = resolveStoredName(request);
        String extension = extractExtension(storedName);
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
                savedFile.getId(), s3Key, request.category(), S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS);

        return PresignedUrlResponse.single(
                savedFile.getId(),
                presignedUrl,
                S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS,
                S3PresignedUrlConstants.UPLOAD_METHOD
        );
    }

    private PresignedUrlResponse generateMultipartStartResponse(PresignedUrlRequest request) {
        validateMetadata(request);

        String storedName = resolveStoredName(request);
        String extension = extractExtension(storedName);
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
                savedFile.getId(), s3Key, uploadId, S3PresignedUrlConstants.MULTIPART_PART_SIZE_BYTES);

        return PresignedUrlResponse.multipart(
                savedFile.getId(),
                S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS,
                S3PresignedUrlConstants.MULTIPART_PART_SIZE_BYTES
        );
    }

    private PresignedUrlResponse generateReadPresignedUrl(PresignedUrlRequest request, PresignedUrlMethod method) {
        if (request.fileId() == null) {
            log.warn("Read presigned url requested without fileId - method={}, category={}",
                    method, request.category());
            throw new FileInvalidMetadataException(ErrorCode.FILE_READ_FILE_ID_REQUIRED);
        }

        File file = findFileById(request.fileId());

        String presignedUrl = generateS3PresignedGetUrl(file.getPath());

        log.info("Presigned URL generated - File ID: {}, S3 Key: {}, Method: {}, Expires in: {}s",
                file.getId(), file.getPath(), method, S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS);

        return PresignedUrlResponse.single(
                file.getId(),
                presignedUrl,
                S3PresignedUrlConstants.PRESIGNED_URL_EXPIRATION_SECONDS,
                method.name()
        );
    }

    private PresignedUrlMethod resolveMethod(PresignedUrlRequest request) {
        if (request.method() == null) {
            log.debug("Presigned url method not provided, defaulting to PUT - category={}, fileId={}",
                    request.category(), request.fileId());
            return PresignedUrlMethod.PUT;
        }
        return request.method();
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
            throw new FileStorageMigrationException(ErrorCode.FILE_MULTIPART_UPLOAD_ID_EMPTY);
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

    private record AudioClientFileName(
            String timestamp,
            String mode
    ) {
    }

    private File getMultipartTargetFile(Long fileId) {
        File file = getVideoTargetFile(fileId);
        if (file.getMultipartUploadId() == null || file.getMultipartUploadId().isBlank()) {
            log.warn("Multipart upload not started - fileId={}", fileId);
            throw new FileInvalidMetadataException(ErrorCode.FILE_MULTIPART_NOT_STARTED);
        }
        return file;
    }

    private File getVideoTargetFile(Long fileId) {
        File file = findFileById(fileId);
        if (file.getCategory() != FileCategory.VIDEO) {
            log.warn("Multipart operation rejected - fileId={}, category={}", fileId, file.getCategory());
            throw new FileInvalidMetadataException(ErrorCode.FILE_MULTIPART_ONLY_VIDEO);
        }
        return file;
    }

    private File findFileById(Long fileId) {
        return fileRepository.findById(fileId)
                .orElseThrow(() -> {
                    log.warn("File not found - fileId={}", fileId);
                    return new FileNotFoundException(fileId);
                });
    }
}
