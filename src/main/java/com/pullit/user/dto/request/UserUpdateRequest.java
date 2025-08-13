package com.pullit.user.dto.request;

import com.pullit.common.annotation.PhoneNumber;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "사용자 정보 수정 요청")
public class UserUpdateRequest {

    @Email(message = "이메일 형식이 올바르지 않습니다")
    @Schema(description = "이메일", example = "newemail@example.com")
    private String email;

    @PhoneNumber(required = false)
    @Schema(description = "전화번호", example = "010-9876-5432")
    private String phone;

    @Size(max = 30, message = "이름은 30자 이내여야 합니다")
    @Schema(description = "전체 이름", example = "김철수")
    private String fullName;

}
