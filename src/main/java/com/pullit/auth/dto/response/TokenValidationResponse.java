package com.pullit.auth.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "토큰 검증 응답")
public class TokenValidationResponse {

    /**
     * 토큰 유효성
     */
    @Schema(description = "토큰 유효 여부", example = "true")
    private boolean valid;

    /**
     * 토큰 타입
     * - "access" 또는 "refresh"
     */
    @Schema(description = "토큰 타입", example = "access")
    private String tokenType;

    /**
     * 토큰 주체 (사용자 ID)
     */
    @Schema(description = "사용자 ID", example = "1")
    private String subject;

    /**
     * 토큰 만료 시간
     */
    @Schema(description = "만료 시간", example = "2024-01-02T12:00:00Z")
    private Instant expiresAt;

    /**
     * 남은 유효 시간 (초)
     */
    @Schema(description = "남은 시간(초)", example = "3600")
    private Long remainingTime;

    /**
     * 에러 메시지 (유효하지 않은 경우)
     */
    @Schema(description = "에러 메시지", example = "Token has expired")
    private String error;
}
