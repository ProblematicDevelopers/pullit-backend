package com.pullit.user.dto.request;


import com.pullit.common.annotation.ValidPassword;
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
@Schema(description = "비밀번호 변경 요청")
public class PasswordChangeRequest {
    @NotBlank(message = "현재 비밀번호는 필수입니다")
    @Schema(description = "현재 비밀번호", example = "OldPassword123!")
    private String oldPassword;

    @NotBlank(message = "새 비밀번호는 필수입니다")
    @ValidPassword
    @Schema(description = "새 비밀번호", example = "NewPassword123!")
    private String newPassword;

    @NotBlank(message = "비밀번호 확인은 필수입니다")
    @Schema(description = "새 비밀번호 확인", example = "NewPassword123!")
    private String confirmPassword;

    public boolean isPasswordMatching() {
        return newPassword != null && newPassword.equals(confirmPassword);
    }

}
