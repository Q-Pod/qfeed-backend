package com.ktb.question.controller;

import com.ktb.common.dto.ApiResponse;
import com.ktb.question.domain.QuestionCategory;
import com.ktb.question.domain.QuestionType;
import com.ktb.question.dto.QuestionCreateRequest;
import com.ktb.question.dto.QuestionCategoryListResponse;
import com.ktb.question.dto.QuestionDetailResponse;
import com.ktb.question.dto.QuestionKeywordCheckRequest;
import com.ktb.question.dto.QuestionKeywordCheckResponse;
import com.ktb.question.dto.QuestionKeywordListResponse;
import com.ktb.question.dto.QuestionListResponse;
import com.ktb.question.dto.QuestionSearchResponse;
import com.ktb.question.dto.QuestionTypeListResponse;
import com.ktb.question.dto.QuestionUpdateRequest;
import com.ktb.question.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Question API", description = "질문 관리 API")
@RestController
@RequestMapping("/api/questions")
@RequiredArgsConstructor
@Validated
@Slf4j
public class QuestionController {

    private final QuestionService questionService;

    /**
     * 질문 목록 조회
     */
    @GetMapping
    @Operation(summary = "질문 목록 조회", description = "질문 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuestionListResponse>> getQuestions(
            @Parameter(description = "카테고리") @RequestParam(required = false) QuestionCategory category,
            @Parameter(description = "질문 유형") @RequestParam(required = false) QuestionType type,
            @Parameter(description = "커서") @RequestParam(required = false) Long cursor,
            @Parameter(description = "사이즈", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        log.info("GET /api/questions - type: {}, category: {}, cursor: {}, size: {}",
                type, category, cursor, size);
        QuestionListResponse result = questionService.getQuestions(category, type, cursor, size);
        log.info("GET /api/questions success - count: {}, hasNext: {}",
                result.questions().size(), result.pagination().hasNext());
        return ResponseEntity.ok(new ApiResponse<>("questions_retrieval_success", result));
    }

    /**
     * 질문 카테고리 목록 조회
     */
    @GetMapping("/categories")
    @Operation(summary = "질문 카테고리 목록 조회", description = "질문 카테고리 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<QuestionCategoryListResponse>> getQuestionCategories() {
        log.info("GET /api/questions/categories");
        QuestionCategoryListResponse result = questionService.getQuestionCategories();
        log.info("GET /api/questions/categories success - typeGroups: {}", result.categories().size());
        return ResponseEntity.ok(new ApiResponse<>("question_categories_retrieval_success", result));
    }

    /**
     * 질문 타입 목록 조회
     */
    @GetMapping("/types")
    @Operation(summary = "질문 타입 목록 조회", description = "질문 타입 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공")
    })
    public ResponseEntity<ApiResponse<QuestionTypeListResponse>> getQuestionTypes() {
        log.info("GET /api/questions/types");
        QuestionTypeListResponse result = questionService.getQuestionTypes();
        log.info("GET /api/questions/types success - count: {}", result.types().size());
        return ResponseEntity.ok(new ApiResponse<>("question_types_retrieval_success", result));
    }

    /**
     * 질문 상세 조회
     */
    @GetMapping("/{questionId}")
    @Operation(summary = "질문 상세 조회", description = "질문 상세를 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> getQuestionDetail(
            @Parameter(description = "질문 ID", example = "1")
            @PathVariable Long questionId
    ) {
        log.info("GET /api/questions/{}", questionId);
        QuestionDetailResponse result = questionService.getQuestionDetail(questionId);
        log.info("GET /api/questions/{} success - type: {}, category: {}",
                questionId, result.type(), result.category());
        return ResponseEntity.ok(new ApiResponse<>("question_retrieval_success", result));
    }

    /**
     * 질문 검색
     */
    @GetMapping("/search")
    @Operation(summary = "질문 검색", description = "키워드로 질문을 검색합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuestionSearchResponse>> searchQuestions(
            @Parameter(description = "검색어") @RequestParam(name = "q") String keyword,
            @Parameter(description = "카테고리") @RequestParam(name = "category", required = false) QuestionCategory category,
            @Parameter(description = "질문 유형") @RequestParam(name = "type", required = false) QuestionType type,
            @Parameter(description = "커서") @RequestParam(name = "cursor", required = false) Long cursor,
            @Parameter(description = "사이즈", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size
    ) {
        log.info("GET /api/questions/search - keyword: {}, type: {}, category: {}, cursor: {}, size: {}",
                keyword, type, category, cursor, size);
        QuestionSearchResponse result = questionService.search(keyword, category, type, cursor, size);
        log.info("GET /api/questions/search success - count: {}, hasNext: {}",
                result.questions().size(), result.pagination().hasNext());
        return ResponseEntity.ok(new ApiResponse<>("search_success", result));
    }

    /**
     * 오늘의 추천 질문 조회
     */
    @GetMapping("/recommendation")
    @Operation(summary = "오늘의 추천 질문", description = "오늘의 추천 질문을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> getDailyRecommendation() {
        log.info("GET /api/questions/recommendation");
        QuestionDetailResponse result = questionService.getDailyRecommendation();
        log.info("GET /api/questions/recommendation success - questionId: {}, type: {}, category: {}",
                result.questionId(), result.type(), result.category());
        return ResponseEntity.ok(new ApiResponse<>("question_recommendation_success", result));
    }

    /**
     * 질문 생성
     */
    @PostMapping
    @Operation(summary = "질문 생성", description = "새 질문을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> createQuestion(
            @Valid @RequestBody QuestionCreateRequest request
    ) {
        int keywordCount = request.keywords() == null ? 0 : request.keywords().size();
        log.info("POST /api/questions - type: {}, category: {}, keywordCount: {}",
                request.type(), request.category(), keywordCount);
        QuestionDetailResponse result = questionService.createQuestion(request);
        log.info("POST /api/questions success - questionId: {}", result.questionId());
        return ResponseEntity.status(201)
                .body(new ApiResponse<>("question_created_success", result));
    }

    /**
     * 질문 수정
     */
    @PatchMapping("/{questionId}")
    @Operation(summary = "질문 수정", description = "질문을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuestionDetailResponse>> updateQuestion(
            @Parameter(description = "질문 ID", example = "1")
            @PathVariable Long questionId,
            @RequestBody QuestionUpdateRequest request
    ) {
        int keywordCount = request.keywords() == null ? 0 : request.keywords().size();
        log.info("PATCH /api/questions/{} - hasContent: {}, type: {}, category: {}, useYn: {}, keywordCount: {}",
                questionId, request.content() != null, request.type(), request.category(), request.useYn(), keywordCount);
        QuestionDetailResponse result = questionService.updateQuestion(questionId, request);
        log.info("PATCH /api/questions/{} success", questionId);
        return ResponseEntity.ok(new ApiResponse<>("question_updated_success", result));
    }

    /**
     * 질문 삭제 (Soft Delete)
     */
    @DeleteMapping("/{questionId}")
    @Operation(summary = "질문 삭제", description = "질문을 삭제합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "삭제 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<Void>> deleteQuestion(
            @Parameter(description = "질문 ID", example = "1")
            @PathVariable Long questionId
    ) {
        log.info("DELETE /api/questions/{}", questionId);
        questionService.deleteQuestion(questionId);
        log.info("DELETE /api/questions/{} success", questionId);
        return ResponseEntity.ok(new ApiResponse<>("question_deleted_success", null));
    }

    /**
     * 질문 핵심 키워드 조회
     */
    @GetMapping("/{questionId}/keywords")
    @Operation(summary = "질문 키워드 조회", description = "질문 키워드 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuestionKeywordListResponse>> getQuestionKeywords(
            @Parameter(description = "질문 ID", example = "1")
            @PathVariable Long questionId
    ) {
        log.info("GET /api/questions/{}/keywords", questionId);
        QuestionKeywordListResponse result = questionService.getQuestionKeywords(questionId);
        log.info("GET /api/questions/{}/keywords success - count: {}", questionId, result.keywords().size());
        return ResponseEntity.ok(new ApiResponse<>("question_keywords_retrieval_success", result));
    }

    /**
     * 질문 핵심 키워드 포함 여부 확인
     */
    @PostMapping("/{questionId}/keyword-checks")
    @Operation(summary = "질문 키워드 체크", description = "질문 키워드 포함 여부를 확인합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<QuestionKeywordCheckResponse>> checkQuestionKeyword(
            @Parameter(description = "질문 ID", example = "1")
            @PathVariable Long questionId,
            @Valid @RequestBody QuestionKeywordCheckRequest request
    ) {
        int keywordCount = request.keywords() == null ? 0 : request.keywords().size();
        log.info("POST /api/questions/{}/keyword-checks - keywordCount: {}", questionId, keywordCount);
        QuestionKeywordCheckResponse result = questionService.checkQuestionKeywords(questionId, request.keywords());
        long includedCount = result.keywords().stream()
                .filter(match -> match.included())
                .count();
        log.info("POST /api/questions/{}/keyword-checks success - includedCount: {}",
                questionId, includedCount);
        return ResponseEntity.ok(new ApiResponse<>("question_keywords_check_success", result));
    }
}
