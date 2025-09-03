package com.pullit.auth.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

/**
 * JWT 블랙리스트 검증기
 * 토큰이 블랙리스트에 있는지 확인하여 무효화된 토큰의 사용을 방지합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BlacklistJwtValidator implements OAuth2TokenValidator<Jwt> {
    
    private final RedisTemplate<String, String> redisTemplate;
    
    private static final String BLACKLIST_PREFIX = "auth:blacklist:token:";
    
    private static final OAuth2Error BLACKLISTED_TOKEN_ERROR = new OAuth2Error(
        "blacklisted_token",
        "This token has been revoked and is no longer valid",
        null
    );
    
    @Override
    public OAuth2TokenValidatorResult validate(Jwt token) {
        try {
            String jti = getJtiFromJwt(token);
            String blacklistKey = BLACKLIST_PREFIX + jti;
            
            // Check if token is blacklisted
            Boolean isBlacklisted = redisTemplate.hasKey(blacklistKey);
            
            if (Boolean.TRUE.equals(isBlacklisted)) {
                // Get blacklist reason for logging
                String blacklistInfo = redisTemplate.opsForValue().get(blacklistKey);
                log.warn("Attempted use of blacklisted token: jti={}, userId={}, info={}", 
                    jti, token.getSubject(), blacklistInfo);
                    
                return OAuth2TokenValidatorResult.failure(BLACKLISTED_TOKEN_ERROR);
            }
            
            return OAuth2TokenValidatorResult.success();
            
        } catch (Exception e) {
            log.error("Error during blacklist validation", e);
            // On error, fail closed (treat as blacklisted for security)
            return OAuth2TokenValidatorResult.failure(BLACKLISTED_TOKEN_ERROR);
        }
    }
    
    /**
     * JWT에서 JTI(JWT ID) 추출
     * JTI가 없으면 토큰의 고유 식별자를 생성
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
        
        // Last resort: use random UUID (should not happen in practice)
        return UUID.randomUUID().toString();
    }
}