package com.pullit.auth.controller;

import com.pullit.auth.dto.request.TokenRevokeRequest;
import com.pullit.auth.dto.response.TokenRevokeResponse;
import com.pullit.auth.service.EnhancedJwtService;
import com.pullit.auth.service.JwtBlacklistService;
import com.pullit.common.dto.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Enhanced authentication controller with token blacklist support
 * This controller extends the authentication functionality with additional security features
 */
@Slf4j
@RestController
@RequestMapping("/api/auth/v2")
@RequiredArgsConstructor
@Tag(name = "Enhanced Authentication", description = "향상된 인증 관련 API")
public class EnhancedAuthController {

    private final EnhancedJwtService enhancedJwtService;
    private final JwtBlacklistService jwtBlacklistService;
    private final com.pullit.auth.service.ActiveSessionService activeSessionService;

    @PostMapping("/logout")
    @Operation(
        summary = "강화된 로그아웃",
        description = "액세스 토큰과 리프레시 토큰을 모두 블랙리스트에 추가하고 세션을 종료합니다."
    )
    public ResponseEntity<ApiResponse<Void>> enhancedLogout(
        @RequestHeader(value = "Authorization", required = false) String authHeader,
        @RequestBody(required = false) LogoutRequest request
    ) {
        try {
            String accessToken = null;
            String refreshToken = null;
            
            // Extract access token from header
            if (authHeader != null && authHeader.startsWith("Bearer ")) {
                accessToken = authHeader.substring(7);
            }
            
            // Get refresh token from request body if provided
            if (request != null && request.getRefreshToken() != null) {
                refreshToken = request.getRefreshToken();
            }
            
            // Blacklist access token
            if (accessToken != null) {
                enhancedJwtService.blacklistToken(accessToken, "User logout");
                
                // Clear session if exists
                try {
                    Long userId = enhancedJwtService.getUserIdFromToken(accessToken);
                    String sessionId = enhancedJwtService.getSessionIdFromToken(accessToken);
                    activeSessionService.clearIfMatch(userId, sessionId);
                } catch (Exception e) {
                    log.debug("Session clear failed during logout: {}", e.getMessage());
                }
            }
            
            // Blacklist refresh token and its family
            if (refreshToken != null) {
                enhancedJwtService.blacklistToken(refreshToken, "User logout");
                
                // Blacklist entire refresh token family to prevent rotation abuse
                String familyId = enhancedJwtService.getFamilyIdFromToken(refreshToken);
                if (familyId != null) {
                    jwtBlacklistService.blacklistRefreshTokenFamily(familyId, "Logout - family invalidation");
                }
            }
            
            log.info("User successfully logged out with token blacklisting");
            return ResponseEntity.ok(ApiResponse.successWithoutData("로그아웃 성공"));
            
        } catch (Exception e) {
            log.error("Logout error", e);
            // Still return success for idempotent behavior
            return ResponseEntity.ok(ApiResponse.successWithoutData("로그아웃 처리"));
        }
    }

