package com.pullit.auth.service;

import com.pullit.auth.dto.request.LoginRequest;
import com.pullit.auth.dto.response.LoginResponse;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.user.dto.request.UserCreateRequest;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.entity.User;
import com.pullit.user.entity.UserRole;
import com.pullit.user.service.UserService;
import com.pullit.teacher.service.TeacherService;
import com.pullit.student.service.StudentService;
import com.pullit.user.dto.request.TeacherInfo;
import com.pullit.user.dto.request.StudentInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserService userService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final TeacherService teacherService;
    private final StudentService studentService;


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

        // Check if user already exists (from OAuth2 login)
        Optional<User> existingUser = userService.findByUsername(request.getUsername());
        
        if (existingUser.isPresent()) {
            log.info("User already exists from OAuth2 login: {}", request.getUsername());
            User user = existingUser.get();
            
            // Update user role if needed
            user.setRole(request.getUserRole());
            
            // Teacher/Student 정보는 AuthController에서 직접 처리
            
            return UserResponse.from(user);
        } else {
            // New user registration
            UserResponse newUser = userService.createUser(request);
            log.info("Registration successful for username: {}", request.getUsername());
            return newUser;
        }
    }

    @Transactional
    public UserResponse registerWithTeacher(UserCreateRequest request) {
        log.info("Registration with teacher info for username: {}", request.getUsername());

        // 1. Create user first (this will check for existing users)
        UserResponse response = userService.createUser(request);
        
        // 2. If teacher role, create teacher record in separate transaction
        if (request.getUserRole() == UserRole.TEACHER && request.getTeacherInfo() != null) {
            try {
                // Get the created user by ID (more reliable than username)
                User savedUser = userService.getUserById(response.getId());
                
                // Create teacher record - this will be in separate transaction
                createTeacherAsync(savedUser, request.getTeacherInfo());
                log.info("Teacher record creation initiated for user: {}", savedUser.getUsername());
            } catch (Exception e) {
                log.error("Failed to initiate teacher record creation for user: {}, error: {}", 
                         request.getUsername(), e.getMessage());
                // Continue even if teacher creation fails
            }
        }
        
        // 3. If student role, create student record in separate transaction
        if (request.getUserRole() == UserRole.STUDENT && request.getStudentInfo() != null) {
            try {
                // Get the created user by ID (more reliable than username)
                User savedUser = userService.getUserById(response.getId());
                
                // Create student record - this will be in separate transaction
                createStudentAsync(savedUser, request.getStudentInfo());
                log.info("Student record creation initiated for user: {}", savedUser.getUsername());
            } catch (Exception e) {
                log.error("Failed to initiate student record creation for user: {}, error: {}", 
                         request.getUsername(), e.getMessage());
                // Continue even if student creation fails
            }
        }
        
        return response;
    }
    
    // Create teacher asynchronously to avoid transaction conflicts
    private void createTeacherAsync(User user, TeacherInfo teacherInfo) {
        // Run in separate thread to ensure separate transaction
        new Thread(() -> {
            try {
                Thread.sleep(500); // Small delay to ensure user transaction completes
                teacherService.createTeacher(user, teacherInfo);
                log.info("Teacher record created successfully for user: {}", user.getUsername());
            } catch (Exception e) {
                log.error("Failed to create teacher record for user: {}, error: {}", 
                         user.getUsername(), e.getMessage());
            }
        }).start();
    }
    
    // Create student asynchronously to avoid transaction conflicts
    private void createStudentAsync(User user, StudentInfo studentInfo) {
        // Run in separate thread to ensure separate transaction
        new Thread(() -> {
            try {
                Thread.sleep(500); // Small delay to ensure user transaction completes
                studentService.createStudent(user, studentInfo);
                log.info("Student record created successfully for user: {}", user.getUsername());
            } catch (Exception e) {
                log.error("Failed to create student record for user: {}, error: {}", 
                         user.getUsername(), e.getMessage());
            }
        }).start();
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
    
    /**
     * OAuth2 인증 URL 생성
     * @param provider OAuth2 제공자 (naver, kakao, google 등)
     * @return 인증 URL
     */
    public String generateOAuth2AuthorizationUrl(String provider) {
        log.info("Generating OAuth2 authorization URL for provider: {}", provider);
        
        // 실제로는 OAuth2 설정에서 URL을 생성해야 하지만,
        // 임시로 직접 URL을 반환
        switch (provider.toLowerCase()) {
            case "naver":
                // Naver OAuth2 URL 생성 로직
                return "https://nid.naver.com/oauth2.0/authorize?" +
                       "response_type=code&" +
                       "client_id=YOUR_CLIENT_ID&" +
                       "redirect_uri=http://localhost:8080/api/auth/oauth2/callback/naver&" +
                       "state=" + generateState();
            case "kakao":
                return "https://kauth.kakao.com/oauth/authorize?" +
                       "response_type=code&" +
                       "client_id=YOUR_CLIENT_ID&" +
                       "redirect_uri=http://localhost:8080/api/auth/oauth2/callback/kakao";
            case "google":
                return "https://accounts.google.com/o/oauth2/v2/auth?" +
                       "response_type=code&" +
                       "client_id=YOUR_CLIENT_ID&" +
                       "redirect_uri=http://localhost:8080/api/auth/oauth2/callback/google&" +
                       "scope=email profile";
            default:
                throw new BusinessException(ErrorCode.INVALID_INPUT, 
                    "Unsupported OAuth2 provider: " + provider);
        }
    }
    
    /**
     * OAuth2 콜백 처리
     * @param provider OAuth2 제공자
     * @param code 인증 코드
     * @param state CSRF 방지용 state
     * @return LoginResponse
     */
    @Transactional
    public LoginResponse processOAuth2Callback(String provider, String code, String state) {
        log.info("Processing OAuth2 callback for provider: {}", provider);
        
        // TODO: 실제 OAuth2 토큰 교환 및 사용자 정보 조회 로직 구현
        // 현재는 임시로 테스트 데이터 사용
        
        // 임시 데이터 (실제로는 OAuth2 API를 통해 가져와야 함)
        String providerId = "test_" + System.currentTimeMillis();
        String email = "oauth2_" + provider + "@test.com";
        String name = "OAuth2 User";
        
        // socialLogin 메서드를 사용하여 로그인 처리
        return socialLogin(provider, providerId, email, name);
    }
    
    private String generateState() {
        return String.valueOf(System.currentTimeMillis());
    }
}
