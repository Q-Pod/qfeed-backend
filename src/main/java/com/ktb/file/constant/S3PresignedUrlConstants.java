package com.ktb.file.constant;

import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

public final class S3PresignedUrlConstants {

    public static final int PRESIGNED_URL_EXPIRATION_SECONDS = 300; // 5분
    public static final int MULTIPART_PART_SIZE_BYTES = 8 * 1024 * 1024; // 8MB
    public static final int MULTIPART_STALE_HOURS = 24;
    public static final String UPLOAD_METHOD = "PUT";
    public static final String FILE_NAME_SEPARATOR = "_";
    public static final String FILE_TYPE_STT = "STT";
    public static final String FILE_TYPE_REAL_VIDEO = "REAL_VIDEO";

    public static final DateTimeFormatter CLIENT_TIMESTAMP_FORMATTER =
            DateTimeFormatter.ofPattern("yyyy-MM-dd_HH:mm:ss");
    public static final Pattern AUDIO_FILE_NAME_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}_\\d{2}:\\d{2}:\\d{2})_(PRACTICE|REAL|UNKNOWN)_"
                    + FILE_TYPE_STT
                    + "_[0-9a-fA-F-]{36}\\.(webm|mp3|m4a|wav|ogg)$",
            Pattern.CASE_INSENSITIVE
    );
    public static final Pattern VIDEO_FILE_NAME_PATTERN = Pattern.compile(
            "^(\\d{4}-\\d{2}-\\d{2}_\\d{2}:\\d{2}:\\d{2})_"
                    + FILE_TYPE_REAL_VIDEO
                    + "_[0-9a-fA-F-]{36}\\.(webm|mp4)$",
            Pattern.CASE_INSENSITIVE
    );

    private S3PresignedUrlConstants() {
    }
}
