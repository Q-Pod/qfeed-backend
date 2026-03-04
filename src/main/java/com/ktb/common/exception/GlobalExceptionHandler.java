package com.ktb.common.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.dto.CommonErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.catalina.connector.ClientAbortException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.BindException;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonErrorResponse> handleBusinessException(
            BusinessException e, HttpServletRequest request) {

        log.warn("Business exception: {} - {}", e.getErrorCode(), e.getMessage());

        CommonErrorResponse response = CommonErrorResponse.of(
                e.getErrorCode(),
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(e.getStatus())
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonErrorResponse> handleValidationException(
            MethodArgumentNotValidException e, HttpServletRequest request) {

        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Validation exception: {}", detail);

        CommonErrorResponse response = CommonErrorResponse.of(
                ErrorCode.INVALID_INPUT,
                detail,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<CommonErrorResponse> handleBindException(
            BindException e, HttpServletRequest request) {

        String detail = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(", "));

        log.warn("Bind exception: {}", detail);

        CommonErrorResponse response = CommonErrorResponse.of(
                ErrorCode.INVALID_INPUT,
                detail,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<CommonErrorResponse> handleTypeMismatch(
            MethodArgumentTypeMismatchException e, HttpServletRequest request) {

        String detail = String.format("%s: %s", e.getName(), e.getValue());

        log.warn("Type mismatch: {}", detail);

        CommonErrorResponse response = CommonErrorResponse.of(
                ErrorCode.INVALID_INPUT,
                detail,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    public ResponseEntity<CommonErrorResponse> handleClientDisconnect(
            Exception e, HttpServletRequest request) {
        log.warn("Client disconnected - path={}, exception={}, message={}",
                request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());

        CommonErrorResponse response = CommonErrorResponse.of(
                ErrorCode.FILE_MULTIPART_ABORT_FAILED,
                e.getMessage(),
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonErrorResponse> handleUnexpectedException(
            Exception e, HttpServletRequest request) {

        log.error("Unexpected exception", e);

        CommonErrorResponse response = CommonErrorResponse.of(
                ErrorCode.INTERNAL_SERVER_ERROR,
                request.getRequestURI()
        );

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }
}
