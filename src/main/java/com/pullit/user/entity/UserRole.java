package com.pullit.user.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum UserRole {
    ADMIN("ROLE_ADMIN","관리자"),
    TEACHER("ROLE_TEACHER", "교사"),
    STUDENT("ROLE_STUDENT","학생");

    private final String authority;
    private final String description;



}
