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
    private String classGrade;  // '07', '08', '09'
    private String classSubject; // 'MA', 'KO', 'EN', 'SC', 'SO'
    private Long teacherId;
    private LocalDateTime createdDate;
    private LocalDateTime updatedDate;
}
