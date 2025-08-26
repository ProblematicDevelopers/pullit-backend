package com.pullit.classes.service;

import com.pullit.classes.dto.response.ClassDetailResponse;
import com.pullit.exam.dto.response.UserExamSchoolResponse;

import java.util.List;

public interface ClassesService {
    
  
    /**
     * 클래스 ID로 특정 클래스의 상세 정보를 조회
     */
    ClassDetailResponse getClassDetailById(Long classId);

    List<UserExamSchoolResponse> getExamsByClassId(Long classId);

    UserExamSchoolResponse getExamsByClassIdAndExamId(Long classId, Long examId);
}
