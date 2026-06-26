package com.ktb.ai.tts.controller;

import com.ktb.ai.tts.dto.request.TtsRequest;
import com.ktb.ai.tts.dto.response.TtsAudioResponse;
import com.ktb.ai.tts.service.TtsService;
import com.ktb.common.util.TelemetryUtils;
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

    private static final String AUDIO_MPEG = "audio/mpeg";
    private static final String HEADER_TTS_MESSAGE = "X-TTS-Message";
    private static final String HEADER_TTS_USER_ID = "X-TTS-User-Id";
    private static final String HEADER_TTS_SESSION_ID = "X-TTS-Session-Id";

    private final TtsService ttsService;

    @PostMapping(produces = AUDIO_MPEG)
    @Operation(summary = "TTS 변환", description = "텍스트를 음성(mp3)으로 변환하여 audio/mpeg 단일 바디로 반환합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "변환 성공",
                    content = @Content(
                            mediaType = AUDIO_MPEG,
                            schema = @Schema(type = "string", format = "binary")
                    )
            ),
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

        TelemetryUtils.attachSessionAttributes(
                request.sessionId(),
                request.userId(),
                null
        );

        TtsAudioResponse response = ttsService.convertToSpeech(
                request.userId(),
                request.sessionId(),
                request.text()
        );

        HttpHeaders headers = new HttpHeaders();
        if (response.audioContentType() == null || response.audioContentType().isBlank()) {
            headers.setContentType(MediaType.parseMediaType(AUDIO_MPEG));
        } else {
            headers.setContentType(MediaType.parseMediaType(response.audioContentType()));
        }
        if (response.fileName() != null && !response.fileName().isBlank()) {
            headers.add(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + sanitizeFileName(response.fileName()) + "\"");
        }
        if (response.message() != null && !response.message().isBlank()) {
            headers.add(HEADER_TTS_MESSAGE, response.message());
        }
        if (response.userId() != null) {
            headers.add(HEADER_TTS_USER_ID, String.valueOf(response.userId()));
        }
        if (response.sessionId() != null && !response.sessionId().isBlank()) {
            headers.add(HEADER_TTS_SESSION_ID, response.sessionId());
        }
        log.info("TTS convert response ready - userId={}, sessionId={}, payloadBytes={}, contentType={}",
                request.userId(), request.sessionId(),
                response.audioPayload() == null ? 0 : response.audioPayload().length,
                response.audioContentType());

        return ResponseEntity.ok()
                .headers(headers)
                .body(response.audioPayload());
    }

    private String sanitizeFileName(String fileName) {
        return fileName.replaceAll("[\\r\\n\"]", "");
    }
}
