package com.pullit.classes.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;



@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassUpdateRequest {
    
    @NotBlank(message = "학급명은 필수입니다")
    @Size(min = 1, max = 50, message = "학급명은 1~50자 이내여야 합니다")
    private String className;
    
    @NotBlank(message = "학년은 필수입니다")
    @Pattern(regexp = "^(07|08|09)$", message = "학년은 07(1학년), 08(2학년), 09(3학년) 중 하나여야 합니다")
    private String classGrade;
    
    @NotBlank(message = "과목은 필수입니다")
    @Pattern(regexp = "^(MA|KO|EN|SC|SO)$", message = "과목은 MA(수학), KO(국어), EN(영어), SC(과학), SO(사회) 중 하나여야 합니다")
    private String classSubject;
}