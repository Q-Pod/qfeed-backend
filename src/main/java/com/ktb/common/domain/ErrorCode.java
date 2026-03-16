package com.ktb.common.domain;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // ==================== OAuth 관련 ====================
    UNSUPPORTED_PROVIDER(400, "AUTH001", "지원하지 않는 OAuth 제공자입니다"),
    INVALID_STATE(401, "AUTH002", "state 검증에 실패했습니다"),
    INVALID_AUTHORIZATION_CODE(401, "AUTH003", "authorization code가 유효하지 않습니다"),
    OAUTH_PROVIDER_ERROR(422, "AUTH004", "OAuth 제공자 통신에 실패했습니다"),
    OAUTH_CONNECTION_NOT_FOUND(404, "AUTH005", "OAuth 연동 정보를 찾을 수 없습니다"),
    OAUTH_ALREADY_UNLINKED(409, "AUTH006", "이미 연동 해제된 OAuth입니다"),
    INVALID_EXCHANGE_CODE(401, "AUTH007", "교환 코드가 유효하지 않습니다"),

    // ==================== Token 관련 ====================
    INVALID_ACCESS_TOKEN(401, "AUTH011", "Access Token이 유효하지 않습니다"),
    INVALID_REFRESH_TOKEN(401, "AUTH012", "Refresh Token이 유효하지 않습니다"),
    MISSING_REFRESH_TOKEN(400, "AUTH013", "Refresh Token이 누락되었습니다"),
    TOKEN_REUSE_DETECTED(403, "AUTH014", "토큰 재사용이 탐지되었습니다"),
    TOKEN_HASHING_FAILED(422, "AUTH015", "토큰 해시 생성에 실패했습니다"),

    // ==================== Family/Session 관련 ====================
    FAMILY_REVOKED(401, "AUTH021", "세션이 이미 종료되었습니다"),
    FAMILY_OWNERSHIP_MISMATCH(403, "AUTH022", "세션 소유권이 일치하지 않습니다"),
    TOKEN_FAMILY_NOT_FOUND(404, "AUTH023", "토큰 패밀리를 찾을 수 없습니다"),

    // ==================== Account 관련 ====================
    ACCOUNT_NOT_FOUND(404, "AUTH031", "계정을 찾을 수 없습니다"),
    ACCOUNT_INVALID_NICKNAME(400, "AUTH032", "닉네임이 올바르지 않습니다"),

    // ==================== Question 관련 ====================
    QUESTION_NOT_FOUND(404, "Q001", "질문을 찾을 수 없습니다"),
    QUESTION_DISABLED(400, "Q002", "비활성화된 질문입니다"),
    QUESTION_ALREADY_DELETED(400, "Q003", "이미 삭제된 질문입니다"),
    QUESTION_INVALID_CONTENT(400, "Q004", "질문 내용이 올바르지 않습니다"),
    QUESTION_TYPE_REQUIRED(400, "Q005", "질문 유형은 필수입니다"),
    QUESTION_CATEGORY_REQUIRED(400, "Q006", "질문 카테고리는 필수입니다"),
    QUESTION_CONTENT_REQUIRED(400, "Q007", "질문 내용은 필수입니다"),
    QUESTION_CONTENT_HAS_SPACES(400, "Q008", "질문 내용은 앞뒤 공백을 포함할 수 없습니다"),
    QUESTION_CONTENT_TOO_SHORT(400, "Q009", "질문 내용은 2자 이상이어야 합니다"),
    QUESTION_CONTENT_TOO_LONG(400, "Q010", "질문 내용은 200자를 초과할 수 없습니다"),
    QUESTION_TYPE_CATEGORY_MISMATCH(400, "Q011", "질문 유형과 카테고리 조합이 올바르지 않습니다"),

    // ==================== Answer 관련 ====================
    ANSWER_NOT_FOUND(404, "A001", "답변을 찾을 수 없습니다"),
    ANSWER_ACCESS_DENIED(403, "A002", "답변에 대한 접근 권한이 없습니다"),
    DUPLICATE_ANSWER(409, "A003", "이미 해당 질문에 대한 답변이 존재합니다"),
    ANSWER_INVALID_CONTENT(400, "A004", "답변 내용이 올바르지 않습니다"),
    ANSWER_INVALID_STATUS_TRANSITION(400, "A005", "허용되지 않는 답변 상태 전이입니다"),
    ANSWER_TYPE_REQUIRED(400, "A006", "답변 유형은 필수입니다"),

    // ==================== Interview Session 관련 ====================
    INTERVIEW_SESSION_NOT_FOUND(404, "IS001", "인터뷰 세션을 찾을 수 없습니다"),
    INTERVIEW_SESSION_ACCESS_DENIED(403, "IS002", "인터뷰 세션 접근 권한이 없습니다"),
    INTERVIEW_SESSION_EXPIRED(409, "IS003", "인터뷰 세션이 만료되었습니다"),
    INTERVIEW_SESSION_INVALID_STATE(409, "IS004", "인터뷰 세션 상태가 올바르지 않습니다"),
    INTERVIEW_SESSION_INVALID_INPUT(400, "IS005", "인터뷰 요청 입력값이 올바르지 않습니다"),
    INTERVIEW_SESSION_INVALID_CONFIG(500, "IS006", "인터뷰 세션 설정값이 올바르지 않습니다"),

    // ==================== File 관련 ====================
    INVALID_FILE_FORMAT(400, "F001", "지원하지 않는 파일 형식입니다"),
    FILE_NOT_FOUND(404, "F002", "파일을 찾을 수 없습니다"),
    FILE_ALREADY_DELETED(409, "F003", "이미 삭제된 파일입니다"),
    FILE_SIZE_EXCEEDED(422, "F004", "파일 크기 제한을 초과했습니다"),
    FILE_STORAGE_MIGRATION_FAILED(422, "F005", "파일 저장소 마이그레이션에 실패했습니다"),
    FILE_INVALID_METADATA(400, "F006", "파일 메타데이터가 올바르지 않습니다"),
    FILE_NOT_DELETED(409, "F007", "삭제되지 않은 파일입니다"),
    FILE_PART_NUMBER_INVALID(400, "F008", "part_number는 1 이상이어야 합니다"),
    FILE_MULTIPART_ONLY_VIDEO(400, "F009", "multipart 업로드는 VIDEO 카테고리에서만 지원됩니다"),
    FILE_MULTIPART_NOT_STARTED(409, "F010", "multipart 업로드가 시작되지 않았습니다"),
    FILE_VIDEO_CONFIRM_NOT_ALLOWED(409, "F011", "VIDEO 업로드는 confirm 대신 multipart complete API를 사용해야 합니다"),
    FILE_MULTIPART_ABORT_ALREADY_UPLOADED(409, "F012", "이미 업로드 완료된 파일은 중단할 수 없습니다"),
    FILE_NAME_EXTENSION_INVALID(400, "F024", "파일명 확장자가 없거나 유효하지 않습니다"),
    FILE_UNSUPPORTED_MIME_EXTENSION(400, "F014", "MIME 타입에 대응하는 확장자를 확인할 수 없습니다"),
    FILE_EXTENSION_MIME_MISMATCH(400, "F015", "파일 확장자와 MIME 타입이 일치하지 않습니다"),
    FILE_AUDIO_FILE_NAME_PATTERN_INVALID(400, "F016", "AUDIO file_name 패턴이 올바르지 않습니다"),
    FILE_VIDEO_FILE_NAME_PATTERN_INVALID(400, "F017", "VIDEO file_name 패턴이 올바르지 않습니다"),
    FILE_CLIENT_TIMESTAMP_INVALID(400, "F018", "file_name 내 타임스탬프 형식이 올바르지 않습니다"),
    FILE_READ_FILE_ID_REQUIRED(400, "F019", "file_id는 GET/HEAD 요청에서 필수입니다"),
    FILE_MULTIPART_UPLOAD_ID_EMPTY(422, "F020", "multipart upload id 생성에 실패했습니다"),
    FILE_MULTIPART_COMPLETE_FAILED(422, "F021", "multipart 업로드 완료 처리에 실패했습니다"),
    FILE_MULTIPART_ABORT_FAILED(422, "F022", "multipart 업로드 중단 처리에 실패했습니다"),
    FILE_NOT_UPLOADED(422, "F023", "S3에 파일이 업로드되지 않았습니다"),

    // ==================== Hashtag 관련 ====================
    HASHTAG_NOT_FOUND(404, "H001", "해시태그를 찾을 수 없습니다"),
    HASHTAG_NAME_REQUIRED(400, "H002", "해시태그 이름은 필수입니다"),
    HASHTAG_NAME_TOO_LONG(400, "H003", "해시태그 이름은 100자를 초과할 수 없습니다"),

    // ==================== Metric 관련 ====================
    METRIC_NAME_REQUIRED(400, "M001", "평가 지표 이름은 필수입니다"),
    METRIC_NAME_TOO_LONG(400, "M002", "평가 지표 이름은 100자를 초과할 수 없습니다"),
    METRIC_SCORE_INVALID_RANGE(400, "M003", "평가 점수는 1-5 사이여야 합니다"),
    METRIC_NOT_FOUND(404, "M004", "평가 지표를 찾을 수 없습니다"),

    // ==================== Search 관련 ====================
    SEARCH_KEYWORD_TOO_SHORT(400, "S001", "검색어는 너무 짧습니다"),
    SEARCH_FAILED(500, "S002", "검색에 실패했습니다"),

    // ==================== AI Feedback 관련 ====================
    AI_FEEDBACK_SERVICE_ERROR(422, "AI001", "AI 피드백 서비스 오류가 발생했습니다"),
    AI_FEEDBACK_REQUEST_FAILED(422, "AI002", "AI 피드백 요청에 실패했습니다"),
    AI_FEEDBACK_TIMEOUT(422, "AI003", "AI 피드백 요청 시간이 초과되었습니다"),
    AI_FEEDBACK_EMPTY_QUESTION(400, "AI004", "질문 텍스트가 비어있습니다"),
    AI_FEEDBACK_EMPTY_ANSWER(400, "AI005", "답변 텍스트가 비어있습니다"),
    AI_FEEDBACK_ANSWER_TOO_SHORT(400, "AI006", "답변 길이가 부족합니다"),
    AI_FEEDBACK_ANSWER_TOO_LONG(400, "AI007", "답변 길이가 초과되었습니다"),
    AI_FEEDBACK_ALREADY_IN_PROGRESS(409, "AI008", "이미 AI 피드백 생성이 진행 중입니다"),
    AI_FEEDBACK_RATE_LIMIT_EXCEEDED(429, "AI009", "AI 피드백 요청 한도를 초과했습니다"),
    AI_FEEDBACK_INTERNAL_SERVER_ERROR(422, "AI010", "AI 서버 내부 오류가 발생했습니다"),
    AI_FEEDBACK_LLM_SERVICE_UNAVAILABLE(422, "AI011", "LLM 서비스 연결에 실패했습니다"),
    AI_FEEDBACK_SERVICE_TEMPORARILY_UNAVAILABLE(422, "AI012", "AI 피드백 서비스를 일시적으로 사용할 수 없습니다"),
    AI_FEEDBACK_DEPENDENCY_FAILED(502, "AI013", "AI 피드백 의존 서비스 호출에 실패했습니다"),

    // ==================== STT 관련 ====================
    STT_SERVICE_ERROR(422, "STT001", "STT 서비스 오류가 발생했습니다"),
    AUDIO_TOO_LONG(400, "STT002", "오디오 길이가 최대 허용 길이를 초과했습니다"),
    AUDIO_TOO_LARGE(400, "STT003", "오디오 파일 크기가 최대 허용 크기를 초과했습니다"),
    AUDIO_NOT_FOUND(404, "STT004", "오디오 파일을 찾을 수 없습니다"),
    AUDIO_UNPROCESSABLE(422, "STT005", "오디오 파일을 처리할 수 없습니다"),
    STT_TIMEOUT(408, "STT006", "STT 변환 시간이 초과되었습니다"),
    STT_REQUEST_FAILED(422, "STT007", "STT 요청에 실패했습니다"),

    // ==================== TTS 관련 ====================
    TTS_SERVICE_ERROR(500, "TTS001", "TTS 서비스 오류가 발생했습니다"),
    TTS_API_KEY_INVALID(401, "TTS002", "TTS API 키가 유효하지 않습니다"),
    TTS_VOICE_NOT_FOUND(404, "TTS003", "TTS 음성 모델을 찾을 수 없습니다"),
    TTS_TIMEOUT(408, "TTS004", "TTS 변환 시간이 초과되었습니다"),
    TTS_RATE_LIMIT_EXCEEDED(429, "TTS005", "TTS 요청 한도를 초과했습니다"),
    TTS_DEPENDENCY_FAILED(502, "TTS006", "TTS 의존 서비스 호출에 실패했습니다"),

    // ==================== Abuse 관련 ====================
    ABUSE_RATE_LIMIT_EXCEEDED(429, "ABUSE001", "요청 속도 제한을 초과했습니다"),
    ABUSE_DUPLICATE_CONTENT(409, "ABUSE002", "중복된 답변입니다"),
    ABUSE_LOW_QUALITY_CONTENT(400, "ABUSE003", "답변 품질이 기준에 미달합니다"),
    ABUSE_DAILY_QUOTA_EXCEEDED(429, "ABUSE004", "일일 제출 한도를 초과했습니다"),
    ABUSE_COOLDOWN_ACTIVE(429, "ABUSE005", "재시도 대기 시간이 남아있습니다"),

    // ==================== Notification 관련 ====================
    NOTICE_NOT_FOUND(404, "N001", "공지사항을 찾을 수 없습니다"),
    NOTICE_ALREADY_PUBLISHED(400, "N002", "이미 발행된 공지사항입니다"),
    NOTICE_INVALID_STATUS_TRANSITION(400, "N003", "허용되지 않는 공지사항 상태 전이입니다"),
    NOTICE_TITLE_REQUIRED(400, "N004", "공지 제목은 필수입니다"),
    NOTICE_BODY_REQUIRED(400, "N005", "공지 내용은 필수입니다"),
    NOTICE_TITLE_TOO_LONG(400, "N006", "공지 제목은 200자를 초과할 수 없습니다"),
    NOTICE_BODY_TOO_LONG(400, "N007", "공지 내용은 2000자를 초과할 수 없습니다"),
    USER_NOTIFICATION_NOT_FOUND(404, "N011", "알림을 찾을 수 없습니다"),
    USER_NOTIFICATION_ACCESS_DENIED(403, "N012", "알림에 대한 접근 권한이 없습니다"),
    USER_NOTIFICATION_TITLE_REQUIRED(400, "N013", "알림 제목은 필수입니다"),
    USER_NOTIFICATION_TITLE_TOO_LONG(400, "N014", "알림 제목은 200자를 초과할 수 없습니다"),
    NOTIFICATION_PREF_NOT_FOUND(404, "N021", "알림 수신 설정을 찾을 수 없습니다"),
    CAMPAIGN_KEY_REQUIRED(400, "N030", "캠페인 키는 필수입니다"),
    CAMPAIGN_NOT_FOUND(404, "N031", "캠페인을 찾을 수 없습니다"),
    CAMPAIGN_KEY_DUPLICATE(409, "N032", "중복된 캠페인 키입니다"),
    CAMPAIGN_INVALID_STATUS_TRANSITION(400, "N033", "허용되지 않는 캠페인 상태 전이입니다"),
    CAMPAIGN_KEY_TOO_LONG(400, "N034", "캠페인 키는 200자를 초과할 수 없습니다"),
    CAMPAIGN_SCHEDULED_AT_REQUIRED(400, "N035", "캠페인 예약 시간은 필수입니다"),
    CAMPAIGN_SCHEDULE_FAILED(422, "N036", "캠페인 스케줄링에 실패했습니다"),

    // ==================== Device 관련 ====================
    DEVICE_NOT_FOUND(404, "DEV001", "디바이스를 찾을 수 없습니다"),
    DEVICE_TOKEN_REQUIRED(400, "DEV002", "푸시 토큰이 필요합니다"),
    DEVICE_ALREADY_REGISTERED(409, "DEV003", "이미 등록된 디바이스입니다"),

    // ==================== Notification Target 관련 ====================
    NOTIFICATION_TARGET_NOT_FOUND(404, "NT001", "발송 대상을 찾을 수 없습니다"),
    NOTIFICATION_TARGET_DUPLICATE(409, "NT002", "중복 발송 대상입니다"),


    // ==================== Portfolio 관련 ====================
    PORTFOLIO_PROJECT_LIMIT_EXCEEDED(400, "PORT001", "포트폴리오당 3개의 프로젝트만 가질 수 있습니다."),
    PORTFOLIO_NOT_FOUND(404, "PORT002", "포트폴리오를 찾을 수 없습니다."),

    // ==================== Project 관련 ====================
    PROJECT_REQUIRED(400, "PRO001", "프로젝트는 필수입니다."),
    TECH_STACK_REQUIRED(400, "PRO002", "기술 스택은 필수입니다."),

    // ==================== 공통 ====================
    INVALID_INPUT(400, "C001", "입력값이 올바르지 않습니다"),
    INTERNAL_SERVER_ERROR(422, "C002", "서버 내부 오류가 발생했습니다"),
    FORBIDDEN(403, "C004", "접근 권한이 없습니다"),
    UNAUTHORIZED(401, "AUTH901", "인증이 필요합니다"),
    TOO_MANY_REQUESTS(429, "AUTH903", "요청이 너무 많습니다");

    private final int status;
    private final String code;
    private final String message;
}
