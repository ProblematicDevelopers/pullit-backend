package com.pullit.auth.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;

import java.time.Instant;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * JWT 블랙리스트 서비스 테스트
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("JWT Blacklist Service Tests")
class JwtBlacklistServiceTest {

    @Autowired
    private JwtBlacklistService jwtBlacklistService;

    @MockBean
    private RedisTemplate<String, String> redisTemplate;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ValueOperations<String, String> valueOperations;

    @MockBean
    private SetOperations<String, String> setOperations;

    private static final String TEST_TOKEN = "test.jwt.token";
    private static final String TEST_JTI = "test-jti-123";
    private static final Long TEST_USER_ID = 1L;
    private static final String TEST_FAMILY_ID = "test-family-123";

    @BeforeEach
    void setUp() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    @Test
    @DisplayName("토큰을 블랙리스트에 추가할 수 있다")
    void testBlacklistToken() {
        // Given
        Instant expiresAt = Instant.now().plusSeconds(3600);
        Jwt jwt = mock(Jwt.class);
        
        when(jwtDecoder.decode(TEST_TOKEN)).thenReturn(jwt);
        when(jwt.getClaim("jti")).thenReturn(TEST_JTI);
        when(jwt.getExpiresAt()).thenReturn(expiresAt);
        when(jwt.getSubject()).thenReturn(TEST_USER_ID.toString());
        
        // When
        jwtBlacklistService.blacklistToken(TEST_TOKEN, "Test reason");
        
        // Then
        verify(valueOperations).set(
            eq("auth:blacklist:token:" + TEST_JTI),
            anyString(),
            anyLong(),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("만료된 토큰은 블랙리스트에 추가되지 않는다")
    void testBlacklistExpiredToken() {
        // Given
        Instant expiresAt = Instant.now().minusSeconds(3600); // Already expired
        Jwt jwt = mock(Jwt.class);
        
        when(jwtDecoder.decode(TEST_TOKEN)).thenReturn(jwt);
        when(jwt.getClaim("jti")).thenReturn(TEST_JTI);
        when(jwt.getExpiresAt()).thenReturn(expiresAt);
        
        // When
        jwtBlacklistService.blacklistToken(TEST_TOKEN, "Test reason");
        
        // Then
        verify(valueOperations, never()).set(anyString(), anyString(), anyLong(), any(TimeUnit.class));
    }

    @Test
    @DisplayName("토큰이 블랙리스트에 있는지 확인할 수 있다")
    void testIsBlacklisted() {
        // Given
        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode(TEST_TOKEN)).thenReturn(jwt);
        when(jwt.getClaim("jti")).thenReturn(TEST_JTI);
        when(redisTemplate.hasKey("auth:blacklist:token:" + TEST_JTI)).thenReturn(true);
        
        // When
        boolean isBlacklisted = jwtBlacklistService.isBlacklisted(TEST_TOKEN);
        
        // Then
        assertTrue(isBlacklisted);
        verify(redisTemplate).hasKey("auth:blacklist:token:" + TEST_JTI);
    }

    @Test
    @DisplayName("블랙리스트에 없는 토큰은 false를 반환한다")
    void testIsNotBlacklisted() {
        // Given
        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode(TEST_TOKEN)).thenReturn(jwt);
        when(jwt.getClaim("jti")).thenReturn(TEST_JTI);
        when(redisTemplate.hasKey("auth:blacklist:token:" + TEST_JTI)).thenReturn(false);
        
        // When
        boolean isBlacklisted = jwtBlacklistService.isBlacklisted(TEST_TOKEN);
        
        // Then
        assertFalse(isBlacklisted);
    }

    @Test
    @DisplayName("리프레시 토큰 패밀리에 토큰을 추가할 수 있다")
    void testAddToRefreshTokenFamily() {
        // Given
        long ttlSeconds = 7200L;
        
        // When
        jwtBlacklistService.addToRefreshTokenFamily(TEST_FAMILY_ID, TEST_JTI, ttlSeconds);
        
        // Then
        verify(setOperations).add("auth:refresh:family:" + TEST_FAMILY_ID, TEST_JTI);
        verify(redisTemplate).expire("auth:refresh:family:" + TEST_FAMILY_ID, ttlSeconds, TimeUnit.SECONDS);
    }

