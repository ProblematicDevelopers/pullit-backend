package com.pullit.exam.dto.response;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExamCountBySubjectResponse {

    /**
     * 과목 ID
     */
    private Long subjectId;

    /**
     * 과목명
     */
    private String subjectName;

    /**
     * TestWizard 시험(Exam) 개수
     */
    private Long testWizardExamCount;

    /**
     * 사용자 생성 시험(UserExam) 개수
     */
    private Long userCreatedExamCount;

    /**
     * 전체 시험 개수 (합계)
     */
    private Long totalExamCount;

    /**
     * 공개 시험 개수
     */
    private Long publicExamCount;

    /**
     * 학교 공개 시험 개수
     */
    private Long schoolExamCount;

    /**
     * 비공개 시험 개수
     */
    private Long privateExamCount;

    // ===== 헬퍼 메서드 =====

    /**
     * 전체 개수 계산
     */
    public void calculateTotal() {
        this.totalExamCount =
                (testWizardExamCount != null ? testWizardExamCount : 0L) +
                        (userCreatedExamCount != null ? userCreatedExamCount : 0L);
    }

    /**
     * 퍼센트 계산 (TestWizard 비율)
     */
    public double getTestWizardPercentage() {
        if (totalExamCount == null || totalExamCount == 0) {
            return 0.0;
        }
        return (testWizardExamCount * 100.0) / totalExamCount;
    }

    /**
     * 퍼센트 계산 (User Created 비율)
     */
    public double getUserCreatedPercentage() {
        if (totalExamCount == null || totalExamCount == 0) {
            return 0.0;
        }
        return (userCreatedExamCount * 100.0) / totalExamCount;
    }
}
