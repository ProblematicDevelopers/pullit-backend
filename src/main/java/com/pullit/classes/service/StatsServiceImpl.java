package com.pullit.classes.service;

import com.pullit.cbt.entity.AttemptExam;
import com.pullit.classes.dto.response.StatsDetailResponse;
import com.pullit.classes.dto.response.StatsLineResponse;
import com.pullit.classes.repository.StatsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StatsServiceImpl implements StatsService {

    private final StatsRepository statsRepository;

    @Override
    public List<StatsLineResponse> findStatsLines(Long userId, Long classId) {
        return statsRepository.findStatsLines(userId, classId).stream()
                .map(StatsLineResponse::from)
                .collect(Collectors.toList());
    }

    @Override
    public List<StatsDetailResponse> findStatsDetail(Long userId, Long classId) {
        return statsRepository.findStatsDetail(userId, classId).stream()
                .map(StatsDetailResponse::from)
                .collect(Collectors.toList());
    }
}
