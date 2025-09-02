package com.pullit.user.repository;

import com.pullit.user.dto.request.PasswordChangeRequest;
import com.pullit.user.dto.request.UserCreateRequest;
import com.pullit.user.dto.request.UserUpdateRequest;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);


    Optional<User> findByUsernameOrEmail(String username, String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Modifying
    @Query("UPDATE User u SET u.lastLoginAt = :lastLoginAt WHERE u.id = :userId")
    void updateLastLoginAt(@Param("userId") Long userId, @Param("lastLoginAt") LocalDateTime lastLoginAt);

    // 아이디 찾기 - 이름으로 사용자 조회
    @Query("SELECT u FROM User u WHERE u.fullName = :fullName")
    List<User> findByFullName(@Param("fullName") String fullName);
    
    // 비밀번호 찾기용 메서드 (기존 findByUsername과 동일하므로 제거)

}
