package com.ktb.answer.dto.request;

import com.ktb.answer.domain.AnswerType;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * 답변 목록 조회 요청 DTO
 */
@Schema(description = "답변 목록 조회 요청")
public record AnswerListRequest(

        @Parameter(description = "답변 타입 필터 (선택)", example = "PRACTICE_INTERVIEW")
        @Schema(description = "답변 타입 필터 (PRACTICE_INTERVIEW, REAL_INTERVIEW)", example = "PRACTICE_INTERVIEW")
        AnswerType type,

        @Parameter(description = "질문 카테고리 필터 (선택)", example = "OS")
        @Schema(description = "질문 카테고리 필터 (OS, NETWORK, DB, COMPUTER_ARCHITECTURE, DATA_STRUCTURE_ALGORITHM)", example = "OS")
        QuestionCategory category,

        @Parameter(description = "질문 타입 필터 (선택, MVP: CS만 허용)", example = "CS")
        @Schema(description = "질문 타입 필터 (CS, SYSTEM_DESIGN, PORTFOLIO). 현재는 CS만 지원", example = "CS")
        QuestionType questionType,

        @Parameter(description = "조회 시작 날짜 (YYYY-MM-DD, 미입력 시 dateTo 기준 1개월 전)", example = "2026-01-01")
        @Schema(description = "조회 시작 날짜 (미입력 시 dateTo 기준 1개월 전)", example = "2026-01-01", format = "date")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate dateFrom,

        @Parameter(description = "조회 종료 날짜 (YYYY-MM-DD, 미입력 시 오늘)", example = "2026-01-31")
        @Schema(description = "조회 종료 날짜 (미입력 시 오늘)", example = "2026-01-31", format = "date")
        @DateTimeFormat(pattern = "yyyy-MM-dd")
        LocalDate dateTo,

        @Parameter(description = "페이지 크기 (1-50, 기본값 10)", example = "10")
        @Schema(description = "페이지 크기 (기본값: 10, 최대: 50)", example = "10", minimum = "1", maximum = "50")
        @Min(1) @Max(50)
        Integer limit,

        @Parameter(description = "다음 페이지 커서 (Base64 인코딩, 이전 응답의 nextCursor)", example = "eyJsYXN0X2NyZWF0ZWRfYXQiOiIyMDI2LTAxLTIyVDEwOjAwOjAwIiwibGFzdF9hbnN3ZXJfaWQiOjEwfQ==")
        @Schema(description = "페이지네이션 커서 (필터/정렬 조건은 첫 요청과 동일해야 함)", example = "eyJsYXN0X2NyZWF0ZWRfYXQiOiIyMDI2LTAxLTIyVDEwOjAwOjAwIiwibGFzdF9hbnN3ZXJfaWQiOjEwfQ==")
        String cursor
) {

    private static final int DEFAULT_LIMIT = 10;
    private static final int MAX_LIMIT = 50;

    public AnswerListRequest {
        if (limit == null) {
            limit = DEFAULT_LIMIT;
        }
    }
}
