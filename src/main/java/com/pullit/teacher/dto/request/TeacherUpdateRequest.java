package com.pullit.teacher.dto.request;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class TeacherUpdateRequest {
    private String areaCode;  // 과목 코드
    private String schoolName; // 학교명
}
