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

    public static final String ERROR_MESSAGE_EXTENSION_NOT_FOUND = "파일 확장자를 찾을 수 없습니다";
    public static final String ERROR_MESSAGE_FILE_NOT_UPLOADED = "S3에 파일이 업로드되지 않았습니다";
    public static final String ERROR_MESSAGE_MULTIPART_UPLOAD_ID_EMPTY = "multipart upload id 생성에 실패했습니다";
    public static final String ERROR_MESSAGE_MULTIPART_ONLY_VIDEO =
            "multipart 업로드는 VIDEO 카테고리에서만 지원됩니다";
    public static final String ERROR_MESSAGE_MULTIPART_NOT_STARTED =
            "multipart 업로드가 시작되지 않았습니다";
    public static final String ERROR_MESSAGE_PART_NUMBER_INVALID =
            "part_number는 1 이상이어야 합니다";
    public static final String ERROR_MESSAGE_VIDEO_CONFIRM_NOT_ALLOWED =
            "VIDEO 업로드는 confirm 대신 multipart complete API를 사용해야 합니다";
    public static final String ERROR_MESSAGE_MULTIPART_COMPLETE_FAILED =
            "multipart 업로드 완료 처리에 실패했습니다";
    public static final String ERROR_MESSAGE_MULTIPART_ABORT_FAILED =
            "multipart 업로드 중단 처리에 실패했습니다";
    public static final String ERROR_MESSAGE_MULTIPART_ABORT_ALREADY_UPLOADED =
            "이미 업로드 완료된 파일은 중단할 수 없습니다";
    public static final String ERROR_MESSAGE_UNSUPPORTED_MIME_EXTENSION =
            "MIME 타입에 대응하는 확장자를 확인할 수 없습니다";
    public static final String ERROR_MESSAGE_EXTENSION_MIME_MISMATCH =
            "파일 확장자와 MIME 타입이 일치하지 않습니다";
    public static final String ERROR_MESSAGE_AUDIO_FILE_NAME_PATTERN_INVALID =
            "AUDIO file_name 패턴이 올바르지 않습니다. expected: YYYY-MM-DD_HH:MM:SS_<MODE>_STT_<UUID>.<ext>";
    public static final String ERROR_MESSAGE_VIDEO_FILE_NAME_PATTERN_INVALID =
            "VIDEO file_name 패턴이 올바르지 않습니다. expected: YYYY-MM-DD_HH:MM:SS_REAL_VIDEO_<UUID>.<ext>";
    public static final String ERROR_MESSAGE_CLIENT_TIMESTAMP_INVALID =
            "file_name 내 타임스탬프 형식이 올바르지 않습니다. expected: YYYY-MM-DD_HH:MM:SS";

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
