package com.ktb.auth.security.handler;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.ktb.auth.security.exception.AuthFailureException;
import com.ktb.common.domain.ErrorCode;
import com.ktb.common.dto.CommonErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {
    private final JsonMapper jsonMapper =
        JsonMapper.builder()
            .addModule(new JavaTimeModule())
            .build();

    @Override
    public void commence(
        @NonNull HttpServletRequest request,
        @NonNull HttpServletResponse response,
        @NonNull AuthenticationException authException
    ) throws IOException {
        ErrorCode errorCode = ErrorCode.UNAUTHORIZED;
        String detail = null;

        if (authException instanceof AuthFailureException authFailureException) {
            errorCode = authFailureException.getErrorCode();
            detail = authFailureException.getMessage();
        }

        CommonErrorResponse errorResponse = CommonErrorResponse.of(
            errorCode,
            detail,
            request.getRequestURI()
        );

        response.setStatus(errorCode.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        jsonMapper.writeValue(response.getWriter(), errorResponse);
    }
}
