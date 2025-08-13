package com.pullit.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.common.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {

        log.error("Unauthorized error: {}", authException.getMessage());
        log.error("Request URI: {}", request.getRequestURI());

        ErrorCode errorCode = determineErrorCode(request, authException);

        ApiResponse<Void> errorResponse = ApiResponse.error(errorCode);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));

    }
    private ErrorCode determineErrorCode(HttpServletRequest request,
                                         AuthenticationException authException) {

        // JwtDecoder에서 설정한 에러 속성 확인
        String errorAttribute = (String) request.getAttribute("jwt_error");

        if (errorAttribute != null) {
            return switch (errorAttribute) {
                case "expired" -> ErrorCode.EXPIRED_TOKEN;
                case "invalid" -> ErrorCode.INVALID_TOKEN;
                case "malformed" -> ErrorCode.INVALID_TOKEN;
                default -> ErrorCode.UNAUTHORIZED;
            };
        }

        // 메시지 기반 판단 (fallback)
        String message = authException.getMessage();
        if (message != null) {
            if (message.contains("expired") || message.contains("Expired")) {
                return ErrorCode.EXPIRED_TOKEN;
            }
            if (message.contains("JWT") || message.contains("token")) {
                return ErrorCode.INVALID_TOKEN;
            }
        }

        // 기본값
        return ErrorCode.UNAUTHORIZED;
    }

}
