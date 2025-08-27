package com.pullit.exam.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserExamCreateRequest {
    private String examName;
    private String gradeCode;
    private String gradeName;
    private String termCode;
    private String termName;
    private String areaCode;
    private String areaName;
    private String examType;
    private Integer timeLimit;
    private LocalDate examDate;
    private String description;
    private List<UserExamItemRequest> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UserExamItemRequest {
        private Long itemId;
        private Long subjectId;
        private Integer itemOrder;
        private Integer points;
    }
}
