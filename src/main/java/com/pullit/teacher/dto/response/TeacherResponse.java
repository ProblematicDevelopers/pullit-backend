package com.pullit.teacher.dto.response;

import com.pullit.teacher.entity.Teacher;
import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder
public class TeacherResponse {
    private Long userId;
    private String areaName;  // 과목명
    private String areaCode;  // 과목 코드
    private String schoolName; // 학교명
    private Long schoolId;    // 학교 ID
    private String areaCodeName; // 지역 코드
    private String areaNameName; // 지역명

    public static TeacherResponse from(Teacher teacher) {
        if (teacher == null) {
            return null;
        }
        
        return TeacherResponse.builder()
                .userId(teacher.getUserId())
                .areaName(teacher.getAreaDisplayName())
                .areaCode(teacher.getArea() != null ? teacher.getArea().getCode() : null)
                .schoolName(teacher.getSchool() != null ? teacher.getSchool().getSchoolName() : null)
                .schoolId(teacher.getSchool() != null ? teacher.getSchool().getId() : null)
                .areaCodeName(teacher.getArea() != null ? teacher.getArea().getCode() : null)
                .areaNameName(teacher.getArea() != null ? teacher.getArea().getName() : null)
                .build();
    }
}
