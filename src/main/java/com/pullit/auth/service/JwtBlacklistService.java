package com.pullit.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * JWT 블랙리스트 관리 서비스
 * Redis를 사용하여 무효화된 토큰을 관리하고, 토큰 검증 시 블랙리스트 체크를 수행합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JwtBlacklistService {

    private final RedisTemplate<String, String> redisTemplate;
    private final JwtDecoder jwtDecoder;

    // Redis key prefixes
    private static final String BLACKLIST_PREFIX = "auth:blacklist:token:";
    private static final String REFRESH_FAMILY_PREFIX = "auth:refresh:family:";
    private static final String TOKEN_JTI_PREFIX = "auth:token:jti:";
    
    // Token types
    private static final String TOKEN_TYPE_ACCESS = "access";
    private static final String TOKEN_TYPE_REFRESH = "refresh";
    
    /**
     * 토큰을 블랙리스트에 추가
     * 토큰의 남은 유효시간만큼 Redis에 저장하여 자동 만료되도록 설정
     */
    public void blacklistToken(String token, String reason) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String jti = getJtiFromJwt(jwt);
            Instant expiresAt = jwt.getExpiresAt();
            
            if (expiresAt == null) {
                log.warn("Token has no expiration, skipping blacklist: jti={}", jti);
                return;
            }
            
            // Calculate remaining TTL
            long ttlSeconds = Duration.between(Instant.now(), expiresAt).getSeconds();
            
            if (ttlSeconds <= 0) {
                log.debug("Token already expired, skipping blacklist: jti={}", jti);
                return;
            }
            
            // Store in blacklist with TTL
            String blacklistKey = BLACKLIST_PREFIX + jti;
            String blacklistValue = String.format("%s|%s|%s", 
                jwt.getSubject(), 
                reason, 
                Instant.now().toString());
            
            redisTemplate.opsForValue().set(blacklistKey, blacklistValue, ttlSeconds, TimeUnit.SECONDS);
            log.info("Token blacklisted: jti={}, userId={}, reason={}, ttl={}s", 
                jti, jwt.getSubject(), reason, ttlSeconds);
                
        } catch (Exception e) {
            log.error("Failed to blacklist token", e);
        }
    }
    
    /**
     * 토큰이 블랙리스트에 있는지 확인
     */
    public boolean isBlacklisted(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);
            String jti = getJtiFromJwt(jwt);
            
            String blacklistKey = BLACKLIST_PREFIX + jti;
            Boolean exists = redisTemplate.hasKey(blacklistKey);
            
            if (Boolean.TRUE.equals(exists)) {
                log.debug("Token is blacklisted: jti={}", jti);
                return true;
            }
            
            return false;
        } catch (Exception e) {
            log.error("Failed to check blacklist status", e);
            // 에러 시 안전하게 블랙리스트로 처리
            return true;
        }
    }
    
    /**
     * JWT에서 JTI(JWT ID) 추출
     * JTI가 없으면 토큰 해시값으로 대체
     */
    private String getJtiFromJwt(Jwt jwt) {
        // Check for jti claim
        String jti = jwt.getClaim("jti");
        if (jti != null) {
            return jti;
        }
        
        // Fallback: generate ID from token content
        String subject = jwt.getSubject();
        Instant issuedAt = jwt.getIssuedAt();
        
        if (subject != null && issuedAt != null) {
            return UUID.nameUUIDFromBytes(
                (subject + issuedAt.toString()).getBytes()
            ).toString();
        }
        
        // Last resort: use random UUID
        return UUID.randomUUID().toString();
    }
    
    /**
     * 리프레시 토큰 패밀리 추가
     * 토큰 rotation 시 이전 토큰들을 추적하기 위함
     */
    public void addToRefreshTokenFamily(String familyId, String tokenJti, long ttlSeconds) {
        try {
            String familyKey = REFRESH_FAMILY_PREFIX + familyId;
            
            // Add token to family set
            redisTemplate.opsForSet().add(familyKey, tokenJti);
            
            // Set expiration for the family
            redisTemplate.expire(familyKey, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("Added token to refresh family: familyId={}, jti={}", familyId, tokenJti);
        } catch (Exception e) {
            log.error("Failed to add token to refresh family", e);
        }
    }
    
    /**
     * 리프레시 토큰 패밀리 전체 블랙리스트 처리
     * 토큰 탈취 의심 시 전체 패밀리 무효화
     */
    public void blacklistRefreshTokenFamily(String familyId, String reason) {
        try {
            String familyKey = REFRESH_FAMILY_PREFIX + familyId;
            Set<String> familyTokens = redisTemplate.opsForSet().members(familyKey);
            
            if (familyTokens == null || familyTokens.isEmpty()) {
                log.debug("No tokens found in refresh family: {}", familyId);
                return;
            }
            
            // Blacklist all tokens in the family
            for (String tokenJti : familyTokens) {
                String blacklistKey = BLACKLIST_PREFIX + tokenJti;
                String blacklistValue = String.format("family:%s|%s|%s", 
                    familyId, 
                    reason, 
                    Instant.now().toString());
                
                // Set with 7 days TTL for family blacklist
                redisTemplate.opsForValue().set(blacklistKey, blacklistValue, 7, TimeUnit.DAYS);
            }
            
            // Delete the family
            redisTemplate.delete(familyKey);
            
            log.warn("Blacklisted entire refresh token family: familyId={}, count={}, reason={}", 
                familyId, familyTokens.size(), reason);
                
        } catch (Exception e) {
            log.error("Failed to blacklist refresh token family", e);
        }
    }
    
    /**
     * 사용자의 모든 토큰 블랙리스트 처리
     * 계정 탈취, 비밀번호 변경 등의 경우 사용
     */
    public void blacklistAllUserTokens(Long userId, String reason) {
        try {
            // Pattern to find all user's tokens
            String pattern = TOKEN_JTI_PREFIX + userId + ":*";
            Set<String> userTokenKeys = redisTemplate.keys(pattern);
            
            if (userTokenKeys == null || userTokenKeys.isEmpty()) {
                log.debug("No active tokens found for user: {}", userId);
                return;
            }
            
            // Blacklist each token
            for (String tokenKey : userTokenKeys) {
                String jti = tokenKey.substring((TOKEN_JTI_PREFIX + userId + ":").length());
                String blacklistKey = BLACKLIST_PREFIX + jti;
                String blacklistValue = String.format("user:%d|%s|%s", 
                    userId, 
                    reason, 
                    Instant.now().toString());
                
                // Set with 24 hours TTL for user-wide blacklist
                redisTemplate.opsForValue().set(blacklistKey, blacklistValue, 24, TimeUnit.HOURS);
            }
            
            // Delete user's token tracking keys
            redisTemplate.delete(userTokenKeys);
            
            log.warn("Blacklisted all tokens for user: userId={}, count={}, reason={}", 
                userId, userTokenKeys.size(), reason);
                
        } catch (Exception e) {
            log.error("Failed to blacklist all user tokens", e);
        }
    }
    
    /**
     * 토큰 JTI 추적 저장
     * 사용자별 활성 토큰 추적을 위함
     */
    public void trackTokenJti(Long userId, String jti, String tokenType, long ttlSeconds) {
        try {
            String trackingKey = TOKEN_JTI_PREFIX + userId + ":" + jti;
            String trackingValue = tokenType + "|" + Instant.now().toString();
            
            redisTemplate.opsForValue().set(trackingKey, trackingValue, ttlSeconds, TimeUnit.SECONDS);
            
            log.debug("Tracking token JTI: userId={}, jti={}, type={}", userId, jti, tokenType);
        } catch (Exception e) {
            log.error("Failed to track token JTI", e);
        }
    }
    
    /**
     * 만료된 블랙리스트 엔트리 정리
     * 주기적으로 실행하여 Redis 메모리 최적화
     */
    public void cleanupExpiredEntries() {
        try {
            // Redis TTL이 자동으로 처리하므로 추가 정리는 불필요
            // 이 메서드는 수동 정리가 필요한 경우를 위해 유지
            log.debug("Blacklist cleanup check completed");
        } catch (Exception e) {
            log.error("Failed to cleanup expired blacklist entries", e);
        }
    }
    
    /**
     * 블랙리스트 통계 조회
     */
    public BlacklistStats getBlacklistStats() {
        try {
            Set<String> blacklistKeys = redisTemplate.keys(BLACKLIST_PREFIX + "*");
            Set<String> familyKeys = redisTemplate.keys(REFRESH_FAMILY_PREFIX + "*");
            Set<String> jtiKeys = redisTemplate.keys(TOKEN_JTI_PREFIX + "*");
            
            return BlacklistStats.builder()
                .blacklistedTokenCount(blacklistKeys != null ? blacklistKeys.size() : 0)
                .refreshFamilyCount(familyKeys != null ? familyKeys.size() : 0)
                .trackedTokenCount(jtiKeys != null ? jtiKeys.size() : 0)
                .build();
        } catch (Exception e) {
            log.error("Failed to get blacklist stats", e);
            return BlacklistStats.builder().build();
        }
    }
    
    /**
     * 블랙리스트 통계 DTO
     */
    @lombok.Builder
    @lombok.Getter
    public static class BlacklistStats {
        private final int blacklistedTokenCount;
        private final int refreshFamilyCount;
        private final int trackedTokenCount;
    }
}