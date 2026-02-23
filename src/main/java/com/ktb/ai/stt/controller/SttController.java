package com.ktb.ai.stt.controller;

import com.ktb.ai.stt.dto.request.SttRequest;
import com.ktb.ai.stt.dto.response.SttResponse;
import com.ktb.ai.stt.service.SttService;
import com.ktb.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "STT API", description = "Speech-to-Text 변환 API")
@RestController
@RequestMapping("/api/ai/stt")
@RequiredArgsConstructor
@Slf4j
public class SttController {

    private static final String MESSAGE_STT_CONVERTED = "speech_to_text_success";

    private final SttService sttService;

    @PostMapping
    @Operation(summary = "STT 변환", description = "오디오 파일을 텍스트로 변환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변환 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "STT 처리 실패",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<SttResponse>> convert(
            @Valid @RequestBody SttRequest request
    ) {
        log.info("STT convert request - userId={}, sessionId={}", request.userId(), request.sessionId());
        String text = sttService.convertToText(
                request.userId(),
                request.sessionId(),
                request.audioUrl()
        );
        log.info("STT convert response ready - userId={}, sessionId={}, textLength={}",
                request.userId(), request.sessionId(), text == null ? 0 : text.length());

        return ResponseEntity.ok(
                new ApiResponse<>(MESSAGE_STT_CONVERTED,
                        new SttResponse(request.userId(), request.sessionId(), text))
        );
    }
}