    @Test
    @DisplayName("리프레시 토큰 패밀리 전체를 블랙리스트 처리할 수 있다")
    void testBlacklistRefreshTokenFamily() {
        // Given
        String familyKey = "auth:refresh:family:" + TEST_FAMILY_ID;
        Set<String> familyTokens = Set.of("jti-1", "jti-2", "jti-3");
        
        when(setOperations.members(familyKey)).thenReturn(familyTokens);
        
        // When
        jwtBlacklistService.blacklistRefreshTokenFamily(TEST_FAMILY_ID, "Family compromise");
        
        // Then
        for (String jti : familyTokens) {
            verify(valueOperations).set(
                eq("auth:blacklist:token:" + jti),
                anyString(),
                eq(7L),
                eq(TimeUnit.DAYS)
            );
        }
        verify(redisTemplate).delete(familyKey);
    }

    @Test
    @DisplayName("사용자의 모든 토큰을 블랙리스트 처리할 수 있다")
    void testBlacklistAllUserTokens() {
        // Given
        String pattern = "auth:token:jti:" + TEST_USER_ID + ":*";
        Set<String> userTokenKeys = Set.of(
            "auth:token:jti:1:jti-1",
            "auth:token:jti:1:jti-2"
        );
        
        when(redisTemplate.keys(pattern)).thenReturn(userTokenKeys);
        
        // When
        jwtBlacklistService.blacklistAllUserTokens(TEST_USER_ID, "Account compromise");
        
        // Then
        verify(valueOperations, times(2)).set(
            anyString(),
            anyString(),
            eq(24L),
            eq(TimeUnit.HOURS)
        );
        verify(redisTemplate).delete(userTokenKeys);
    }

    @Test
    @DisplayName("토큰 JTI를 추적할 수 있다")
    void testTrackTokenJti() {
        // Given
        String tokenType = "access";
        long ttlSeconds = 3600L;
        
        // When
        jwtBlacklistService.trackTokenJti(TEST_USER_ID, TEST_JTI, tokenType, ttlSeconds);
        
        // Then
        String expectedKey = "auth:token:jti:" + TEST_USER_ID + ":" + TEST_JTI;
        verify(valueOperations).set(
            eq(expectedKey),
            anyString(),
            eq(ttlSeconds),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("블랙리스트 통계를 조회할 수 있다")
    void testGetBlacklistStats() {
        // Given
        when(redisTemplate.keys("auth:blacklist:token:*")).thenReturn(Set.of("key1", "key2"));
        when(redisTemplate.keys("auth:refresh:family:*")).thenReturn(Set.of("family1"));
        when(redisTemplate.keys("auth:token:jti:*")).thenReturn(Set.of("jti1", "jti2", "jti3"));
        
        // When
        JwtBlacklistService.BlacklistStats stats = jwtBlacklistService.getBlacklistStats();
        
        // Then
        assertEquals(2, stats.getBlacklistedTokenCount());
        assertEquals(1, stats.getRefreshFamilyCount());
        assertEquals(3, stats.getTrackedTokenCount());
    }

    @Test
    @DisplayName("JTI가 없는 토큰도 처리할 수 있다")
    void testHandleTokenWithoutJti() {
        // Given
        Jwt jwt = mock(Jwt.class);
        when(jwtDecoder.decode(TEST_TOKEN)).thenReturn(jwt);
        when(jwt.getClaim("jti")).thenReturn(null);
        when(jwt.getSubject()).thenReturn(TEST_USER_ID.toString());
        when(jwt.getIssuedAt()).thenReturn(Instant.now());
        when(jwt.getExpiresAt()).thenReturn(Instant.now().plusSeconds(3600));
        
        // When
        jwtBlacklistService.blacklistToken(TEST_TOKEN, "No JTI token");
        
        // Then
        verify(valueOperations).set(
            anyString(),  // Generated JTI
            anyString(),
            anyLong(),
            eq(TimeUnit.SECONDS)
        );
    }

    @Test
    @DisplayName("에러 발생 시 블랙리스트 체크는 true를 반환한다 (fail closed)")
    void testIsBlacklistedOnError() {
        // Given
        when(jwtDecoder.decode(TEST_TOKEN)).thenThrow(new RuntimeException("Decode error"));
        
        // When
        boolean isBlacklisted = jwtBlacklistService.isBlacklisted(TEST_TOKEN);
        
        // Then
        assertTrue(isBlacklisted); // Fail closed for security
    }
}