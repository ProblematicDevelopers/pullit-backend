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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        // 요청 정보 로깅
        log.error("Access denied error: {}", accessDeniedException.getMessage());
        log.error("Request URI: {}", request.getRequestURI());

        // 사용자 정보 로깅 (디버깅용)
        if (request.getUserPrincipal() != null) {
            log.error("User: {}", request.getUserPrincipal().getName());
        }

        // ApiResponse 형식으로 에러 응답 생성
        ApiResponse<Void> errorResponse = ApiResponse.error(ErrorCode.ACCESS_DENIED);

        // 응답 설정
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());

        // JSON으로 변환하여 응답 전송
        response.getWriter().write(objectMapper.writeValueAsString(errorResponse));
    }
}
