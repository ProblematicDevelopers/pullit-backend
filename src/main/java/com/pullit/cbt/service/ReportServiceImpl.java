package com.pullit.cbt.service;

import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.cbt.entity.AttemptExam;
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
}
