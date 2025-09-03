package com.pullit.auth.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 토큰 무효화 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 무효화 요청")
public class TokenRevokeRequest {
    
    @NotBlank(message = "Token is required")
    @Schema(description = "무효화할 토큰 (액세스 또는 리프레시)", example = "eyJhbGciOiJSUzI1NiJ9...")
    private String token;
    
    @Schema(description = "무효화 사유", example = "Suspicious activity detected")
    private String reason;
}