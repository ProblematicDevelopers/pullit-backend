package com.pullit.cbt.dto.request;

import lombok.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserExamToCbtRequest {
    
    @NotNull(message = "원본 시험 ID는 필수입니다")
    private Long sourceExamId;  // 원본 UserExam ID
    
    @NotNull(message = "대상 학급 ID는 필수입니다")
    private Long targetClassId;  // CBT를 출제할 학급 ID
    
    @NotNull(message = "시험 유형을 선택해주세요")
    private ExamDeliveryType deliveryType;  // CBT 또는 ASSIGNMENT
    
    @Positive(message = "제한 시간은 양수여야 합니다")
    private Integer timeLimit;  // 시험 제한 시간 (분)
    
    private String examName;  // 새로운 시험 이름 (없으면 원본 이름 + " (CBT)" 사용)
    
    private String description;  // 시험 설명
    
    private Boolean shuffleQuestions;  // 문제 순서 섞기 여부
    
    private Boolean showResultImmediately;  // 시험 종료 후 바로 결과 표시 여부
    
    public enum ExamDeliveryType {
        CBT,        // 실시간 CBT 시험
        ASSIGNMENT  // 과제 형태로 제출
    }
}