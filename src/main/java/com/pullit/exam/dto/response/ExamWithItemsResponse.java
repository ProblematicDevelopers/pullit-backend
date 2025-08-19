package com.pullit.exam.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamWithItemsResponse {
    private Long examId;
    private String examName;
    private Integer itemCount;
    private List<Long> itemIds;

    private String gradeCode;
    private String gradeName;
    private String areaCode;
    private String areaName;
    private Long subjectId;
    private String subjectName;
}
