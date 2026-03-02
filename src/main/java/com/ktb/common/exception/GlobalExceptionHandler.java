package com.ktb.common.exception;

import com.ktb.common.domain.ErrorCode;
import com.ktb.common.dto.CommonErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
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

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonErrorResponse> handleException(
            Exception e, HttpServletRequest request) {

        if (isClientAbortException(e)) {
            log.warn("Client aborted request - path={}, message={}", request.getRequestURI(), e.getMessage());
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

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

    private boolean isClientAbortException(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            String className = current.getClass().getName();
            String message = current.getMessage();
            if ("org.apache.catalina.connector.ClientAbortException".equals(className)
                    || "org.springframework.web.context.request.async.AsyncRequestNotUsableException".equals(className)) {
                return true;
            }
            if (message != null
                    && (message.contains("Broken pipe") || message.contains("Connection reset by peer"))) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
