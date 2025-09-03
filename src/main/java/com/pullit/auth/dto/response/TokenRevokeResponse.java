package com.pullit.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * 토큰 무효화 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 무효화 응답")
public class TokenRevokeResponse {
    
    @Schema(description = "무효화 성공 여부", example = "true")
    private boolean success;
    
    @Schema(description = "결과 메시지", example = "Token successfully revoked")
    private String message;
    
    @Schema(description = "무효화 시각")
    private Instant revokedAt;
}