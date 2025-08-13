package com.pullit.user.service;

import com.pullit.user.entity.User;

import java.util.Optional;

public interface UserService {

    // 사용자 조회
    User getUserById(Long id);
    User getUserByUsername(String username);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);

    // 사용자 생성
    UserResponse createUser(UserCreateRequest request);

    // 사용자 정보 수정
    UserResponse updateUser(Long userId, UserUpdateRequest request);

    // 비밀번호 변경
    void changePassword(Long userId, PasswordChangeRequest request);

    // 사용자 존재 여부 확인
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    // 로그인 시 최종 로그인 시간 업데이트
    void updateLastLogin(Long userId);

    // 사용자 삭제
    void deleteUser(Long userId);

}
