package com.pullit.user.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.pullit.user.entity.User;
import com.pullit.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 응답")

public class UserResponse {


    @Schema(description = "사용자 ID", example = "1")
    private Long id;

    @Schema(description = "사용자명", example = "user123")
    private String username;

    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @Schema(description = "전체 이름", example = "홍길동")
    private String fullName;

    @Schema(description = "사용자 역할", example = "STUDENT")
    private UserRole role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "마지막 로그인 시간", example = "2024-01-01 12:00:00")
    private LocalDateTime lastLoginAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "생성일시", example = "2024-01-01 10:00:00")
    private LocalDateTime createdDate;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .fullName(user.getFullName())
                .role(user.getRole())
                .lastLoginAt(user.getLastLoginAt())
                .createdDate(user.getCreatedDate())
                .build();
    }

}
