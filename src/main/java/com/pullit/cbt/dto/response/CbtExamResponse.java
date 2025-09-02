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
    // 추가 메타: 생성 시 알림 및 라우팅에 활용
    private Long classId;
    private List<Long> studentIds;
}
