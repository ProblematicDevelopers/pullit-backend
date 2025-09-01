package com.pullit.classes.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentInvitationRequest {
    
    @NotEmpty(message = "초대할 학생 정보는 필수입니다")
    @Size(max = 50, message = "한 번에 최대 50명까지 초대 가능합니다")
    private List<StudentInviteInfo> students;
    
    @Size(max = 500, message = "초대 메시지는 500자 이내여야 합니다")
    private String invitationMessage;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StudentInviteInfo {
        
        @Email(message = "유효한 이메일 형식이어야 합니다")
        private String email;
        
        @Size(min = 2, max = 30, message = "이름은 2-30자 사이여야 합니다")
        private String name;  // 학생 이름 (선택)
        
        @Min(value = 1, message = "번호는 1 이상이어야 합니다")
        @Max(value = 50, message = "번호는 50 이하여야 합니다")
        private Long studentNo;  // 학번 (선택)
    }
}