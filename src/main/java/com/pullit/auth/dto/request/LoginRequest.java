package com.pullit.auth.dto.request;

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
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "로그인 요청")
public class LoginRequest {

    @Pattern(regexp = PATTERN_USERNAME,message = "ID의 형식이 올바르지 않습니다")
    @NotBlank(message = "ID는 필수입니다")
    @Size(min = 4, max = 20, message = "ID는 4~20자여야 합니다")
    @Schema(description = "유저ID", example = "testuser", requiredMode = Schema.RequiredMode.REQUIRED)
    private String username;

    @Pattern(regexp = PATTERN_PASSWORD, message = MSG_INVALID_PASSWORD)
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    @Schema(description = "비밀번호", example = "Password123!", requiredMode = Schema.RequiredMode.REQUIRED)
    private String password;

}
