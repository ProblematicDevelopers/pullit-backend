package com.pullit.auth.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.auth.service.AuthService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * OAuth2 인증 성공 핸들러
 * OAuth2 로그인 성공 시 JWT 토큰을 생성하고 프론트엔드로 리다이렉트합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final AuthService authService;
    private final ObjectMapper objectMapper;
    
    @Value("${oauth2.success.redirect-uri:http://localhost:5173/oauth2/callback}")
    private String redirectUri;
    
    @Value("${oauth2.success.response-type:redirect}")
    private String responseType; // "redirect" or "json"

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, 
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        Map<String, Object> attributes = oAuth2User.getAttributes();
        
        log.info("OAuth2 authentication successful for user with attributes: {}", attributes);
        
        try {
            // OAuth2 사용자 정보 추출
            String provider = (String) attributes.get("provider");
            String providerId = (String) attributes.get("providerId");
            String email = (String) attributes.get("email");
            String name = (String) attributes.get("name");
            
            log.info("Processing OAuth2 login - Provider: {}, ProviderId: {}, Email: {}, Name: {}", 
                    provider, providerId, email, name);
            
            // AuthService를 통해 사용자 생성 또는 로그인 처리
            LoginResponse loginResponse = authService.socialLogin(provider, providerId, email, name);
            
            log.info("JWT tokens generated successfully for OAuth2 user: {}", email);
            
            // 응답 타입에 따라 처리
            if ("json".equalsIgnoreCase(responseType)) {
                // JSON 응답 (API 방식)
                sendJsonResponse(response, loginResponse);
            } else {
                // 리다이렉트 응답 (기본)
                sendRedirectResponse(request, response, loginResponse);
            }
            
        } catch (Exception e) {
            log.error("Failed to process OAuth2 authentication success", e);
            
            // 에러 발생 시 에러 페이지로 리다이렉트
            String errorUrl = UriComponentsBuilder.fromUriString(redirectUri)
                    .queryParam("error", "authentication_failed")
                    .queryParam("message", URLEncoder.encode(e.getMessage(), StandardCharsets.UTF_8))
                    .build()
                    .toUriString();
            
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
        }
    }

    /**
     * JSON 형태로 응답 (주로 API 테스트용)
     */
    private void sendJsonResponse(HttpServletResponse response, LoginResponse loginResponse) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_OK);
        
        // CORS 헤더 추가: 요청 Origin 반영 (전역 CORS에서 검증)
        // Note: This handler isn't actively used in current flow, but make it safe
        // by echoing Origin to support various environments.
        // When no Origin, fall back to wildcard (non-credentialed) behavior is omitted
        // because we always use credentials.
        // In practice, Security CORS config governs access.
        // This header is only informational here.
        // Use request origin if available.
        // response.setHeader is idempotent; set credentials as true.
        // We cannot access request here; leaving origin header unset relies on global CORS.
        response.setHeader("Access-Control-Allow-Credentials", "true");
        
        // JSON 응답 작성
        Map<String, Object> result = Map.of(
                "success", true,
                "data", loginResponse,
                "message", "OAuth2 authentication successful"
        );
        
        response.getWriter().write(objectMapper.writeValueAsString(result));
        response.getWriter().flush();
    }

    /**
     * 프론트엔드로 리다이렉트 (기본 동작)
     * JWT 토큰을 쿼리 파라미터로 전달합니다.
     */
    private void sendRedirectResponse(HttpServletRequest request, 
                                       HttpServletResponse response, 
                                       LoginResponse loginResponse) throws IOException {
        
        // 프론트엔드 콜백 URL 구성
        String targetUrl = UriComponentsBuilder.fromUriString(redirectUri)
                .queryParam("success", true)
                .queryParam("accessToken", loginResponse.getAccessToken())
                .queryParam("refreshToken", loginResponse.getRefreshToken())
                .queryParam("tokenType", loginResponse.getTokenType())
                .queryParam("expiresIn", loginResponse.getExpiresIn())
                // 사용자 정보도 전달 (선택사항)
                .queryParam("userId", loginResponse.getUser().getId())
                .queryParam("username", URLEncoder.encode(loginResponse.getUser().getUsername(), StandardCharsets.UTF_8))
                .queryParam("email", URLEncoder.encode(loginResponse.getUser().getEmail(), StandardCharsets.UTF_8))
                .queryParam("fullName", URLEncoder.encode(loginResponse.getUser().getFullName(), StandardCharsets.UTF_8))
                .queryParam("role", loginResponse.getUser().getRole())
                .build()
                .toUriString();
        
        log.info("Redirecting to frontend with JWT tokens: {}", targetUrl);
        
        // 리다이렉트 수행
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    /**
     * 동적으로 리다이렉트 URI 설정 (필요시 사용)
     */
    public void setRedirectUri(String redirectUri) {
        this.redirectUri = redirectUri;
    }

    /**
     * 응답 타입 설정 (필요시 사용)
     */
    public void setResponseType(String responseType) {
        this.responseType = responseType;
    }
}
