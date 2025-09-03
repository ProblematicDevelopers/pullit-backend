package com.pullit.auth.service;

import com.pullit.auth.config.JwtProperties;
import com.pullit.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Enhanced JWT Service with blacklist support
 * This service extends the original JwtService with additional security features
 */
@Slf4j
@Service
@Primary  // This will be used instead of the original JwtService
@RequiredArgsConstructor
public class EnhancedJwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;
    private final JwtBlacklistService jwtBlacklistService;

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpiration());
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("jti", jti)  // JWT ID for tracking
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .build();
        
        // Track token JTI
        jwtBlacklistService.trackTokenJti(
            user.getId(), 
            jti, 
            "access", 
            jwtProperties.getAccessTokenExpiration()
        );

        JwsHeader jwsHeader = JwsHeader.with(()->"RS256").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,claims)).getTokenValue();
    }

    public String generateRefreshToken(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpiration());
        String jti = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();  // New family for initial token

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("jti", jti)  // JWT ID for tracking
                .claim("type","refresh")
                .claim("familyId", familyId)  // Family ID for rotation tracking
                .build();
        
        // Track token JTI and add to family
        jwtBlacklistService.trackTokenJti(
            user.getId(), 
            jti, 
            "refresh", 
            jwtProperties.getRefreshTokenExpiration()
        );
        jwtBlacklistService.addToRefreshTokenFamily(
            familyId, 
            jti, 
            jwtProperties.getRefreshTokenExpiration()
        );

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public String generateAccessToken(User user, String sessionId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpiration());
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("jti", jti)  // JWT ID for tracking
                .claim("username", user.getUsername())
                .claim("email", user.getEmail())
                .claim("fullName", user.getFullName())
                .claim("role", user.getRole().name())
                .claim("sessionId", sessionId)
                .build();
        
        // Track token JTI
        jwtBlacklistService.trackTokenJti(
            user.getId(), 
            jti, 
            "access", 
            jwtProperties.getAccessTokenExpiration()
        );

        JwsHeader jwsHeader = JwsHeader.with(()->"RS256").build();
        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,claims)).getTokenValue();
    }

    public String generateRefreshToken(User user, String sessionId) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpiration());
        String jti = UUID.randomUUID().toString();
        String familyId = UUID.randomUUID().toString();  // New family for initial token

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("jti", jti)  // JWT ID for tracking
                .claim("type","refresh")
                .claim("sessionId", sessionId)
                .claim("familyId", familyId)  // Family ID for rotation tracking
                .build();
        
        // Track token JTI and add to family
        jwtBlacklistService.trackTokenJti(
            user.getId(), 
            jti, 
            "refresh", 
            jwtProperties.getRefreshTokenExpiration()
        );
        jwtBlacklistService.addToRefreshTokenFamily(
            familyId, 
            jti, 
            jwtProperties.getRefreshTokenExpiration()
        );

        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    /**
     * Generate new refresh token from old one (rotation)
     * Maintains the family ID but creates new JTI
     */
    public String rotateRefreshToken(String oldRefreshToken) {
        try {
            Jwt oldJwt = jwtDecoder.decode(oldRefreshToken);
            Long userId = Long.parseLong(oldJwt.getSubject());
            String familyId = oldJwt.getClaim("familyId");
            String oldJti = oldJwt.getClaim("jti");
            
            // Blacklist the old token
            jwtBlacklistService.blacklistToken(oldRefreshToken, "Token rotation");
            
            // Generate new refresh token with same family
            Instant now = Instant.now();
            Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpiration());
            String newJti = UUID.randomUUID().toString();
            
            JwtClaimsSet claims = JwtClaimsSet.builder()
                    .issuer(jwtProperties.getIssuer())
                    .issuedAt(now)
                    .expiresAt(expiresAt)
                    .subject(userId.toString())
                    .claim("jti", newJti)
                    .claim("type", "refresh")
                    .claim("familyId", familyId)  // Keep same family
                    .claim("sessionId", oldJwt.getClaim("sessionId"))
                    .build();
            
            // Track new token in same family
            jwtBlacklistService.trackTokenJti(userId, newJti, "refresh", 
                jwtProperties.getRefreshTokenExpiration());
            jwtBlacklistService.addToRefreshTokenFamily(familyId, newJti, 
                jwtProperties.getRefreshTokenExpiration());
            
            return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
            
        } catch (Exception e) {
            log.error("Failed to rotate refresh token", e);
            throw new JwtException("Token rotation failed");
        }
    }

    public boolean validateToken(String token) {
        try {
            // Check blacklist first for quick rejection
            if (jwtBlacklistService.isBlacklisted(token)) {
                log.warn("Attempted use of blacklisted token");
                return false;
            }
            
            Jwt jwt = jwtDecoder.decode(token);
            return true;
        } catch(JwtValidationException e) {
            log.error("토큰 검증 실패 {}", e.getMessage());
            return false;
        } catch(JwtException e) {
            log.error("토큰 Decode 실패 {}", e.getMessage());
            return false;
        }
    }

    /**
     * Get JTI from token
     */
    public String getJtiFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaim("jti");
        } catch (JwtException e) {
            log.error("Failed to get JTI from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Get family ID from refresh token
     */
    public String getFamilyIdFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaim("familyId");
        } catch (JwtException e) {
            log.error("Failed to get family ID from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Blacklist a token explicitly
     */
    public void blacklistToken(String token, String reason) {
        jwtBlacklistService.blacklistToken(token, reason);
    }

    /**
     * Blacklist all user tokens
     */
    public void blacklistAllUserTokens(Long userId, String reason) {
        jwtBlacklistService.blacklistAllUserTokens(userId, reason);
    }
    
    // Methods from original JwtService that are still needed
    
    public Long getUserIdFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return Long.parseLong(jwt.getSubject());
        } catch(JwtException e) {
            log.error("token에서 id추출 실패 : {}", e.getMessage());
            throw e;
        }
    }
    
    public boolean isRefreshToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String type = jwt.getClaim("type");
            return "refresh".equals(type);
        } catch (JwtException e) {
            log.error("Failed to check token type: {}", e.getMessage());
            return false;
        }
    }
    
    public String getUsernameFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaim("username");
        } catch (JwtException e) {
            log.error("Failed to get username from token: {}", e.getMessage());
            throw e;
        }
    }
    
    public String getRoleFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaim("role");
        } catch (JwtException e) {
            log.error("Failed to get role from token: {}", e.getMessage());
            throw e;
        }
    }
    
    public Instant getExpirationFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getExpiresAt();
        } catch (JwtException e) {
            log.error("Failed to get expiration from token: {}", e.getMessage());
            throw e;
        }
    }
    
    public String getSessionIdFromToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            return jwt.getClaim("sessionId");
        } catch (JwtException e) {
            log.error("Failed to get sessionId from token: {}", e.getMessage());
            return null;
        }
    }
}