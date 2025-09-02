package com.pullit.student.dto.request;

import com.pullit.common.embedded.StringCodeNamePair;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "학생 정보 수정 요청")
public class StudentUpdateRequest {
    
    @Schema(description = "학년 정보")
    private StringCodeNamePair grade;
    
    @Schema(description = "학교 ID")
    private Long schoolId;
    
    @Schema(description = "학교명")
    private String schoolName;
}
