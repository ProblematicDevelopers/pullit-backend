package com.pullit.user.service;

import com.pullit.common.annotation.LoggingTrace;
import com.pullit.common.annotation.TimeExecution;
import com.pullit.common.exception.BusinessException;
import com.pullit.common.exception.ErrorCode;
import com.pullit.user.dto.request.PasswordChangeRequest;
import com.pullit.user.dto.request.UserCreateRequest;
import com.pullit.user.dto.request.UserUpdateRequest;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.entity.User;
import com.pullit.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

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
}
