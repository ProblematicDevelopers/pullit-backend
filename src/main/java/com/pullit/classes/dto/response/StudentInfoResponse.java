package com.pullit.classes.dto.response;

import lombok.*;

import java.time.LocalDate;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudentInfoResponse {
    private Long studentId;
    private String studentName;
    private String email;
    private String phoneNumber;
    private Long grade; // 학년
    private String studentNumber; // 학번
    private LocalDate enrolledDate; // 수강 신청일
    private String status; // ONLINE, OFFLINE, AWAY
}
