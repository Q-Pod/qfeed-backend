package com.ktb.ai.tts.controller;

import com.ktb.ai.tts.dto.request.TtsRequest;
import com.ktb.ai.tts.dto.response.TtsMultipartResponse;
import com.ktb.ai.tts.service.TtsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "TTS API", description = "Text-to-Speech 변환 API")
@RestController
@RequestMapping("/api/ai/tts")
@RequiredArgsConstructor
@Slf4j
public class TtsController {

    private static final String MULTIPART_MIXED = "multipart/mixed";

    private final TtsService ttsService;

    @PostMapping(produces = MULTIPART_MIXED)
    @Operation(summary = "TTS 변환", description = "텍스트를 음성(mp3)으로 변환하여 multipart/mixed로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "변환 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "API 키 인증 실패",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "음성 모델 없음",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "408", description = "TTS 처리 시간 초과",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "요청 한도 초과",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "500", description = "TTS 처리 실패",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "TTS 의존 서비스 실패",
                    content = @Content(schema = @Schema(implementation = com.ktb.common.dto.CommonErrorResponse.class)))
    })
    public ResponseEntity<byte[]> convert(@Valid @RequestBody TtsRequest request) {
        log.info("TTS convert request - userId={}, sessionId={}, textLength={}",
                request.userId(), request.sessionId(), request.text() == null ? 0 : request.text().length());
        TtsMultipartResponse response = ttsService.convertToSpeech(
                request.userId(),
                request.sessionId(),
                request.text()
        );

        HttpHeaders headers = new HttpHeaders();
        if (response.contentType() == null || response.contentType().isBlank()) {
            headers.setContentType(MediaType.parseMediaType(MULTIPART_MIXED));
        } else {
            headers.add(HttpHeaders.CONTENT_TYPE, response.contentType());
        }
        if (response.contentDisposition() != null && !response.contentDisposition().isBlank()) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION, response.contentDisposition());
        }
        log.info("TTS convert response ready - userId={}, sessionId={}, payloadBytes={}, contentType={}",
                request.userId(), request.sessionId(),
                response.payload() == null ? 0 : response.payload().length,
                response.contentType());

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.payload());
    }
}
