package com.pullit.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 재발급 요청")
public class TokenRefreshRequest {
    @NotBlank(message = "Refresh Token은 필수입니다")
    @Schema(description = "리프레시 토큰",
            example = "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
            requiredMode =  Schema.RequiredMode.REQUIRED)
    private String refreshToken;
}
