package com.pullit.classes.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ClassResponse {
    private Long classId;
    private String className;
    private Long classGrade;
    private String classSubject;
    private Long teacherId;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
