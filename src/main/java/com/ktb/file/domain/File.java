package com.ktb.file.domain;

import com.ktb.common.domain.BaseTimeEntity;
import com.ktb.file.exception.FileAlreadyDeletedException;
import com.ktb.file.exception.FileExtensionNotAllowedException;
import com.ktb.file.exception.FileInvalidMetadataException;
import com.ktb.file.exception.FileNotDeletedException;
import com.ktb.file.exception.FileSizeExceededException;
import com.ktb.file.util.SizeUtil;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
        name = "FILE",
        indexes = {
                @Index(name = "idx_stored_name", columnList = "file_stored_name"),
                @Index(name = "idx_hash", columnList = "file_hash"),
                @Index(name = "idx_deleted", columnList = "file_deleted_at"),
                @Index(name = "idx_created", columnList = "file_created_at"),
                @Index(name = "idx_storage", columnList = "file_storage_type"),
                @Index(name = "idx_ext", columnList = "file_ext"),
                @Index(name = "idx_category", columnList = "file_category")
        }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class File extends BaseTimeEntity {

    private static final int ORIGINAL_NAME_MAX_LENGTH = 500;
    private static final int STORED_NAME_MAX_LENGTH = 500;
    private static final int PATH_MAX_LENGTH = 1000;
    private static final int EXTENSION_MAX_LENGTH = 20;
    private static final int MIME_TYPE_MAX_LENGTH = 100;
    private static final int URL_MAX_LENGTH = 2000;
    private static final int HASH_LENGTH = 64;
    private static final int MULTIPART_UPLOAD_ID_MAX_LENGTH = 300;
    private static final long MIN_FILE_SIZE = 0L;
    private static final long TEMP_CLEANUP_DAYS = 1L;
    private static final long PERMANENT_DELETE_DAYS = 7L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "file_id")
    private Long id;

    @Column(name = "file_original_name", nullable = false, length = ORIGINAL_NAME_MAX_LENGTH)
    private String originalName;

    @Column(name = "file_stored_name", nullable = false, length = STORED_NAME_MAX_LENGTH)
    private String storedName;

    @Column(name = "file_path", nullable = false, length = PATH_MAX_LENGTH)
    private String path;

    @Column(name = "file_ext", nullable = false, length = EXTENSION_MAX_LENGTH)
    private String extension;

    @Column(name = "file_size", nullable = false)
    private Long size;

    @Column(name = "file_hash", length = HASH_LENGTH)
    private String hash;

    @Column(name = "file_mime_type", nullable = false, length = MIME_TYPE_MAX_LENGTH)
    private String mimeType;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_storage_type", nullable = false, length = 20)
    private StorageType storageType = StorageType.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(name = "file_category", nullable = false, length = 20)
    private FileCategory category;

    @Column(name = "file_url", length = URL_MAX_LENGTH)
    private String url;

    @Column(name = "file_created_at", nullable = false, updatable = false)
    private LocalDateTime fileCreatedAt;

    @Column(name = "file_deleted_at")
    private LocalDateTime fileDeletedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "upload_status", nullable = false, length = 20)
    private FileUploadStatus uploadStatus = FileUploadStatus.UPLOADED;

    @Column(name = "multipart_upload_id", length = MULTIPART_UPLOAD_ID_MAX_LENGTH)
    private String multipartUploadId;

    @Column(name = "multipart_started_at")
    private LocalDateTime multipartStartedAt;

    @Builder
    private File(
            String originalName,
            String storedName,
            String path,
            String extension,
            Long size,
            String hash,
            String mimeType,
            StorageType storageType,
            FileCategory category,
            String url,
            FileUploadStatus uploadStatus,
            String multipartUploadId,
            LocalDateTime multipartStartedAt
    ) {
        validateFile(originalName, storedName, path, extension, size, mimeType);

        this.originalName = originalName;
        this.storedName = storedName;
        this.path = path;
        this.extension = extension.toLowerCase();
        this.size = size;
        this.hash = hash;
        this.mimeType = mimeType;
        this.storageType = storageType != null ? storageType : StorageType.LOCAL;
        this.category = category;
        this.url = url;
        this.uploadStatus = uploadStatus != null ? uploadStatus : FileUploadStatus.UPLOADED;
        this.multipartUploadId = multipartUploadId;
        this.multipartStartedAt = multipartStartedAt;
        this.fileCreatedAt = LocalDateTime.now();
    }

    public void setUploadStatus(FileUploadStatus uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public void startMultipartUpload(String uploadId, LocalDateTime startedAt) {
        this.multipartUploadId = uploadId;
        this.multipartStartedAt = startedAt;
    }

    public void clearMultipartUpload() {
        this.multipartUploadId = null;
        this.multipartStartedAt = null;
    }

    public static File create(
            String originalName,
            String storedName,
            String path,
            String extension,
            Long size,
            String mimeType,
            FileCategory category,
            StorageType storageType
    ) {
        return File.builder()
                .originalName(originalName)
                .storedName(storedName)
                .path(path)
                .extension(extension)
                .size(size)
                .mimeType(mimeType)
                .category(category)
                .storageType(storageType)
                .build();
    }

    public static File createWithHash(
            String originalName,
            String storedName,
            String path,
            String extension,
            Long size,
            String hash,
            String mimeType,
            FileCategory category,
            StorageType storageType
    ) {
        return File.builder()
                .originalName(originalName)
                .storedName(storedName)
                .path(path)
                .extension(extension)
                .size(size)
                .hash(hash)
                .mimeType(mimeType)
                .category(category)
                .storageType(storageType)
                .build();
    }

    private void validateFile(
            String originalName,
            String storedName,
            String path,
            String extension,
            Long size,
            String mimeType
    ) {
        if (originalName == null || originalName.trim().isEmpty()) {
            throw new FileInvalidMetadataException("원본 파일명은 필수입니다.");
        }
        if (originalName.length() > ORIGINAL_NAME_MAX_LENGTH) {
            throw new FileInvalidMetadataException(
                    "원본 파일명은 " + ORIGINAL_NAME_MAX_LENGTH + "자를 초과할 수 없습니다."
            );
        }

        if (storedName == null || storedName.trim().isEmpty()) {
            throw new FileInvalidMetadataException("저장 파일명은 필수입니다.");
        }
        if (storedName.length() > STORED_NAME_MAX_LENGTH) {
            throw new FileInvalidMetadataException(
                    "저장 파일명은 " + STORED_NAME_MAX_LENGTH + "자를 초과할 수 없습니다."
            );
        }

        if (path == null || path.trim().isEmpty()) {
            throw new FileInvalidMetadataException("파일 경로는 필수입니다.");
        }
        if (path.length() > PATH_MAX_LENGTH) {
            throw new FileInvalidMetadataException(
                    "파일 경로는 " + PATH_MAX_LENGTH + "자를 초과할 수 없습니다."
            );
        }

        if (extension == null || extension.trim().isEmpty()) {
            throw new FileInvalidMetadataException("파일 확장자는 필수입니다.");
        }
        if (extension.length() > EXTENSION_MAX_LENGTH) {
            throw new FileInvalidMetadataException(
                    "파일 확장자는 " + EXTENSION_MAX_LENGTH + "자를 초과할 수 없습니다."
            );
        }

        if (size == null || size < MIN_FILE_SIZE) {
            throw new FileInvalidMetadataException("파일 크기는 0 이상이어야 합니다.");
        }

        if (mimeType == null || mimeType.trim().isEmpty()) {
            throw new FileInvalidMetadataException("MIME 타입은 필수입니다.");
        }
        if (mimeType.length() > MIME_TYPE_MAX_LENGTH) {
            throw new FileInvalidMetadataException(
                    "MIME 타입은 " + MIME_TYPE_MAX_LENGTH + "자를 초과할 수 없습니다."
            );
        }
    }

    public void updateUrl(String newUrl) {
        if (newUrl != null && newUrl.length() > URL_MAX_LENGTH) {
            throw new FileInvalidMetadataException(
                    "URL은 " + URL_MAX_LENGTH + "자를 초과할 수 없습니다."
            );
        }
        this.url = newUrl;
    }

    public void migrateStorage(StorageType newStorageType, String newPath, String newUrl) {
        this.storageType = newStorageType;
        this.path = newPath;
        this.url = newUrl;
    }

    public void updateHash(String hash) {
        if (hash != null && hash.length() != HASH_LENGTH) {
            throw new FileInvalidMetadataException("SHA-256 해시는 " + HASH_LENGTH + "자여야 합니다.");
        }
        this.hash = hash;
    }

    public void delete() {
        if (isDeleted()) {
            throw new FileAlreadyDeletedException(id);
        }
        this.fileDeletedAt = LocalDateTime.now();
    }

    public void restore() {
        if (!isDeleted()) {
            throw new FileNotDeletedException(id);
        }
        this.fileDeletedAt = null;
    }

    public boolean isDeleted() {
        return fileDeletedAt != null;
    }

    public boolean isActive() {
        return fileDeletedAt == null;
    }

    public boolean isImage() {
        return mimeType != null && mimeType.startsWith("image/");
    }


    public boolean isVideo() {
        return mimeType != null && mimeType.startsWith("video/");
    }

    public String getReadableSize() {
        return SizeUtil.getReadableSize(size);
    }

    public void validateSizeLimit() {
        long maxSize = category.getMaxSizeBytes();
        if (size > maxSize) {
            throw new FileSizeExceededException(category, getReadableSize());
        }
    }

    public void validateExtension() {
        if (!category.isAllowedExtension(extension)) {
            throw new FileExtensionNotAllowedException(category, extension);
        }
    }

    public long getDaysSinceCreated() {
        return java.time.Duration.between(fileCreatedAt, LocalDateTime.now()).toDays();
    }

    public boolean shouldBeCleanedAsTemp() {
        return category == FileCategory.TEMP && getDaysSinceCreated() >= TEMP_CLEANUP_DAYS;
    }

    public boolean shouldBePermanentlyDeleted() {
        if (!isDeleted()) {
            return false;
        }
        return java.time.Duration.between(fileDeletedAt, LocalDateTime.now())
                .toDays() >= PERMANENT_DELETE_DAYS;
    }

}
