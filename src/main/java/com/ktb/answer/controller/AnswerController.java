package com.ktb.answer.controller;

import com.ktb.answer.dto.AnswerSubmitCommand;
import com.ktb.answer.dto.AnswerSubmitResult;
import com.ktb.answer.dto.request.AnswerDetailRequest;
import com.ktb.answer.dto.request.AnswerListRequest;
import com.ktb.answer.dto.request.AnswerSubmitRequest;
import com.ktb.answer.dto.request.SessionAnswerSubmitRequest;
import com.ktb.answer.dto.response.AnswerDetailResponse;
import com.ktb.answer.dto.response.AnswerListResponse;
import com.ktb.answer.dto.response.AnswerSubmitResponse;
import com.ktb.answer.dto.response.FeedbackResponse;
import com.ktb.answer.dto.response.SessionAnswerSubmitResponse;
import com.ktb.answer.service.AiFeedbackOrchestrator;
import com.ktb.answer.service.AnswerApplicationService;
import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Answer API", description = "답변 관리 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class AnswerController {

    private final AnswerApplicationService answerApplicationService;
    private final AiFeedbackOrchestrator aiFeedbackOrchestrator;

    private static final String MESSAGE_ANSWER_LIST_RETRIEVED = "learning_records_retrieval_success";
    private static final String MESSAGE_ANSWER_DETAIL_RETRIEVED = "record_retrieval_success";
    private static final String MESSAGE_ANSWER_SUBMITTED = "answer_submitted_success";
    private static final String MESSAGE_FEEDBACK_RETRIEVED = "feedback_retrieval_success";

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

        AnswerListResponse response = answerApplicationService.getList(
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

    @Operation(summary = "답변 상세 조회", description = "특정 답변의 상세 정보를 조회합니다 (본인만)")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    @GetMapping("/answers/{answerId}")
    public ResponseEntity<ApiResponse<AnswerDetailResponse>> getAnswerDetail(
            @Parameter(hidden = true) Long accountId,  // TODO: Spring Security에서 주입
            @Parameter(description = "답변 ID", example = "1")
            @PathVariable Long answerId,
            @Valid @ModelAttribute AnswerDetailRequest request
    ) {
        // TODO: 구현 필요
        // 1. accountId 추출
        // 2. Service 호출 (소유권 검증 포함)
        // 3. expand 파라미터 처리
        // 4. Result를 Response DTO로 변환
        // 5. ApiResponse로 래핑하여 반환

        log.info("GET /api/answers/{} - accountId: {}", answerId, accountId);

        return ResponseEntity.ok(
                new ApiResponse<>(MESSAGE_ANSWER_DETAIL_RETRIEVED, null)
        );
    }

    @Operation(summary = "답변 제출 (연습/단일)", description = "텍스트답변을 제출합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문을 찾을 수 없음")
    })
    @PostMapping(value = "/interview/answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<AnswerSubmitResponse>> submitAnswer(
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Valid @ModelAttribute AnswerSubmitRequest request
    ) {
        Long accountId = principal.getAccount().getId();
        AnswerSubmitCommand command = new AnswerSubmitCommand(request.questionId(), request.answerText(), request.answerType());

        AnswerSubmitResult submitResult = answerApplicationService.submit(accountId, command);

        log.info("POST /api/interview/answers - accountId: {}, questionId: {}",
                accountId, request.questionId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(MESSAGE_ANSWER_SUBMITTED, submitResult.from()));
    }

    @Operation(summary = "실전 세션 기반 답변 제출", description = "실전 모드 세션에서 비디오 답변을 제출합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션 상태 오류 또는 중복 답변")
    })
    @PostMapping(value = "/interview/sessions/{sessionId}/answers", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<SessionAnswerSubmitResponse>> submitSessionAnswer(
            @Parameter(hidden = true) Long accountId,  // TODO: Spring Security에서 주입
            @Parameter(description = "세션 ID", example = "uuid-string")
            @PathVariable String sessionId,
            @Valid @ModelAttribute SessionAnswerSubmitRequest request
    ) {
        // TODO: 구현 필요
        // 1. accountId 추출
        // 2. 세션 소유권 검증
        // 3. Request DTO를 Command로 변환
        // 4. Service 호출 (파일 업로드, 즉각 피드백, 세션 업데이트, 다음 질문 생성)
        // 5. Result를 Response DTO로 변환
        // 6. ApiResponse로 래핑하여 201 Created 반환

        log.info("POST /api/interview/sessions/{}/answers - accountId: {}, questionId: {}",
                sessionId, accountId, request.questionId());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(MESSAGE_ANSWER_SUBMITTED, null));
    }

    @Operation(summary = "AI 피드백 조회", description = "답변에 대한 AI 피드백을 조회합니다")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드백 완료"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "피드백 처리 중"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    @GetMapping("/interviews/answers/{answerId}/feedback")
    public ResponseEntity<ApiResponse<FeedbackResponse>> getFeedback(
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Parameter(description = "답변 ID", example = "1")
            @PathVariable Long answerId
    ) {
        Long accountId = principal.getAccount().getId();
        FeedbackResponse feedbackResponse = aiFeedbackOrchestrator.getFeedbackSync(answerId, accountId);

        log.info("GET /api/interviews/answers/{}/feedback - accountId: {}", answerId, accountId);

        return ResponseEntity.ok(
                new ApiResponse<>(MESSAGE_FEEDBACK_RETRIEVED, feedbackResponse)
        );
    }
}
