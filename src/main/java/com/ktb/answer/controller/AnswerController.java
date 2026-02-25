package com.ktb.answer.controller;

import com.ktb.answer.dto.AnswerDetailResult;
import com.ktb.answer.dto.request.AnswerListRequest;
import com.ktb.answer.dto.response.detail.AnswerDetailResponse;
import com.ktb.answer.dto.response.list.AnswerListResponse;
import com.ktb.answer.exception.AnswerDetailInvalidInputException;
import com.ktb.answer.service.AnswerDomainService;
import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Answer API", description = "답변 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AnswerController {

    private final AnswerDomainService answerDomainService;

    private static final String MESSAGE_ANSWER_LIST_RETRIEVED = "learning_records_retrieval_success";
    private static final String MESSAGE_ANSWER_DETAIL_RETRIEVED = "record_retrieval_success";

    @Operation(summary = "답변 목록 조회",
            description = "사용자의 학습 기록 목록을 조회합니다 (본인만). "
                    + "날짜 필터 미입력 시 최근 1개월 기준으로 조회됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @GetMapping("/answers")
    public ResponseEntity<ApiResponse<AnswerListResponse>> getAnswerList(
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Valid @ParameterObject AnswerListRequest request
    ) {
        Long accountId = principal.getAccount().getId();

        AnswerListResponse response = answerDomainService.getList(
                accountId,
                request.type(),
                request.category(),
                request.questionType(),
                request.dateFrom(),
                request.dateTo(),
                request.cursor(),
                request.limit()
        );

        log.info("GET /api/answers - accountId: {}, size: {}", accountId, response.records().size());

        return ResponseEntity.ok(
                new ApiResponse<>(MESSAGE_ANSWER_LIST_RETRIEVED, response)
        );
    }

    @Operation(
            summary = "답변 상세 조회",
            description = "특정 답변의 상세 정보를 조회합니다 (본인만). "
                    + "REAL_INTERVIEW 답변은 세션 최종 피드백(토픽/종합 피드백, 인터뷰 히스토리, 메트릭)을 함께 반환합니다."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    @GetMapping("/answers/{answerId}")
    public ResponseEntity<ApiResponse<AnswerDetailResponse>> getAnswerDetail(
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Parameter(description = "답변 ID", example = "1")
            @PathVariable Long answerId,
            HttpServletRequest request
    ) {
        Long accountId = principal.getAccount().getId();
        if (!request.getParameterMap().isEmpty()) {
            log.warn("GET /api/answers/{} rejected - accountId={}, unsupportedQueryParams={}",
                    answerId, accountId, request.getParameterMap().keySet());
            throw new AnswerDetailInvalidInputException(
                    "answer detail endpoint does not support query parameters"
            );
        }

        log.info("GET /api/answers/{} - accountId: {}", answerId, accountId);

        AnswerDetailResult detailResult = answerDomainService.getDetail(accountId, answerId);
        AnswerDetailResponse response = AnswerDetailResponse.of(detailResult);

        return ResponseEntity.ok(
                new ApiResponse<>(MESSAGE_ANSWER_DETAIL_RETRIEVED, response)
        );
    }
}
