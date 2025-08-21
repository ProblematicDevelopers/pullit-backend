package com.pullit.auth.controller;

import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.auth.exception.SocialLoginNewUserException;
import com.pullit.auth.service.OAuth2Service;
import com.pullit.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth/oauth2")
@RequiredArgsConstructor
@Tag(name = "OAuth2 Authentication", description = "소셜 로그인 관련 API")
public class OAuth2Controller {

    private final OAuth2Service oAuth2Service;

    @GetMapping("/callback/{provider}")
    @Operation(
            summary = "OAuth2 콜백 처리",
            description = "소셜 로그인 후 콜백을 처리하고 JWT 토큰을 반환합니다."
    )
    public ResponseEntity<ApiResponse<Object>> handleOAuth2Callback(
            @PathVariable String provider) {
        
        log.info("OAuth2 callback received for provider: {}", provider);
        
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            LoginResponse response = oAuth2Service.handleOAuth2LoginSuccess(authentication);
            
            // 기존 사용자: 로그인 성공
            return ResponseEntity.ok(
                    ApiResponse.success(response, "LOGIN_SUCCESS")
            );
        } catch (SocialLoginNewUserException e) {
            // 신규 사용자: 소셜 정보를 세션에 저장하고 상태 전달
            log.info("New OAuth2 user for provider: {}", provider);
            // TODO: 세션에 소셜 정보 저장 로직 추가
            
            return ResponseEntity.ok(
                    ApiResponse.success(null, "NEW_USER")
            );
        } catch (Exception e) {
            log.error("OAuth2 callback processing failed for provider: {}", provider, e);
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("OAUTH2_ERROR", "OAuth2 로그인 처리 실패: " + e.getMessage()));
        }
    }

    @GetMapping("/login/{provider}")
    @Operation(
            summary = "OAuth2 로그인 시작",
            description = "지정된 소셜 로그인 제공자로 로그인을 시작합니다."
    )
    public ResponseEntity<ApiResponse<String>> startOAuth2Login(
            @PathVariable String provider) {
        
        log.info("Starting OAuth2 login for provider: {}", provider);
        
        // Spring Security OAuth2가 자동으로 올바른 URL 생성
        // 각 제공자별로 다른 OAuth2 URL을 자동으로 처리
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