package com.pullit.user.dto.request;

import lombok.Data;

@Data
public class StudentInfo {
    private Long classGroupId;
    private Long studentNo;
    private Long grade;
}