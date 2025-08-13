package com.pullit.auth.controller;

import com.pullit.auth.dto.request.LoginRequest;
import com.pullit.auth.dto.request.TokenRefreshRequest;
import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.auth.dto.response.TokenValidationResponse;
import com.pullit.auth.service.AuthService;
import com.pullit.auth.service.JwtService;
import com.pullit.common.annotation.LoggingTrace;
import com.pullit.common.annotation.RateLimited;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.user.dto.request.UserCreateRequest;
import com.pullit.user.dto.response.UserResponse;
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
    private final JwtService jwtService;

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
    @RateLimited(limit = 3, duration = 1, timeUnit = TimeUnit.MINUTES)
    public ResponseEntity<ApiResponse<UserResponse>> register(
            @Valid @RequestBody UserCreateRequest request) {

        log.info("회원가입 요청: {}", request.getUsername());

        UserResponse response = authService.register(request);

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
            // 토큰 검증
            boolean isValid = jwtService.validateToken(token);

            if (isValid) {
                // 토큰 정보 추출
                Long userId = jwtService.getUserIdFromToken(token);
                Instant expiresAt = jwtService.getExpirationFromToken(token);
                boolean isRefreshToken = jwtService.isRefreshToken(token);

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
    public ResponseEntity<ApiResponse<Void>> logout() {

        // TODO: 서버 측 로그아웃 처리 (블랙리스트 등)
        // 현재는 클라이언트에서 토큰 삭제로 처리

        return ResponseEntity.ok(
                ApiResponse.successWithoutData("로그아웃 성공")
        );
    }
}
