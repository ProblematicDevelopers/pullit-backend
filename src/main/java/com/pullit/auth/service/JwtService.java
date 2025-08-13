package com.pullit.auth.service;

import com.pullit.auth.config.JwtProperties;
import com.pullit.user.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.nio.file.attribute.AclEntryPermission;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;
    private final JwtProperties jwtProperties;

    public String generateAccessToken(User user){
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getAccessTokenExpiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("username", user.getUsername())  // 사용자명
                .claim("email", user.getEmail())  // 이메일
                .claim("fullName", user.getFullName())  // 전체 이름
                .claim("role", user.getRole().name())  // 권한 (ADMIN, TEACHER, STUDENT)
                .build();

        JwsHeader jwsHeader = JwsHeader.with(()->"RS256").build();

        return jwtEncoder.encode(JwtEncoderParameters.from(jwsHeader,claims)).getTokenValue();
    }

    public String generateRefreshToken(User user){
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(jwtProperties.getRefreshTokenExpiration());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.getIssuer())
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getId().toString())
                .claim("type","refresh")
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(claims)).getTokenValue();
    }

    public Long getUserIdFromToken(String token){
        try{
            Jwt jwt = jwtDecoder.decode(token);
            return Long.parseLong(jwt.getSubject());
        }catch(JwtException e){
            log.error("token에서 id추출 실패 : {}",e.getMessage());
            throw e;
        }
    }

    public boolean validateToken(String token){
        try{
            Jwt jwt = jwtDecoder.decode(token);
            return true;
        }catch(JwtValidationException e){
            log.error("토큰 검증 실패 {}",e.getMessage());
            return false;
        }catch(JwtException e){
            log.error("토큰 Decode 실패 {}",e.getMessage());
            return false;
        }
    }

    public boolean isRefreshToken(String token) {
        try {
            Jwt jwt = jwtDecoder.decode(token);

            // type 클레임 확인
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

}
