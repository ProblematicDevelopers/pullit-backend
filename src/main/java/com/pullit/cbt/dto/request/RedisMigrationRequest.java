package com.pullit.cbt.dto.request;

import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class RedisMigrationRequest {
    private Map<String, Integer> questionTimes; // 문제별 소요 시간
    private Map<String, Integer> questionVisits; // 문제별 방문 횟수
    private Map<String, String> questionAnswers; // 문제별 답변
    private Integer totalTime; // 총 시험 시간
    private String status; // 시험 상태
}
