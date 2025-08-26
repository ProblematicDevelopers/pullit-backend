package com.pullit.exam.dto.response;

import java.time.LocalDate;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExamSchoolResponse {
    private Long id;
    private String examName;
    private String gradeCode;
    private String gradeName;
    private String termCode;
    private String termName;
    private String areaCode;
    private String areaName;
    private String examType;
    private String visibility;
    private String pdfUrl;
    private String answerPdfUrl;
    private Integer timeLimit;
    private LocalDate examDate;
    private String description;
    private Integer totalItems;
}
