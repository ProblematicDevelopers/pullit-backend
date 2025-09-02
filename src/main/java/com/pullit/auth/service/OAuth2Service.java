package com.pullit.auth.service;

import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.auth.dto.response.OAuth2LoginResult;
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
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class OAuth2Service {

    private final UserService userService;
    private final JwtService jwtService;
    private final ActiveSessionService activeSessionService;
    private final com.pullit.auth.config.JwtProperties jwtProperties;

    @Transactional
    public LoginResponse handleOAuth2LoginSuccess(Authentication authentication) {
        // 기존 메서드 (Spring Security OAuth2용)
        return handleOAuth2LoginSuccessWithAuthentication(authentication);
    }
    
    @Transactional
    public OAuth2LoginResult handleOAuth2LoginSuccess(Map<String, Object> socialInfo) {
        // 새로운 메서드 (커스텀 OAuth2 흐름용)
        return handleOAuth2LoginSuccessWithSocialInfo(socialInfo);
    }
    
    private LoginResponse handleOAuth2LoginSuccessWithAuthentication(Authentication authentication) {
        try {
            // 세션에서 소셜 정보 가져오기
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            HttpSession session = request.getSession();
            @SuppressWarnings("unchecked")
            Map<String, Object> sessionSocialInfo = (Map<String, Object>) session.getAttribute("oauth2_social_info");
            
            if (sessionSocialInfo == null) {
                log.error("OAuth2 session info not found");
                throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
            }
            
            String provider = (String) sessionSocialInfo.get("provider");
            String providerId = (String) sessionSocialInfo.get("providerId");
            String email = (String) sessionSocialInfo.get("email");
            String name = (String) sessionSocialInfo.get("name");
            
            log.info("OAuth2 login success - Provider: {}, ID: {}, Email: {}", provider, providerId, email);
            
            // 기존 사용자 확인 (이메일로)
            Optional<User> existingUser = email != null ? userService.findByEmail(email) : Optional.empty();
            
            if (existingUser.isPresent()) {
                // 기존 사용자: 세션 생성 후 JWT 토큰 생성
                User user = existingUser.get();
                String sessionId = java.util.UUID.randomUUID().toString();
                activeSessionService.setActiveSession(user.getId(), sessionId, jwtProperties.getRefreshTokenExpiration());
                String accessToken = jwtService.generateAccessToken(user, sessionId);
                String refreshToken = jwtService.generateRefreshToken(user, sessionId);
                
                // 마지막 로그인 시간 업데이트
                userService.updateLastLogin(user.getId());
                
                // 세션 정리
                session.removeAttribute("oauth2_social_info");
                
                return LoginResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .user(com.pullit.user.dto.response.UserResponse.from(user))
                        .tokenType("Bearer")
                        .expiresIn(86400L)
                        .user(com.pullit.user.dto.response.UserResponse.from(user))
                        .build();
            } else {
                // 신규 사용자: 예외 발생 (소셜 정보 포함)
                Map<String, Object> socialInfo = new HashMap<>();
                socialInfo.put("provider", provider);
                socialInfo.put("providerId", providerId);
                socialInfo.put("email", email);
                socialInfo.put("name", name);
                
                // 세션 정보는 유지 (회원가입 시 사용)
                throw new SocialLoginNewUserException("신규 사용자입니다. 회원가입을 진행해주세요.", socialInfo);
            }
        } catch (SocialLoginNewUserException e) {
            // 신규 사용자 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("OAuth2 login processing failed", e);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }
    
    private OAuth2LoginResult handleOAuth2LoginSuccessWithSocialInfo(Map<String, Object> socialInfo) {
        try {
            String provider = (String) socialInfo.get("provider");
            String providerId = (String) socialInfo.get("providerId");
            String email = (String) socialInfo.get("email");
            String name = (String) socialInfo.get("name");
            String username = (String) socialInfo.get("username");
            
            log.info("OAuth2 login success with social info - Provider: {}, ID: {}, Email: {}", provider, providerId, email);
            
            // 기존 사용자 확인 (이메일로)
            Optional<User> existingUser = email != null ? userService.findByEmail(email) : Optional.empty();
            
            if (existingUser.isPresent()) {
                // 기존 사용자: 세션 생성 후 JWT 토큰 생성
                User user = existingUser.get();
                log.info("Generating JWT tokens for user: {}", user.getUsername());
                String sessionId = java.util.UUID.randomUUID().toString();
                activeSessionService.setActiveSession(user.getId(), sessionId, jwtProperties.getRefreshTokenExpiration());
                String accessToken = jwtService.generateAccessToken(user, sessionId);
                String refreshToken = jwtService.generateRefreshToken(user, sessionId);
                
                log.info("JWT tokens generated - Access Token: {}..., Refresh Token: {}...", 
                    accessToken != null ? accessToken.substring(0, Math.min(20, accessToken.length())) : "null",
                    refreshToken != null ? refreshToken.substring(0, Math.min(20, refreshToken.length())) : "null");
                
                // 마지막 로그인 시간 업데이트
                userService.updateLastLogin(user.getId());
                
                OAuth2LoginResult result = OAuth2LoginResult.builder()
                        .accessToken(accessToken)
                        .refreshToken(refreshToken)
                        .user(com.pullit.user.dto.response.UserResponse.from(user))  // 사용자 정보 추가
                        .provider(provider)
                        .providerId(providerId)
                        .email(email)
                        .name(name)
                        .username(username)
                        .build();
                
                log.info("OAuth2LoginResult built successfully for user: {}", user.getUsername());
                return result;
            } else {
                // 신규 사용자: 예외 발생 (소셜 정보 포함)
                throw new SocialLoginNewUserException("신규 사용자입니다. 회원가입을 진행해주세요.", socialInfo);
            }
        } catch (SocialLoginNewUserException e) {
            // 신규 사용자 예외는 그대로 전파
            throw e;
        } catch (Exception e) {
            log.error("OAuth2 login processing failed", e);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }
    }

    private String getProviderFromAuthentication(Authentication authentication) {
        // OAuth2 인증에서 제공자 정보 추출
        if (authentication.getPrincipal() instanceof OAuth2User oauth2User) {
            // OAuth2User의 attributes에서 제공자 정보 확인
            Map<String, Object> attributes = oauth2User.getAttributes();
            
            // Google: sub 필드 존재
            if (attributes.containsKey("sub")) return "google";
            
            // Kakao: id 필드 존재하고 kakao_account 있음
            if (attributes.containsKey("id") && attributes.containsKey("kakao_account")) return "kakao";
            
            // Naver: response 필드 존재
            if (attributes.containsKey("response")) return "naver";
        }
        
        // 세션에서 제공자 정보 확인
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            HttpSession session = request.getSession();
            @SuppressWarnings("unchecked")
            Map<String, Object> socialInfo = (Map<String, Object>) session.getAttribute("oauth2_social_info");
            if (socialInfo != null && socialInfo.get("provider") != null) {
                return (String) socialInfo.get("provider");
            }
        } catch (Exception e) {
            log.warn("Failed to get provider from session", e);
        }
        
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
                @SuppressWarnings("unchecked")
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null) {
                    return (String) kakaoAccount.get("email");
                }
                break;
            case "naver":
                @SuppressWarnings("unchecked")
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
                @SuppressWarnings("unchecked")
                Map<String, Object> kakaoAccount = (Map<String, Object>) attributes.get("kakao_account");
                if (kakaoAccount != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> profile = (Map<String, Object>) kakaoAccount.get("profile");
                    return (String) profile.get("nickname");
                }
                break;
            case "naver":
                @SuppressWarnings("unchecked")
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
