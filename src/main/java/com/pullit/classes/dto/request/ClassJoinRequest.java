package com.pullit.classes.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassJoinRequest {
    
    @NotBlank(message = "초대 코드는 필수입니다")
    @Pattern(regexp = "^[A-Z0-9]{6,8}$", 
             message = "초대 코드는 6-8자리 영문 대문자와 숫자 조합이어야 합니다")
    private String inviteCode;
    
    @Min(value = 1, message = "번호는 1 이상이어야 합니다")
    @Max(value = 50, message = "번호는 50 이하여야 합니다")
    private Long studentNo;  // 본인이 원하는 학번 (선택)
}