package com.ktb.interview.controller;

import com.ktb.interview.dto.ai.InterviewFeedbackDataResponse;
import com.ktb.interview.dto.request.InterviewSessionCreateRequest;
import com.ktb.interview.dto.request.PracticeAnswerSubmitRequest;
import com.ktb.interview.dto.request.RealAnswerSubmitRequest;
import com.ktb.interview.dto.request.SessionFinalFeedbackRequest;
import com.ktb.interview.dto.response.session.InterviewPracticeSubmitResponse;
import com.ktb.interview.dto.response.session.InterviewSessionCreateResponse;
import com.ktb.interview.dto.response.session.InterviewSessionFinalFeedbackResponse;
import com.ktb.interview.dto.response.session.InterviewRealSubmitResponse;
import com.ktb.interview.dto.response.session.InterviewSessionStateResponse;
import com.ktb.interview.dto.response.session.SessionFeedbackFailedResponse;
import com.ktb.interview.dto.response.session.SessionFeedbackPendingResponse;
import com.ktb.interview.application.InterviewSessionManagementService;
import com.ktb.interview.application.InterviewSubmissionService;
import com.ktb.interview.session.domain.InterviewSessionStatus;
import com.ktb.auth.security.adapter.SecurityUserAccount;
import com.ktb.common.dto.ApiResponse;
import com.ktb.common.util.HttpRequestUtils;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 세션 기반 면접(연습/실전) 플로우 전용 컨트롤러.
 * 답변 목록/상세 조회와 분리해 인터뷰 도메인 책임을 명확히 유지합니다.
 */
@Tag(name = "Interview Session API", description = "세션 기반 인터뷰 API")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Slf4j
public class InterviewSessionController {

    private static final String HEADER_X_FORWARDED_FOR = "X-Forwarded-For";
    private static final String HEADER_X_REAL_IP = "X-Real-IP";
    private static final String SESSION_STATUS_COMPLETED = InterviewSessionStatus.COMPLETED.name();
    private static final String SESSION_STATUS_FAILED = InterviewSessionStatus.FAILED.name();

    private final InterviewSubmissionService interviewSubmissionService;
    private final InterviewSessionManagementService interviewSessionManagementService;

