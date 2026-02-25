package com.ktb.file.domain;

import java.util.Arrays;
import java.util.List;
import lombok.Getter;

@Getter
public enum FileCategory {
    PROFILE(
            "프로필 이미지",
            "image",
            5 * 1024 * 1024L, // 5MB
            Arrays.asList("jpg", "jpeg", "png", "gif", "webp"),
            Arrays.asList("image/jpeg", "image/png", "image/gif", "image/webp")
    ),

    ARCHITECTURE(
            "아키텍처 다이어그램",
            "image",
            10 * 1024 * 1024L, // 10MB
            Arrays.asList("jpg", "jpeg", "png", "gif", "svg"),
            Arrays.asList("image/jpeg", "image/png", "image/gif", "image/svg+xml")
    ),

    ATTACHMENT(
            "일반 첨부파일",
            "image",
            50 * 1024 * 1024L, // 50MB
            Arrays.asList("jpg", "jpeg", "png", "gif"),
            Arrays.asList("image/jpeg", "image/png", "image/gif")
    ),

    TEMP(
            "임시 파일",
            "image",
            100 * 1024 * 1024L, // 100MB
            Arrays.asList("jpg", "jpeg", "png", "gif"),
            Arrays.asList("image/jpeg", "image/png", "image/gif")
    ),

    AUDIO(
            "음성 파일",
            "audio",
            10 * 1024 * 1024L, // 10MB
            Arrays.asList("mp3", "wav", "m4a"),
            Arrays.asList("audio/mpeg", "audio/wav", "audio/x-m4a", "audio/mp4")
    ),

    VIDEO(
            "비디오 파일",
            "video",
            100 * 1024 * 1024L, // 100MB
            Arrays.asList("mp4", "mov", "avi"),
            Arrays.asList("video/mp4", "video/quicktime", "video/x-msvideo")
    );

    private final String description;
    private final String s3Directory;
    private final long maxSizeBytes;
    private final List<String> allowedExtensions;
    private final List<String> allowedMimeTypes;

    FileCategory(String description, String s3Directory, long maxSizeBytes, List<String> allowedExtensions,
                 List<String> allowedMimeTypes) {
        this.description = description;
        this.s3Directory = s3Directory;
        this.maxSizeBytes = maxSizeBytes;
        this.allowedExtensions = allowedExtensions;
        this.allowedMimeTypes = allowedMimeTypes;
    }

    public boolean isAllowedExtension(String extension) {
        if (extension == null) {
            return false;
        }
        return allowedExtensions.contains(extension.toLowerCase());
    }

    public double getMaxSizeMB() {
        return maxSizeBytes / (1024.0 * 1024.0);
    }

    public boolean isImageOnly() {
        return this == PROFILE;
    }

    public boolean shouldBeAutoDeleted() {
        return this == TEMP;
    }

    public boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return allowedMimeTypes.contains(mimeType.toLowerCase());
    }

    public String getAllowedExtensionsAsString() {
        return String.join(", ", allowedExtensions);
    }

    public String getAllowedMimeTypesAsString() {
        return String.join(", ", allowedMimeTypes);
    }
}
