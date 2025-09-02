package com.pullit.teacher.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ClassGradeOverviewResponse {
    
    private Long classId;
    private String className;
    private Integer totalStudents;
    private Integer totalExams;
    private Double classAverageScore;
    private Double classMedianScore;
    private Double highestScore;
    private Double lowestScore;
    private LocalDateTime lastExamDate;
    
    // 최근 시험 요약
    private List<RecentExamSummary> recentExams;
    
    // 성적 등급별 학생 수
    private GradeRangeCount gradeDistribution;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentExamSummary {
        private Long examId;
        private String examName;
        private LocalDateTime examDate;
        private Double averageScore;
        private Integer participantCount;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GradeRangeCount {
        private Integer excellent;  // 90-100
        private Integer good;       // 80-89
        private Integer average;    // 70-79
        private Integer belowAverage; // 60-69
        private Integer poor;       // 0-59
    }
}