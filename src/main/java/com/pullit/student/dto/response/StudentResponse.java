package com.pullit.student.dto.response;

import lombok.*;

@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Builder

public class StudentResponse {
    private Long userId;
    private Long classGroupId;
    private Long studentNo;
    private Long grade;
}
