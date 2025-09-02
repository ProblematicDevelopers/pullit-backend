package com.pullit.exam.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TeacherLiveExamRequest {
    
    @NotBlank(message = "시험명은 필수입니다")
    private String examName;
    
    @NotNull(message = "클래스 ID는 필수입니다")
    private Long classId;
    
    private Integer timeLimit; // 분 단위
    
    private LocalDate scheduledDate;
    
    private LocalTime scheduledTime;
    
    private String description;
    
    private String gradeCode;
    
    private String termCode;
    
    private String subjectCode;
    
    // 문제 ID 목록 (UserExamItem IDs)
    private List<Long> examItemIds;
    
    // 기존 시험지에서 문제 가져오기
    private Long sourceExamId;
    
    // 문제 선택 옵션
    private boolean randomSelection;
    private Integer questionCount; // 랜덤 선택 시 문제 개수
}