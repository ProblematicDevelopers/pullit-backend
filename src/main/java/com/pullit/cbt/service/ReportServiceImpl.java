package com.pullit.cbt.service;

import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.cbt.dto.response.DetailDifficultyResponse;
import com.pullit.cbt.dto.response.DetailErrataResponse;
import com.pullit.cbt.dto.response.DetailEvaluationResponse;
import com.pullit.cbt.entity.AttemptExam;
import com.pullit.cbt.projection.DetailDifficultyProjection;
import com.pullit.cbt.repository.ReportRepository;
import com.pullit.item.enums.AreaCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.awt.geom.Area;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReportServiceImpl implements ReportService {

    private final ReportRepository reportRepository;

    @Override
    public List<AttemptExamResponse> findAttemptExamBySubjectId(Long userId, AreaCode areaCode) {
        return reportRepository
                .findByAreaCodeAndUserId(AreaCode.getStringCode(areaCode), userId)
                .stream()
                .map(AttemptExam::convertToResponseExclude)
                .collect(Collectors.toList());
    }

    @Override
    public List<DetailErrataResponse> findDetailErrataByExamId(Long examId, Long userId) {
        return reportRepository.findDetailErrata(examId, userId)
                .stream()
                .map(DetailErrataResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<DetailDifficultyResponse> findDetailDifficultyByExamId(Long userId, Long examId) {

        return reportRepository.findDetailDifficultyByExamId(userId, examId).stream()
                .map(DetailDifficultyResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<DetailEvaluationResponse> findDetailEvaluationByExamId(Long userId, Long examId) {
        return reportRepository.findDetailEvaluationByExamId(userId, examId).stream()
                .map(DetailEvaluationResponse::from)
                .collect(Collectors.toList());
    }

}
