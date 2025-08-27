package com.pullit.classes.service;

import com.pullit.classes.dto.response.StatsDetailResponse;
import com.pullit.classes.dto.response.StatsLineResponse;

import java.util.List;

public interface StatsService {
    List<StatsLineResponse> findStatsLines(Long userId, Long classId);
    List<StatsDetailResponse> findStatsDetail(Long userId, Long classId);
}
