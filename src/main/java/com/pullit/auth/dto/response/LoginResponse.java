package com.pullit.auth.dto.response;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.pullit.user.dto.response.UserResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "로그인 응답")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LoginResponse {

    @Schema(description = "액세스 토큰", example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String accessToken;

    @Schema(description = "리프레시 토큰",
            example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...")
    private String refreshToken;

    @Schema(description = "토큰 타입", example = "Bearer")
    @Builder.Default
    private String tokenType = "Bearer";

    @Schema(description = "만료 시간(초)", example = "86400")
    private Long expiresIn;

    @Schema(description = "사용자 정보")
    private UserResponse user;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(description = "토큰 발급 시간", example = "2024-01-01 12:00:00")
    private LocalDateTime issuedAt;
}
