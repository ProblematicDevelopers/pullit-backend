package com.pullit.user.dto.request;

import com.pullit.common.annotation.PhoneNumber;
import com.pullit.common.annotation.ValidEnum;
import com.pullit.common.annotation.ValidPassword;
import com.pullit.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "회원가입 요청")
public class UserCreateRequest {
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 4, max = 20, message = "사용자명은 4~20자여야 합니다")
    @Schema(description = "사용자명", example = "user123")
    private String username;

    @NotBlank(message = "비밀번호는 필수입니다")
    @ValidPassword
    @Schema(description = "비밀번호", example = "Password123!")
    private String password;

    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Schema(description = "이메일", example = "user@example.com")
    private String email;

    @PhoneNumber(required = false)  // 커스텀 전화번호 검증
    @Schema(description = "전화번호", example = "010-1234-5678")
    private String phone;

    @NotBlank(message = "이름은 필수입니다")
    @Size(max = 30, message = "이름은 30자 이내여야 합니다")
    @Schema(description = "전체 이름", example = "홍길동")
    private String fullName;

    @ValidEnum(enumClass = UserRole.class, message = "유효하지 않은 역할입니다")
    @Schema(description = "사용자 역할", example = "STUDENT")
    private String role;

    public UserRole getUserRole() {
        return UserRole.valueOf(role);
    }

}
