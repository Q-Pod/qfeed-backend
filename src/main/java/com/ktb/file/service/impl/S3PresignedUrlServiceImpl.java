package com.ktb.file.service.impl;

import com.ktb.common.config.S3Config;
import com.ktb.file.domain.File;
import com.ktb.file.domain.FileCategory;
import com.ktb.file.domain.FileUploadStatus;
import com.ktb.file.domain.StorageType;
import com.ktb.file.dto.request.PresignedUrlMethod;
import com.ktb.file.dto.request.PresignedUrlRequest;
import com.ktb.file.dto.response.FileUploadConfirmResponse;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3PresignedUrlServiceImpl implements S3PresignedUrlService {

    private static final int PRESIGNED_URL_EXPIRATION_SECONDS = 300; // 5분
    private static final String UPLOAD_METHOD = "PUT";
    private static final String ERROR_MESSAGE_EXTENSION_NOT_FOUND = "파일 확장자를 찾을 수 없습니다";
    private static final String ERROR_MESSAGE_FILE_NOT_UPLOADED = "S3에 파일이 업로드되지 않았습니다";

    private final FileRepository fileRepository;
    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    private final S3Config s3Config;

    @Override
    @Transactional
    public PresignedUrlResponse generatePresignedUrl(PresignedUrlRequest request) {
        PresignedUrlMethod method = resolveMethod(request);
        if (method == PresignedUrlMethod.PUT) {
            return generateUploadPresignedUrl(request);
        }
        return generateReadPresignedUrl(request, method);
    }

    @Override
    @Transactional
    public FileUploadConfirmResponse confirmUpload(Long fileId) {
        File file = fileRepository.findById(fileId)
                .orElseThrow(() -> new FileNotFoundException(fileId));

        // S3에서 파일 존재 확인
        boolean exists = isFileExistsInS3(file.getPath());

        if (exists) {
            file.setUploadStatus(FileUploadStatus.UPLOADED);
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

            log.error("Upload confirmation failed - File ID: {}, S3 Key: {}", file.getId(), file.getPath());

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
            return false;
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


    private void validateMetadata(PresignedUrlRequest request) {
        FileCategory category = request.category();

        if (!category.isAllowedMimeType(request.mimeType())) {
            throw new FileInvalidMetadataException(
                    "허용되지 않은 파일 형식입니다. 허용 형식: " + category.getAllowedMimeTypesAsString()
            );
        }

        if (request.fileSize() > category.getMaxSizeBytes()) {
            throw new FileSizeExceededException(category, SizeUtil.getReadableSize(request.fileSize()));
        }

        String extension = extractExtension(request.fileName());
        if (!category.isAllowedExtension(extension)) {
            throw new FileInvalidMetadataException(
                    "허용되지 않은 확장자입니다. 허용 확장자: " + category.getAllowedExtensionsAsString()
            );
        }
    }

    private String extractExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            throw new FileInvalidMetadataException(ERROR_MESSAGE_EXTENSION_NOT_FOUND);
        }
        return fileName.substring(fileName.lastIndexOf(".") + 1).toLowerCase();
    }

    private String generateStoredName(String extension) {
        return UUID.randomUUID().toString() + "." + extension;
    }

    private String buildS3Key(FileCategory category, String originalName) {
        String categoryPrefix = category.getS3Directory();
        return categoryPrefix + "/" + originalName;
    }

    private String buildCdnUrl(String s3Key) {
        return normalizeCdnPrefix() + "/" + s3Key;
    }

    private PresignedUrlResponse generateUploadPresignedUrl(PresignedUrlRequest request) {
        validateMetadata(request);

        String extension = extractExtension(request.fileName());
        String storedName = generateStoredName(extension);
        String s3Key = buildS3Key(request.category(), request.fileName());

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

        return new PresignedUrlResponse(
                savedFile.getId(),
                presignedUrl,
                PRESIGNED_URL_EXPIRATION_SECONDS,
                UPLOAD_METHOD
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

        return new PresignedUrlResponse(
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
}
