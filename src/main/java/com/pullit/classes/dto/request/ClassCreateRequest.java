package com.pullit.classes.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassCreateRequest {
    
    @NotBlank(message = "학급명은 필수입니다")
    @Size(min = 2, max = 100, message = "학급명은 2-100자 사이여야 합니다")
    private String className;  // 예: "2024 중2-3반 수학"
    
    @NotBlank(message = "학년은 필수입니다")
    @Pattern(regexp = "^(07|08|09)$", message = "유효하지 않은 학년 코드입니다")
    private String classGrade;  // 07(1학년), 08(2학년), 09(3학년)
    
    @NotBlank(message = "과목은 필수입니다")
    @Pattern(regexp = "^(MA|KO|EN|SC|SO)$", 
             message = "유효하지 않은 과목 코드입니다")
    private String classSubject;  // MA(수학), KO(국어), EN(영어), SC(과학), SO(사회)
    
    @Size(max = 500, message = "설명은 500자 이내여야 합니다")
    private String description;  // 학급 설명
    
    private Long schoolId;  // 학교 ID (선택)
    
    @Builder.Default
    private Boolean generateInviteCode = true;  // 초대코드 자동 생성 여부
}