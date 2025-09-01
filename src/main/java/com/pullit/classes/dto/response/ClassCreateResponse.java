package com.pullit.classes.dto.response;

import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassCreateResponse {
    
    private Long classId;
    private String className;
    private String classGrade;  // DB 코드 사용: 07, 08, 09
    private String classSubject;  // DB 코드 사용: MA, KO, EN, SC, SO
    private String classSubjectName;  // "수학", "국어" 등 한글명
    private String inviteCode;  // 자동 생성된 초대 코드
    private LocalDateTime createdDate;
    
    // 생성한 선생님 정보
    private TeacherInfo teacher;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TeacherInfo {
        private Long userId;
        private String fullName;
        private String email;
        private String schoolName;
    }
}