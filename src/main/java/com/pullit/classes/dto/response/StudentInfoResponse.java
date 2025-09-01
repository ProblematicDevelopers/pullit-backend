package com.pullit.classes.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudentInfoResponse {
    private Long studentId;
    private String studentName;
    private String email;
    private String phoneNumber;
    private Long grade;
    private String studentNumber;
    private LocalDate enrolledDate;
    private String status;  // AVAILABLE, ASSIGNED, etc.
}