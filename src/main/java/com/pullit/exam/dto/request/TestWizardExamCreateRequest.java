package com.pullit.exam.dto.request;

import com.pullit.exam.enums.ExamVisibility;
import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TestWizardExamCreateRequest {
    @NotBlank(message = "시험지 이름은 필수입니다.")
    @Size(max = 500)
    private String examName;

    @NotBlank(message = "학년 코드는 필수입니다.")
    @Size(max = 10)
    private String gradeCode;

    @Size(max = 20)
    private String gradeName;

    @Size(max = 10)
    private String termCode;

    @Size(max = 20)
    private String termName;

    @Size(max = 10)
    private String areaCode;

    @Size(max = 20)
    private String areaName;

    private String examType = "TESTWIZARD";

    @NotNull(message="공개 범위는 필수입니다.")
    private ExamVisibility visibility = ExamVisibility.PRIVATE;

    private Long classId;

    @Min(10)
    @Max(120)
    private Integer timeLimit = 50;

    private LocalDate examDate;

    @Size(max = 1000)
    private String description;

    private List<TestWizardItemRequest> items;

    private Boolean shuffleQuestions = false;
    private Boolean showAnswerAfterSubmit = false;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class TestWizardItemRequest {
        private Long itemId;
        private Long subjectId;
        private Integer itemOrder;
        private Integer points = 5;
    }
    public boolean isValid() {
        return visibility != ExamVisibility.CLASS_ONLY || classId != null;
    }
}
