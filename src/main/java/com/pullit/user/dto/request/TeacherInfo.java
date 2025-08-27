package com.pullit.user.dto.request;

import lombok.Data;

@Data
public class TeacherInfo {
    private String schoolName;
    private String areaCode;  // 과목 코드
    private String areaName;  // 과목명

}
