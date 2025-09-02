package com.pullit.user.dto.request;

import com.pullit.common.constants.ValidationConstants;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static com.pullit.common.constants.ValidationConstants.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "비밀번호 찾기 요청")
public class FindPasswordRequest {
    @NotBlank(message = "아이디는 필수입니다")
    @Schema(description = "아이디", example = "user123")
    private String username;

    @NotBlank(message = "휴대폰 번호는 필수입니다")
    @Schema(description = "휴대폰 번호", example = "010-1234-5678")
    private String phone;

    @NotBlank(message = "인증번호는 필수입니다")
    @Schema(description = "인증번호", example = "123456")
    private String verificationCode;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @Pattern(regexp = PATTERN_PASSWORD, message = MSG_INVALID_PASSWORD)
    @Size(min = MIN_PASSWORD_LENGTH, max = MAX_PASSWORD_LENGTH, message = "비밀번호는 8~100자여야 합니다")
    @Schema(description = "새 비밀번호", example = "NewPassword123!")
    private String newPassword;

    @NotBlank(message = "새 비밀번호 확인은 필수입니다")
    @Schema(description = "새 비밀번호 확인", example = "NewPassword123!")
    private String confirmPassword;
}
