package com.pullit.classes.dto.response;

import lombok.*;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentInvitationResponse {
    
    private Long classId;
    private String className;
    private int totalInvited;
    private int successCount;
    private int failedCount;
    
    private List<InvitationResult> results;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InvitationResult {
        private String email;
        private boolean success;
        private String message;  // 성공/실패 메시지
        private InvitationStatus status;
    }
    
    public enum InvitationStatus {
        SENT,           // 초대 전송됨
        ALREADY_MEMBER, // 이미 학급 멤버
        USER_NOT_FOUND, // 사용자 없음
        NOT_STUDENT,    // 학생 권한이 아님
        ERROR          // 오류 발생
    }
}