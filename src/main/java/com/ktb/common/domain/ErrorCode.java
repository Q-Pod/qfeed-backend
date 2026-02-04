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

    // ==================== Answer 관련 ====================
    ANSWER_NOT_FOUND(404, "A001", "답변을 찾을 수 없습니다"),
    ANSWER_ACCESS_DENIED(403, "A002", "답변에 대한 접근 권한이 없습니다"),
    DUPLICATE_ANSWER(409, "A003", "이미 해당 질문에 대한 답변이 존재합니다"),
    ANSWER_INVALID_CONTENT(400, "A004", "답변 내용이 올바르지 않습니다"),
    ANSWER_INVALID_STATUS_TRANSITION(400, "A005", "허용되지 않는 답변 상태 전이입니다"),
    ANSWER_TYPE_REQUIRED(400, "A006", "답변 유형은 필수입니다"),

    // ==================== File 관련 ====================
    INVALID_FILE_FORMAT(400, "F001", "지원하지 않는 파일 형식입니다"),
    FILE_NOT_FOUND(404, "F002", "파일을 찾을 수 없습니다"),
    FILE_ALREADY_DELETED(409, "F003", "이미 삭제된 파일입니다"),
    FILE_SIZE_EXCEEDED(422, "F004", "파일 크기 제한을 초과했습니다"),
    FILE_STORAGE_MIGRATION_FAILED(422, "F005", "파일 저장소 마이그레이션에 실패했습니다"),
    FILE_INVALID_METADATA(400, "F006", "파일 메타데이터가 올바르지 않습니다"),
    FILE_NOT_DELETED(409, "F007", "삭제되지 않은 파일입니다"),

    // ==================== Hashtag 관련 ====================
    HASHTAG_NOT_FOUND(404, "H001", "해시태그를 찾을 수 없습니다"),
    HASHTAG_NAME_REQUIRED(400, "H002", "해시태그 이름은 필수입니다"),
    HASHTAG_NAME_TOO_LONG(400, "H003", "해시태그 이름은 100자를 초과할 수 없습니다"),

    // ==================== Metric 관련 ====================
    METRIC_NAME_REQUIRED(400, "M001", "평가 지표 이름은 필수입니다"),
    METRIC_NAME_TOO_LONG(400, "M002", "평가 지표 이름은 100자를 초과할 수 없습니다"),
    METRIC_SCORE_INVALID_RANGE(400, "M003", "평가 점수는 0-100 사이여야 합니다"),
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

    // ==================== STT 관련 ====================
    STT_SERVICE_ERROR(422, "STT001", "STT 서비스 오류가 발생했습니다"),
    AUDIO_TOO_LONG(400, "STT002", "오디오 길이가 최대 허용 길이를 초과했습니다"),
    AUDIO_TOO_LARGE(400, "STT003", "오디오 파일 크기가 최대 허용 크기를 초과했습니다"),
    AUDIO_NOT_FOUND(404, "STT004", "오디오 파일을 찾을 수 없습니다"),
    AUDIO_UNPROCESSABLE(422, "STT005", "오디오 파일을 처리할 수 없습니다"),
    STT_TIMEOUT(408, "STT006", "STT 변환 시간이 초과되었습니다"),
    STT_REQUEST_FAILED(422, "STT007", "STT 요청에 실패했습니다"),

    // ==================== Abuse 관련 ====================
    ABUSE_RATE_LIMIT_EXCEEDED(429, "ABUSE001", "요청 속도 제한을 초과했습니다"),
    ABUSE_DUPLICATE_CONTENT(409, "ABUSE002", "중복된 답변입니다"),
    ABUSE_LOW_QUALITY_CONTENT(400, "ABUSE003", "답변 품질이 기준에 미달합니다"),
    ABUSE_DAILY_QUOTA_EXCEEDED(429, "ABUSE004", "일일 제출 한도를 초과했습니다"),
    ABUSE_COOLDOWN_ACTIVE(429, "ABUSE005", "재시도 대기 시간이 남아있습니다"),

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