    @PostMapping("/revoke-token")
    @Operation(
        summary = "토큰 강제 무효화",
        description = "특정 토큰을 강제로 블랙리스트에 추가합니다. 보안 사고 시 사용됩니다."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TokenRevokeResponse>> revokeToken(
        @Valid @RequestBody TokenRevokeRequest request,
        @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader
    ) {
        try {
            // Extract current user's token for authorization check
            String currentToken = authHeader.substring(7);
            Long currentUserId = enhancedJwtService.getUserIdFromToken(currentToken);
            
            // Revoke the specified token
            String tokenToRevoke = request.getToken();
            String reason = request.getReason() != null ? request.getReason() : "Manual revocation";
            
            // Check if user is revoking their own token or has admin rights
            Long targetUserId = enhancedJwtService.getUserIdFromToken(tokenToRevoke);
            if (!currentUserId.equals(targetUserId)) {
                // Would need admin check here
                log.warn("User {} attempted to revoke token of user {}", currentUserId, targetUserId);
                return ResponseEntity.badRequest()
                    .body(ApiResponse.error("UNAUTHORIZED", "Cannot revoke other user's token"));
            }
            
            // Blacklist the token
            enhancedJwtService.blacklistToken(tokenToRevoke, reason);
            
            // If it's a refresh token, blacklist the family
            if (enhancedJwtService.isRefreshToken(tokenToRevoke)) {
                String familyId = enhancedJwtService.getFamilyIdFromToken(tokenToRevoke);
                if (familyId != null) {
                    jwtBlacklistService.blacklistRefreshTokenFamily(familyId, reason + " - family invalidation");
                }
            }
            
            TokenRevokeResponse response = TokenRevokeResponse.builder()
                .success(true)
                .message("Token successfully revoked")
                .revokedAt(java.time.Instant.now())
                .build();
            
            log.info("Token revoked by user {}: reason={}", currentUserId, reason);
            return ResponseEntity.ok(ApiResponse.success(response, "토큰 무효화 성공"));
            
        } catch (Exception e) {
            log.error("Token revocation error", e);
            
            TokenRevokeResponse response = TokenRevokeResponse.builder()
                .success(false)
                .message("Failed to revoke token: " + e.getMessage())
                .build();
            
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("REVOKE_FAILED", "토큰 무효화 실패"));
        }
    }

    @PostMapping("/revoke-all-tokens")
    @Operation(
        summary = "모든 토큰 무효화",
        description = "사용자의 모든 토큰을 무효화합니다. 계정 탈취 의심 시 사용됩니다."
    )
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<TokenRevokeResponse>> revokeAllTokens(
        @Parameter(hidden = true) @RequestHeader("Authorization") String authHeader,
        @RequestBody(required = false) RevokeAllRequest request
    ) {
        try {
            // Extract current user
            String currentToken = authHeader.substring(7);
            Long userId = enhancedJwtService.getUserIdFromToken(currentToken);
            
            String reason = request != null && request.getReason() != null 
                ? request.getReason() 
                : "User requested all tokens revocation";
            
            // Blacklist all user's tokens
            jwtBlacklistService.blacklistAllUserTokens(userId, reason);
            
            // Clear all sessions
            activeSessionService.clearActiveSession(userId);
            
            TokenRevokeResponse response = TokenRevokeResponse.builder()
                .success(true)
                .message("All tokens successfully revoked. Please login again.")
                .revokedAt(java.time.Instant.now())
                .build();
            
            log.warn("All tokens revoked for user {}: reason={}", userId, reason);
            return ResponseEntity.ok(ApiResponse.success(response, "모든 토큰 무효화 성공"));
            
        } catch (Exception e) {
            log.error("All tokens revocation error", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("REVOKE_ALL_FAILED", "모든 토큰 무효화 실패"));
        }
    }

    @GetMapping("/blacklist-stats")
    @Operation(
        summary = "블랙리스트 통계",
        description = "토큰 블랙리스트 통계를 조회합니다."
    )
    // Admin gating removed per requirements; authenticated by default security chain
    public ResponseEntity<ApiResponse<JwtBlacklistService.BlacklistStats>> getBlacklistStats() {
        try {
            JwtBlacklistService.BlacklistStats stats = jwtBlacklistService.getBlacklistStats();
            return ResponseEntity.ok(ApiResponse.success(stats, "블랙리스트 통계 조회 성공"));
        } catch (Exception e) {
            log.error("Failed to get blacklist stats", e);
            return ResponseEntity.internalServerError()
                .body(ApiResponse.error("STATS_FAILED", "통계 조회 실패"));
        }
    }

    /**
     * Logout request DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class LogoutRequest {
        private String refreshToken;
    }

    /**
     * Revoke all tokens request DTO
     */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class RevokeAllRequest {
        private String reason;
    }
}
