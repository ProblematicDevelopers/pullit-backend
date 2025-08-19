package com.pullit.auth.service;

import com.pullit.auth.dto.request.LoginRequest;
import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.user.dto.request.UserCreateRequest;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.entity.User;
import com.pullit.user.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;


    @Transactional
    public LoginResponse login(LoginRequest request) {
        log.info("Login attempt for username: {}", request.getUsername());


        User user = userService.findByUsername(request.getUsername())
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found - {}", request.getUsername());
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });


        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed: Invalid password for user - {}", request.getUsername());
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }


        userService.updateLastLogin(user.getId());
        log.info("Login successful for user: {}", user.getUsername());

        // 5. JWT 토큰 생성
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);


        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserResponse.from(user))
                .build();
    }


    @Transactional
    public UserResponse register(UserCreateRequest request) {
        log.info("Registration attempt for username: {}", request.getUsername());


        UserResponse newUser = userService.createUser(request);

        log.info("Registration successful for username: {}", request.getUsername());
        return newUser;
    }


    @Transactional(readOnly = true)
    public LoginResponse refresh(String refreshToken) {
        log.debug("Token refresh attempt");


        if (!jwtService.validateToken(refreshToken)) {
            log.warn("Token refresh failed: Invalid refresh token");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        if (!jwtService.isRefreshToken(refreshToken)) {
            log.warn("Token refresh failed: Not a refresh token");
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        Long userId;
        try {
            userId = jwtService.getUserIdFromToken(refreshToken);
        } catch (Exception e) {
            log.error("Token refresh failed: Cannot extract user ID", e);
            throw new BusinessException(ErrorCode.INVALID_TOKEN);
        }

        User user = userService.getUserById(userId);

        String newAccessToken = jwtService.generateAccessToken(user);
        String newRefreshToken = jwtService.generateRefreshToken(user);

        log.info("Token refresh successful for user: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)  // 24시간
                .user(UserResponse.from(user))
                .build();
    }


    @Transactional
    public void logout(Long userId) {
        log.info("Logout for user ID: {}", userId);

        // TODO: Redis에서 Refresh Token 삭제
        // TODO: Access Token 블랙리스트 추가 (선택사항)


    }

    @Transactional
    public LoginResponse loginByEmail(String email, String password) {
        log.info("Login attempt with email: {}", email);

        User user = userService.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Login failed: Email not found - {}", email);
                    return new BusinessException(ErrorCode.INVALID_CREDENTIALS);
                });

        if (!passwordEncoder.matches(password, user.getPassword())) {
            log.warn("Login failed: Invalid password for email - {}", email);
            throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
        }

        userService.updateLastLogin(user.getId());

        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserResponse.from(user))
                .build();
    }

    /**
     * 소셜 로그인 사용자 정보로 로그인 처리
     * @param provider 소셜 로그인 제공자 (google, kakao, naver)
     * @param providerId 소셜 로그인 ID
     * @param email 이메일
     * @param name 이름
     * @return LoginResponse
     */
    @Transactional
    public LoginResponse socialLogin(String provider, String providerId, String email, String name) {
        log.info("Social login attempt - Provider: {}, ID: {}, Email: {}", provider, providerId, email);

        // 기존 사용자 확인 (이메일로)
        User user = userService.findByEmail(email)
                .orElseGet(() -> {
                    log.info("Creating new user for social login - Provider: {}, Email: {}", provider, email);
                    // 새 사용자 생성
                    String username = generateSocialUsername(provider, providerId);
                    String password = generateRandomPassword();
                    
                    UserCreateRequest userRequest = UserCreateRequest.builder()
                            .username(username)
                            .email(email)
                            .fullName(name)
                            .password(password)
                            .build();
                    
                    UserResponse userResponse = userService.createUser(userRequest);
                    return userService.getUserById(userResponse.getId());
                });

        // 마지막 로그인 시간 업데이트
        userService.updateLastLogin(user.getId());

        // JWT 토큰 생성
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user);

        log.info("Social login successful for user: {}", user.getUsername());

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(86400L)
                .user(UserResponse.from(user))
                .build();
    }

    private String generateSocialUsername(String provider, String providerId) {
        return provider + "_" + providerId;
    }

    private String generateRandomPassword() {
        return "SOCIAL_" + System.currentTimeMillis();
    }
}
