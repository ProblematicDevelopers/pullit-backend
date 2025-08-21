package com.pullit.auth.controller;

import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.auth.dto.response.OAuth2LoginResult;
import com.pullit.auth.exception.SocialLoginNewUserException;
import com.pullit.auth.service.OAuth2Service;
import com.pullit.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Authentication", description = "소셜 로그인 관련 API")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    @GetMapping("/callback/{provider}")
    @Operation(
            summary = "OAuth2 콜백 처리 (사용하지 않음)",
            description = "현재 사용하지 않는 엔드포인트입니다. /success를 사용하세요."
    )
    public ResponseEntity<ApiResponse<String>> handleOAuth2Callback(
            @PathVariable String provider) {
        
        log.info("OAuth2 callback endpoint called (deprecated) for provider: {}", provider);
        
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("DEPRECATED", "이 엔드포인트는 더 이상 사용되지 않습니다. /oauth2/success를 사용하세요."));
    }

    @GetMapping("/success")
    @Operation(
            summary = "OAuth2 로그인 성공",
            description = "OAuth2 로그인 성공 후 처리합니다."
    )
    public ResponseEntity<ApiResponse<Object>> handleOAuth2Success(HttpServletRequest request) {
        log.info("OAuth2 login success");
        
        try {
            // 세션에서 소셜 정보 가져오기 (네이버, 카카오 모두 동일하게 처리)
            HttpSession session = request.getSession();
            @SuppressWarnings("unchecked")
            Map<String, Object> socialInfo = (Map<String, Object>) session.getAttribute("oauth2_social_info");
            
            if (socialInfo == null) {
                log.warn("OAuth2 social info not found in session");
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("OAUTH2_INFO_MISSING", "OAuth2 소셜 정보를 찾을 수 없습니다."));
            }
            
            log.info("OAuth2 social info from session: {}", socialInfo);
            
            // OAuth2 서비스로 로그인 처리
            try {
                OAuth2LoginResult result = oAuth2Service.handleOAuth2LoginSuccess(socialInfo);
                return ResponseEntity.ok(
                        ApiResponse.success(result, "LOGIN_SUCCESS")
                );
            } catch (SocialLoginNewUserException e) {
                // 신규 사용자: 소셜 정보 반환
                log.info("New OAuth2 user: {}", socialInfo);
                return ResponseEntity.ok(
                        ApiResponse.success(socialInfo, "NEW_USER")
                );
            }
        } catch (Exception e) {
            log.error("OAuth2 success processing failed", e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("OAUTH2_ERROR", "OAuth2 로그인 처리 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/failure")
    @Operation(
            summary = "OAuth2 로그인 실패",
            description = "OAuth2 로그인 실패 후 처리합니다."
    )
    public ResponseEntity<ApiResponse<String>> handleOAuth2Failure() {
        log.error("OAuth2 login failed");
        return ResponseEntity.badRequest()
                .body(ApiResponse.error("OAUTH2_ERROR", "OAuth2 로그인에 실패했습니다."));
    }

    @GetMapping("/login/{provider}")
    @Operation(
            summary = "OAuth2 로그인 시작",
            description = "지정된 소셜 로그인 제공자로 로그인을 시작합니다."
    )
    public ResponseEntity<ApiResponse<String>> startOAuth2Login(
            @PathVariable String provider) {
        
        log.info("Starting OAuth2 login for provider: {}", provider);
        
        // Spring Security OAuth2 표준 경로 반환
        String loginUrl = "/oauth2/authorization/" + provider;
        
        return ResponseEntity.ok(
                ApiResponse.success(loginUrl, provider + " 로그인 URL 생성 완료")
        );
    }

    @GetMapping("/status")
    @Operation(
            summary = "OAuth2 상태 확인",
            description = "현재 OAuth2 인증 상태를 확인합니다."
    )
    public ResponseEntity<ApiResponse<String>> getOAuth2Status() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication != null && authentication.isAuthenticated()) {
            return ResponseEntity.ok(
                    ApiResponse.success("인증됨", "OAuth2 인증 상태: " + authentication.getName())
            );
        } else {
            return ResponseEntity.ok(
                    ApiResponse.success("미인증", "OAuth2 인증되지 않음")
            );
        }
    }

    @GetMapping("/social-info")
    @Operation(
            summary = "소셜 로그인 정보 조회",
            description = "소셜 로그인에서 받은 사용자 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<Object>> getSocialInfo() {
        // TODO: 세션에서 소셜 정보 조회 로직 구현
        // 현재는 임시 응답
        return ResponseEntity.ok(
                ApiResponse.success(null, "소셜 로그인 정보 조회 API")
        );
    }
} 