package com.pullit.user.dto.request;

import com.pullit.common.embedded.StringCodeNamePair;
import lombok.Data;

@Data
public class StudentInfo {
    private Long classGroupId;
    private Long studentNo;
    private StringCodeNamePair grade;
    private Long schoolId;  // 학교 ID 추가
    private String schoolName;  // 학교명 추가
}