package com.pullit.classes.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassDetailResponse {
    private Long classId;
    private String className;
    private Long classGrade;
    private String classSubject;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
    
    // 담당 교사 정보
    private TeacherInfoResponse teacher;
    
    // 클래스에 속한 학생들 정보
    private List<StudentInfoResponse> students;
    
    // 통계 정보
    private Long totalStudents;
    private Long totalTeachers;
}