    /**
     * 인터뷰 세션을 생성합니다.
     */
    @Operation(summary = "인터뷰 세션 생성", description = "연습/실전 모드 인터뷰 세션을 생성합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "세션 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 질문 생성 의존 서비스 호출 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요")
    })
    @PostMapping("/interview/sessions")
    public ResponseEntity<ApiResponse<InterviewSessionCreateResponse>> createSession(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Valid @RequestBody InterviewSessionCreateRequest request
    ) {
        Long accountId = principal.getAccount().getId();
        log.info("createSession request - accountId={}, interviewType={}, questionType={}, category={}",
                accountId, request.interviewType(), request.questionType(), request.category());
        InterviewSessionCreateResponse data = interviewSessionManagementService.createSession(accountId, request);
        log.info("createSession response - accountId={}, sessionId={}, interviewType={}",
                accountId, data.sessionId(), data.interviewType());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("session_created_success", data));
    }

    /**
     * 인터뷰 세션 현재 상태와 이력을 조회합니다.
     */
    @Operation(summary = "인터뷰 세션 상태 조회", description = "세션의 현재 상태와 인터뷰 이력을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션 만료 또는 상태 오류")
    })
    @GetMapping({"/interview/sessions", "/interview/sessions/"})
    public ResponseEntity<ApiResponse<InterviewSessionStateResponse>> getSessionState(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Parameter(description = "인터뷰 세션 ID(query)", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("sessionId") String sessionId
    ) {
        Long accountId = principal.getAccount().getId();
        log.debug("getSessionState request - accountId={}, sessionId={}", accountId, sessionId);
        InterviewSessionStateResponse data = interviewSessionManagementService.getSessionState(accountId, sessionId);
        log.debug("getSessionState response - accountId={}, sessionId={}, status={}",
                accountId, sessionId, data.status());
        return ResponseEntity.ok(new ApiResponse<>("session_retrieval_success", data));
    }

    /**
     * 세션 상태에 따라 피드백 완료/실패/진행중 응답을 분기 반환합니다.
     */
    @Operation(summary = "인터뷰 세션 피드백 조회", description = "세션 상태에 따라 진행중(202)/완료(200)/실패(200) 응답을 반환합니다. 프론트엔드 폴링 조회용 API입니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "피드백 완료 또는 실패"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "202", description = "피드백 처리 중"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션 상태 오류")
    })
    @GetMapping({"/interview/sessions/feedback", "/interview/sessions/feedback/"})
    public ResponseEntity<ApiResponse<?>> getSessionFeedback(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Parameter(description = "인터뷰 세션 ID(query)", example = "550e8400-e29b-41d4-a716-446655440000")
            @RequestParam("sessionId") String sessionId
    ) {
        Long accountId = principal.getAccount().getId();
        log.debug("getSessionFeedback request - accountId={}, sessionId={}", accountId, sessionId);
        InterviewSessionStateResponse state = interviewSessionManagementService.getSessionState(accountId, sessionId);

        // 상태 기반으로 응답 타입/코드를 분기합니다.
        if (SESSION_STATUS_COMPLETED.equals(state.status())) {
            InterviewFeedbackDataResponse data = interviewSessionManagementService
                    .getSessionFeedbackCompleted(accountId, sessionId)
                    .withStatus(SESSION_STATUS_COMPLETED);
            if ("REAL_INTERVIEW".equalsIgnoreCase(state.interviewType())) {
                data = data.withoutIdentifiers();
            }
            log.debug("getSessionFeedback response - accountId={}, sessionId={}, status={}",
                    accountId, sessionId, state.status());
            return ResponseEntity.ok(new ApiResponse<>("feedback_retrieval_success", data));
        }

        if (SESSION_STATUS_FAILED.equals(state.status())) {
            SessionFeedbackFailedResponse data = interviewSessionManagementService.getSessionFeedbackFailed(accountId, sessionId);
            log.debug("getSessionFeedback response - accountId={}, sessionId={}, status={}",
                    accountId, sessionId, state.status());
            return ResponseEntity.ok(new ApiResponse<>("feedback_retrieval_success", data));
        }

        SessionFeedbackPendingResponse data = interviewSessionManagementService.getSessionFeedbackPending(accountId, sessionId);
        log.debug("getSessionFeedback response - accountId={}, sessionId={}, status={}",
                accountId, sessionId, state.status());
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(new ApiResponse<>("feedback_processing", data));
    }

    /**
     * 연습 모드 답변을 제출하고 세션 이력에 기록합니다.
     */
    @Operation(summary = "연습 모드 답변 제출", description = "연습 모드 단일 답변을 세션 이력에 기록합니다. 최종 피드백은 세션 피드백 요청 API로 분리됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문/세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션 상태 오류")
    })
    @PostMapping("/answers/practice")
    public ResponseEntity<ApiResponse<InterviewPracticeSubmitResponse>> submitPractice(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Valid @RequestBody PracticeAnswerSubmitRequest request,
            @Parameter(hidden = true)
            HttpServletRequest httpRequest
    ) {
        Long accountId = principal.getAccount().getId();
        String clientIp = HttpRequestUtils.extractClientIp(httpRequest, HEADER_X_FORWARDED_FOR, HEADER_X_REAL_IP);
        log.info("submitPractice request - accountId={}, sessionId={}, questionId={}, clientIp={}",
                accountId, request.sessionId(), request.questionId(), clientIp);
        InterviewPracticeSubmitResponse data = interviewSubmissionService.submitPractice(accountId, request, clientIp);
        log.info("submitPractice response - accountId={}, sessionId={}, status={}",
                accountId, data.sessionId(), data.status());

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("answer_submission_success", data));
    }

    /**
     * 실전 모드 답변을 제출하고 다음 turn 정보를 반환합니다.
     */
    @Operation(summary = "실전 모드 답변 제출", description = "실전 세션의 현재 질문에 대한 답변을 제출하고 다음 질문/세션 종료 여부를 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "제출 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "질문/세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션 상태 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 피드백 의존 서비스 호출 실패")
    })
    @PostMapping("/answers/real")
    public ResponseEntity<ApiResponse<InterviewRealSubmitResponse>> submitReal(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Valid @RequestBody RealAnswerSubmitRequest request,
            @Parameter(hidden = true)
            HttpServletRequest httpRequest
    ) {
        Long accountId = principal.getAccount().getId();
        String clientIp = HttpRequestUtils.extractClientIp(httpRequest, HEADER_X_FORWARDED_FOR, HEADER_X_REAL_IP);
        log.info("submitReal request - accountId={}, sessionId={}, questionType={}, clientIp={}",
                accountId, request.sessionId(), request.questionType(), clientIp);
        InterviewRealSubmitResponse data = interviewSubmissionService.submitReal(accountId, request, clientIp);
        String message = data.badCaseFeedback() != null ? "bad_case_detected" : "generate_feedback_success";
        log.info("submitReal response - accountId={}, sessionId={}, status={}, badCase={}",
                accountId, data.sessionId(), data.status(), data.badCaseFeedback() != null);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>(message, data));
    }

    /**
     * 연습/실전 세션에 대해 최종 AI 피드백을 생성합니다.
     */
    @Operation(summary = "세션 최종 피드백 요청", description = "요청 body의 sessionId를 기준으로 세션 누적 이력 최종 AI 피드백을 생성합니다. 실전 모드는 세션 종료 후 호출해야 합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "피드백 생성 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "세션 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "세션을 찾을 수 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "세션 상태 오류"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI 피드백 의존 서비스 호출 실패")
    })
    @PostMapping({"/interview/sessions/feedback/request", "/interview/sessions/feedback/request/"})
    public ResponseEntity<ApiResponse<InterviewSessionFinalFeedbackResponse>> requestSessionFinalFeedback(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Valid @RequestBody SessionFinalFeedbackRequest request,
            @Parameter(hidden = true)
            HttpServletRequest httpRequest
    ) {
        Long accountId = principal.getAccount().getId();
        String clientIp = HttpRequestUtils.extractClientIp(httpRequest, HEADER_X_FORWARDED_FOR, HEADER_X_REAL_IP);
        String sessionId = request.sessionId();
        log.info("requestSessionFinalFeedback request - accountId={}, sessionId={}, clientIp={}",
                accountId, sessionId, clientIp);
        InterviewSessionFinalFeedbackResponse data = interviewSubmissionService.requestSessionFinalFeedback(
                accountId,
                sessionId,
                clientIp
        );
        log.info("requestSessionFinalFeedback response - accountId={}, sessionId={}, status={}",
                accountId, data.sessionId(), data.status());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ApiResponse<>("generate_feedback_success", data));
    }

    /**
     * 저장된 연습 답변 피드백을 조회합니다.
     */
    @Operation(summary = "연습 모드 피드백 조회", description = "저장된 연습 답변의 AI 피드백을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 필요"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "답변 접근 권한 없음"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "답변을 찾을 수 없음")
    })
    @GetMapping("/answers/practice/{answerId}/feedback")
    public ResponseEntity<ApiResponse<InterviewFeedbackDataResponse>> getPracticeFeedback(
            @Parameter(hidden = true)
            @AuthenticationPrincipal SecurityUserAccount principal,
            @Parameter(description = "답변 ID", example = "1")
            @PathVariable Long answerId
    ) {
        Long accountId = principal.getAccount().getId();
        log.info("getPracticeFeedback request - accountId={}, answerId={}", accountId, answerId);
        InterviewFeedbackDataResponse data = interviewSubmissionService.getPracticeFeedback(accountId, answerId);
        log.info("getPracticeFeedback response - accountId={}, answerId={}", accountId, answerId);
        return ResponseEntity.ok(new ApiResponse<>("feedback_retrieval_success", data));
    }
}
