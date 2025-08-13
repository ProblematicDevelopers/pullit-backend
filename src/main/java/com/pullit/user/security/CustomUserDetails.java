package com.pullit.user.security;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.pullit.user.entity.UserRole;
import lombok.Builder;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
@Builder
public class CustomUserDetails implements UserDetails {
    private final Long userId;
    private final String username;
    private final String email;
    private final String fullName;
    private final UserRole role;

    @JsonIgnore
    private final String password;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(role.getAuthority()));
    }

    @Override
    @JsonIgnore
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return username;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static CustomUserDetails fromJwtClaims(Map<String, Object> claims) {
        Long userId = Long.parseLong(claims.get("sub").toString());
        String username = (String) claims.get("username");
        String email = (String) claims.get("email");
        String fullName = (String) claims.get("fullName");
        String roleString = (String) claims.get("role");
        UserRole role = UserRole.valueOf(roleString);

        return CustomUserDetails.builder()
                .userId(userId)
                .username(username)
                .email(email)
                .fullName(fullName)
                .role(role)
                .password(null) // JWT에서는 비밀번호 불필요
                .build();

    }

    // 헬퍼 메서드들
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isTeacher() {
        return role == UserRole.TEACHER;
    }

    public boolean isStudent() {
        return role == UserRole.STUDENT;
    }

}