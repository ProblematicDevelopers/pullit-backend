package com.pullit.classes.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TeacherInfoResponse {
    private Long teacherId;
    private String teacherName;
    private String email;
    private String phoneNumber;
    private String subject; // 담당 과목
}
