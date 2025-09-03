package com.pullit.auth.controller;

import com.pullit.auth.dto.request.LoginRequest;
import com.pullit.auth.dto.request.TokenRefreshRequest;
import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.auth.dto.response.TokenValidationResponse;
import com.pullit.auth.service.AuthService;
import com.pullit.auth.service.OAuth2Service;
import com.pullit.auth.exception.SocialLoginNewUserException;
import com.pullit.common.annotation.LoggingTrace;
import com.pullit.common.annotation.RateLimited;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.teacher.service.TeacherService;
import com.pullit.user.dto.request.UserCreateRequest;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;
    private final com.pullit.auth.service.EnhancedJwtService jwtService;
    private final UserService userService;
    private final TeacherService teacherService;
    private final com.pullit.auth.service.ActiveSessionService activeSessionService;
    private final OAuth2Service oAuth2Service;

    @PostMapping("/login")
    @Operation(
            summary = "로그인",
            description = "사용자명과 비밀번호를 사용하여 로그인하고 JWT 토큰을 발급받습니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "로그인 성공",
                    content = @Content(schema = @Schema(implementation = LoginResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패 (잘못된 사용자명 또는 비밀번호)"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "403",
                    description = "계정이 잠김"
            )
    })
    @LoggingTrace
    @RateLimited(limit = 5, duration = 1, timeUnit = TimeUnit.MINUTES)
    public ResponseEntity<ApiResponse<LoginResponse>> login(
            @Valid @RequestBody LoginRequest request) {

        log.info("Login request received for username: {}", request.getUsername());


        LoginResponse response = authService.login(request);

        return ResponseEntity.ok(
                ApiResponse.success(response, "로그인 성공")
        );
    }

    @GetMapping("/oauth2/success")
    @Operation(
            summary = "OAuth2 성공 처리",
            description = "세션에 저장된 소셜 정보를 기반으로 로그인 완료 또는 신규 사용자 정보를 반환합니다."
    )
    public ResponseEntity<ApiResponse<?>> oauth2Success(jakarta.servlet.http.HttpServletRequest request) {
        try {
            // 세션 기반 소셜 정보 우선 처리
            jakarta.servlet.http.HttpSession session = request.getSession(false);
            if (session != null) {
                @SuppressWarnings("unchecked")
                java.util.Map<String, Object> socialInfo = (java.util.Map<String, Object>) session.getAttribute("oauth2_social_info");
                if (socialInfo != null) {
                    var result = oAuth2Service.handleOAuth2LoginSuccess(socialInfo);
                    return ResponseEntity.ok(ApiResponse.success(result, "LOGIN_SUCCESS"));
                }
            }

            // 레거시 흐름 호환: Authentication 기반 처리 (명시적 캐스팅으로 오버로드 모호성 제거)
            var loginResponse = oAuth2Service.handleOAuth2LoginSuccess((org.springframework.security.core.Authentication) null);
            return ResponseEntity.ok(ApiResponse.success(loginResponse, "LOGIN_SUCCESS"));
        } catch (SocialLoginNewUserException e) {
            // 신규 사용자: 소셜 정보 반환, 메시지를 NEW_USER로 전달
            return ResponseEntity.ok(ApiResponse.success(e.getSocialInfo(), "NEW_USER"));
        } catch (Exception e) {
            return ResponseEntity.ok(ApiResponse.error("OAUTH2_FAILED", e.getMessage()));
        }
    }

    @PostMapping("/register")
    @Operation(
            summary = "회원가입",
            description = "새로운 사용자를 등록합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "201",
                    description = "회원가입 성공",
                    content = @Content(schema = @Schema(implementation = UserResponse.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "중복된 사용자명 또는 이메일"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "잘못된 입력값"
            )
    })
    @LoggingTrace
