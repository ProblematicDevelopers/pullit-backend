package com.pullit.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {
    private Long totalStudents;      // 전체 학생 수
    private Long activeExams;        // 진행 중인 시험 수
    private Double averageGrade;     // 평균 성적
    private Integer todayClasses;    // 오늘 수업 수
    private Long createdExams;       // 생성한 시험 수
    private Long totalQuestions;     // 총 문항 수
    private Long onlineStudents;     // 현재 접속 중인 학생 수
}