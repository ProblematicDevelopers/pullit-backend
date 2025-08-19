package com.pullit.auth.service;

import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.auth.exception.SocialLoginNewUserException;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.user.entity.User;
import com.pullit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuth2Service {

    private final UserService userService;
    private final JwtService jwtService;

    @Transactional
    public LoginResponse handleOAuth2LoginSuccess(Authentication authentication) {
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            String provider = getProviderFromAuthentication(authentication);
            String providerId = oauth2User.getName();
            
            log.info("OAuth2 login success - Provider: {}, ID: {}", provider, providerId);
            
            // 기존 사용자 확인
            User user = findExistingOAuth2User(oauth2User, provider, providerId);
            
            if (user != null) {
                // 기존 사용자: JWT 토큰 생성
                String accessToken = jwtService.generateAccessToken(user);
                String refreshToken = jwtService.generateRefreshToken(user);
                
                // 마지막 로그인 시간 업데이트
                userService.updateLastLogin(user.getId());
                
                return LoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .tokenType("Bearer")
                        .expiresIn(86400L)
                        .user(com.pullit.user.dto.response.UserResponse.from(user))
                        .build();
            } else {
                // 신규 사용자: 예외 발생
                throw new SocialLoginNewUserException("신규 사용자입니다. 회원가입을 진행해주세요.");
            }
        }
        
        throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
    }

    private String getProviderFromAuthentication(Authentication authentication) {
        // OAuth2 인증에서 제공자 정보 추출
        String authName = authentication.getName();
        if (authName.contains("google")) return "google";
        if (authName.contains("kakao")) return "kakao";
        if (authName.contains("naver")) return "naver";
        return "unknown";
    }

    private User findExistingOAuth2User(OAuth2User oauth2User, String provider, String providerId) {
        // 기존 OAuth2 사용자 확인 (이메일로 확인)
        String email = extractEmail(oauth2User.getAttributes(), provider);
        Optional<User> existingUser = email != null ? userService.findByEmail(email) : Optional.empty();
        
        if (existingUser.isPresent()) {
            log.info("Existing OAuth2 user found: {}", existingUser.get().getUsername());
            return existingUser.get();
        }
        
        // 신규 사용자
        log.info("New OAuth2 user - Provider: {}, ID: {}", provider, providerId);
        return null;
    }

    // 소셜 로그인 사용자 생성은 나중에 회원가입 플로우에서 처리
    // private User createOAuth2User(OAuth2User oauth2User, String provider, String providerId) {
    //     // 회원가입 플로우에서 처리하므로 여기서는 제거
    // }

    private String extractEmail(Map<String, Object> attributes, String provider) {
        switch (provider) {
            case "google":
                return (String) attributes.get("email");
            case "kakao":
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    return (String) profile.get("email");
                }
                break;
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                if (response != null) {
                    return (String) response.get("email");
                }
                break;
        }
        return null;
    }

    private String extractName(Map<String, Object> attributes, String provider) {
        switch (provider) {
            case "google":
                return (String) attributes.get("name");
            case "kakao":
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null) {
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    return (String) profile.get("nickname");
                }
                break;
            case "naver":
                Map<String, Object> response = (Map<String, Object>) attributes.get("response");
                if (response != null) {
                    return (String) response.get("name");
                }
                break;
        }
        return "Unknown User";
    }

    // 소셜 로그인 사용자명 생성은 나중에 회원가입 플로우에서 처리
    // private String generateUsername(String provider, String providerId) {
    //     return provider + "_" + providerId;
    // }

    // 소셜 로그인 비밀번호 생성은 나중에 회원가입 플로우에서 처리
    // private String generateRandomPassword() {
    //     return "OAUTH2_" + System.currentTimeMillis();
    // }
} 