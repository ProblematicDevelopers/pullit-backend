package com.pullit.cbt.service;

import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.cbt.dto.response.DetailDifficultyResponse;
import com.pullit.cbt.dto.response.DetailErrataResponse;
import com.pullit.cbt.dto.response.DetailEvaluationResponse;
import com.pullit.cbt.entity.AttemptExam;
import com.pullit.item.enums.AreaCode;

import java.util.List;

public interface ReportService {
    List<AttemptExamResponse> findAttemptExamBySubjectId(Long userId, AreaCode areaCode);
    List<DetailErrataResponse> findDetailErrataByExamId(Long examId, Long userId);
    List<DetailDifficultyResponse> findDetailDifficultyByExamId(Long userId, Long examId);
    List<DetailEvaluationResponse> findDetailEvaluationByExamId(Long userId, Long examId);
}