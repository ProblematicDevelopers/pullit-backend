package com.pullit.user.dto.request;

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
@Schema(description = "아이디 찾기 요청")
public class FindUsernameRequest {
    @NotBlank(message = "이름은 필수입니다")
    @Schema(description = "이름", example = "홍길동")
    private String fullName;

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Schema(description = "휴대폰 번호", example = "010-1234-5678")
    private String phone;
}
