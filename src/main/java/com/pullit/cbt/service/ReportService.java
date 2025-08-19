package com.pullit.cbt.service;

import com.pullit.cbt.entity.AttemptExam;
import com.pullit.item.enums.AreaCode;

import java.util.List;

public interface ReportService {
    List<AttemptExam> findAttemptExamBySubjectId(Long userId, AreaCode areaCode);
}