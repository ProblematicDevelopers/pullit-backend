package com.pullit.cbt.dto.response;

import lombok.*;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CbtExamResponse {
    private Long examId;
    private String examName;
    private String examType;
    private Integer totalItems;
    private Integer timeLimit;
    private String gradeName;
    private String termName;
    private String areaName;
    private String visibility;
    private List<CbtExamItemResponse> examItems;
}
