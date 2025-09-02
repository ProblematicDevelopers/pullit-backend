package com.pullit.student.dto.response;

import com.pullit.common.embedded.StringCodeNamePair;
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
    private StringCodeNamePair grade;
    private Long schoolId;
    private String schoolName;
}
