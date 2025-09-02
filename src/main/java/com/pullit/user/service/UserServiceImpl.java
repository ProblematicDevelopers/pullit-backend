package com.pullit.user.service;

import com.pullit.common.annotation.LoggingTrace;
import com.pullit.common.annotation.TimeExecution;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.common.constants.ValidationConstants;
import com.pullit.user.dto.request.PasswordChangeRequest;
import com.pullit.user.dto.request.UserCreateRequest;
import com.pullit.user.dto.request.UserUpdateRequest;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.entity.User;
import com.pullit.user.entity.UserRole;
import com.pullit.user.repository.UserRepository;
import com.pullit.teacher.service.TeacherService;
import com.pullit.student.service.StudentService;
import com.pullit.application.auth.service.VerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final StudentService studentService;
    private final VerificationService verificationService;

    @Override
    @Cacheable(value = "users", key ="#id")
    @TimeExecution
    public User getUserById(Long id){
        return userRepository.findById(id).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }


    @Override
    @Cacheable(value ="users", key="#username")
    @TimeExecution
    public User getUserByUsername(String username) {
        return userRepository.findByUsername(username).orElseThrow(()-> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    public Optional<User> findByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    @Override
    public boolean existsByUsername(String username) {
        return userRepository.existsByUsername(username);
    }

    @Override
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional
    @LoggingTrace
    @TimeExecution
    public UserResponse createUser(UserCreateRequest request) {
        if(userRepository.existsByUsername(request.getUsername())) {
            throw new BusinessException(ErrorCode.DUPLICATE_USERNAME);
        }
        if(userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }

        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .role(request.getUserRole())
                .build();

        User savedUser = userRepository.save(user);
        log.info("새로운 회원 : {}", savedUser.getUsername());
        
        // AuthController에서 직접 Teacher/Student 서비스를 호출하므로 여기서는 User만 저장
        // Teacher/Student 정보는 AuthController에서 처리
        
        return UserResponse.from(savedUser);
    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    @LoggingTrace
    public UserResponse updateUser(Long userId, UserUpdateRequest request) {
        User user = getUserById(userId);

        if(request.getEmail() != null && !request.getEmail().equals(user.getEmail())){
            if(userRepository.existsByEmail(request.getEmail())){
                throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
            }
            user.setEmail(request.getEmail());
        }
        if (request.getFullName() != null) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }

        return UserResponse.from(user);

    }

    @Override
    @Transactional
    @LoggingTrace
    @CacheEvict(value="users", key="#userId")
    public void changePassword(Long userId, PasswordChangeRequest request) {
        User user = getUserById(userId);
        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new BusinessException(ErrorCode.PASSWORD_MISMATCH);
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        log.info("Password changed for user: {}", user.getUsername());



    }

    @Override
    public void updateLastLogin(Long userId) {
        userRepository.updateLastLoginAt(userId, LocalDateTime.now());

    }

    @Override
    @Transactional
    @CacheEvict(value = "users", key = "#userId")
    @LoggingTrace
    public void deleteUser(Long userId) {
        User user = getUserById(userId);
        userRepository.delete(user);
        log.info("User deleted: {}", user.getUsername());
    }

    @Override
    @LoggingTrace
    public String findUsernameByFullNameAndPhone(String fullName, String phone) {
        // 휴대폰 번호 정규화 (하이픈 제거)
        String normalizedPhone = phone.replaceAll("[^0-9]", "");
        
        log.info("Searching for user with fullName: {} and phone: {}", fullName, normalizedPhone);
        
        // 이름으로 사용자 조회
        List<User> users = userRepository.findByFullName(fullName);
        
        log.info("Found {} users with name: {}", users.size(), fullName);
        
        // 모든 사용자 정보 로그 출력 (디버깅용)
        if (users.isEmpty()) {
            log.info("No users found with name: {}. Checking all users...", fullName);
            List<User> allUsers = userRepository.findAll();
            log.info("Total users in database: {}", allUsers.size());
            for (User u : allUsers) {
                log.info("User: id={}, username={}, fullName={}, phone={}", 
                        u.getId(), u.getUsername(), u.getFullName(), u.getPhone());
            }
        }
        
        // 휴대폰 번호로 필터링
        Optional<User> userOpt = users.stream()
                .filter(user -> {
                    String userPhone = user.getPhone() != null ? user.getPhone().replaceAll("[^0-9]", "") : "";
                    boolean matches = normalizedPhone.equals(userPhone);
                    log.info("Comparing phone: {} with user phone: {} = {}", normalizedPhone, userPhone, matches);
                    return matches;
                })
                .findFirst();
        
        User user = userOpt.orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        log.info("Username found for user: {}", user.getFullName());
        return user.getUsername();
    }

    @Override
    @Transactional
    @LoggingTrace
    public void resetPassword(String username, String phone, String verificationCode, String newPassword) {
        // 1. 사용자 조회 (아이디로)
        log.info("Resetting password for user with username: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        
        log.info("Found user: {}", user.getUsername());
        
        // 2. 휴대폰 번호 검증
        String normalizedPhone = phone.replaceAll("[^0-9]", "");
        String userPhone = user.getPhone() != null ? user.getPhone().replaceAll("[^0-9]", "") : "";
        
        if (!normalizedPhone.equals(userPhone)) {
            log.error("Phone number mismatch: expected {}, got {}", userPhone, normalizedPhone);
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }
        
        // 3. 인증번호 검증 (테스트용 휴대폰 번호 처리)
        boolean isValidCode;
        if ("01011111111".equals(normalizedPhone)) {
            // 테스트용 휴대폰 번호는 000000으로 인증
            isValidCode = "000000".equals(verificationCode);
            log.info("Test mode verification: phone={}, code={}, valid={}", normalizedPhone, verificationCode, isValidCode);
        } else {
            // 실제 인증번호 검증
            isValidCode = verificationService.verifyCode(normalizedPhone, verificationCode);
        }
        
        if (!isValidCode) {
            log.error("Invalid verification code for phone: {}", normalizedPhone);
            throw new BusinessException(ErrorCode.INVALID_VERIFICATION_CODE);
        }
        
        // 3. 새 비밀번호 검증
        if (!newPassword.matches(ValidationConstants.PATTERN_PASSWORD)) {
            throw new BusinessException(ErrorCode.INVALID_PASSWORD);
        }
        
        // 4. 비밀번호 변경
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        
        log.info("Password reset for user: {}", user.getUsername());
    }
}
