package com.pullit.cbt.service;

import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.cbt.entity.AttemptExam;
import com.pullit.item.enums.AreaCode;

import java.util.List;

public interface ReportService {
    List<AttemptExamResponse> findAttemptExamBySubjectId(Long userId, AreaCode areaCode);
}