//    @RateLimited(limit = 3, duration = 1, timeUnit = TimeUnit.MINUTES)
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserCreateRequest request) {

        log.info("회원가입 요청: {}", request.getUsername());

        // 모든 작업을 AuthService에서 트랜잭션으로 처리
        UserResponse response = authService.registerWithTeacher(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "회원가입 성공"));
    }

    @PostMapping("/refresh")
    @Operation(
            summary = "토큰 재발급",
            description = "Refresh Token을 사용하여 새로운 Access Token을 발급받습니다. " +
                    "보안을 위해 Refresh Token도 함께 재발급됩니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "토큰 재발급 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "유효하지 않은 Refresh Token"
            )
    })
    @LoggingTrace
    public ResponseEntity<ApiResponse<LoginResponse>> refresh(
            @Valid @RequestBody TokenRefreshRequest request) {

        log.debug("리프레시 토큰 발급 요청");

        LoginResponse response = authService.refresh(request.getRefreshToken());

        return ResponseEntity.ok(
                ApiResponse.success(response, "토큰 재발급 성공")
        );
    }

    @PostMapping("/refresh-token")
    @Operation(
            summary = "토큰 재발급 (헤더 방식)",
            description = "Authorization 헤더의 Refresh Token으로 새로운 토큰을 발급받습니다."
    )
    @LoggingTrace
    public ResponseEntity<ApiResponse<LoginResponse>> refreshWithHeader(
            @Parameter(description = "Bearer {refreshToken}", required = true)
            @RequestHeader("Authorization") String authHeader) {

        // Bearer 토큰 추출
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error("AUTH_003", "Invalid authorization header"));
        }

        String refreshToken = authHeader.substring(7);  // "Bearer " 제거

        LoginResponse response = authService.refresh(refreshToken);

        return ResponseEntity.ok(
                ApiResponse.success(response, "토큰 재발급 성공")
        );
    }

    @GetMapping("/validate")
    @Operation(
            summary = "토큰 검증",
            description = "JWT 토큰의 유효성을 검증하고 토큰 정보를 반환합니다."
    )
    @LoggingTrace
    public ResponseEntity<ApiResponse<TokenValidationResponse>> validateToken(
            @Parameter(description = "Bearer {token}", required = true)
            @RequestHeader("Authorization") String authHeader) {

        // Bearer 토큰 추출
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            TokenValidationResponse response = TokenValidationResponse.builder()
                    .valid(false)
                    .error("Invalid authorization header")
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        }

        String token = authHeader.substring(7);

        try {
            // 토큰 검증 (서명/만료)
            boolean isValid = jwtService.validateToken(token);

            if (isValid) {
                // 토큰 정보 추출
                Long userId = jwtService.getUserIdFromToken(token);
                Instant expiresAt = jwtService.getExpirationFromToken(token);
                boolean isRefreshToken = jwtService.isRefreshToken(token);
                // 세션 일치 여부 확인 (sessionId가 있는 경우)
                String sessionId = jwtService.getSessionIdFromToken(token);
                if (sessionId != null) {
                    boolean sessionOk = activeSessionService.isActiveSession(userId, sessionId);
                    if (!sessionOk) {
                        TokenValidationResponse response = TokenValidationResponse.builder()
                                .valid(false)
                                .error("Session invalid or taken over")
                                .build();
                        return ResponseEntity.ok(ApiResponse.success(response));
                    }
                }

                // 남은 시간 계산
                long remainingTime = expiresAt.getEpochSecond() - Instant.now().getEpochSecond();

                TokenValidationResponse response = TokenValidationResponse.builder()
                        .valid(true)
                        .tokenType(isRefreshToken ? "refresh" : "access")
                        .subject(userId.toString())
                        .expiresAt(expiresAt)
                        .remainingTime(remainingTime)
                        .build();

                return ResponseEntity.ok(ApiResponse.success(response));
            } else {
                TokenValidationResponse response = TokenValidationResponse.builder()
                        .valid(false)
                        .error("Token validation failed")
                        .build();

                return ResponseEntity.ok(ApiResponse.success(response));
            }
        } catch (Exception e) {
            log.error("Token validation error", e);

            TokenValidationResponse response = TokenValidationResponse.builder()
                    .valid(false)
                    .error(e.getMessage())
                    .build();

            return ResponseEntity.ok(ApiResponse.success(response));
        }
    }

    @PostMapping("/logout")
    @Operation(
            summary = "로그아웃",
            description = "로그아웃 처리. 현재는 클라이언트에서 토큰 삭제로 처리합니다."
    )
    @LoggingTrace
    public ResponseEntity<ApiResponse<Void>> logout(
            @RequestHeader(value = "Authorization", required = false) String authHeader) {

        try {
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                String token = authHeader.substring(7);
                Long userId = jwtService.getUserIdFromToken(token);
                String sessionId = jwtService.getSessionIdFromToken(token);
                activeSessionService.clearIfMatch(userId, sessionId);
            }
        } catch (Exception e) {
            // 로깅만 하고 응답은 성공 처리 (idempotent)
            log.warn("Logout processing error (ignored): {}", e.getMessage());
        }

        return ResponseEntity.ok(ApiResponse.successWithoutData("로그아웃 성공"));
    }
    
    @GetMapping("/oauth2/authorization/{provider}")
    @Operation(
            summary = "OAuth2 로그인 URL 생성",
            description = "OAuth2 제공자(네이버, 카카오 등)의 로그인 URL을 반환합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "OAuth2 로그인 URL 생성 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "400",
                    description = "지원하지 않는 OAuth2 제공자"
            )
    })
    @LoggingTrace
    public ResponseEntity<ApiResponse<String>> getOAuth2AuthorizationUrl(
            @PathVariable String provider) {
        
        log.info("OAuth2 로그인 URL 요청: provider={}", provider);
        
        // OAuth2 로그인 URL 생성 로직
        String authorizationUrl = authService.generateOAuth2AuthorizationUrl(provider);
        
        return ResponseEntity.ok(
                ApiResponse.success(authorizationUrl, "OAuth2 로그인 URL 생성 성공")
        );
    }
    
    // Legacy direct-callback endpoint retained for debugging; path changed to avoid conflict
    @GetMapping("/oauth2/callback-direct/{provider}")
    @Operation(
            summary = "OAuth2 콜백 처리",
            description = "OAuth2 제공자로부터 콜백을 받아 처리합니다."
    )
    @LoggingTrace
    public ResponseEntity<ApiResponse<LoginResponse>> handleOAuth2Callback(
            @PathVariable String provider,
            @RequestParam String code,
            @RequestParam(required = false) String state) {
        
        log.info("OAuth2 콜백 처리: provider={}, code={}, state={}", provider, code, state);
        
        // OAuth2 인증 처리 및 JWT 토큰 발급
        LoginResponse response = authService.processOAuth2Callback(provider, code, state);
        
        return ResponseEntity.ok(
                ApiResponse.success(response, "OAuth2 로그인 성공")
        );
    }
}
