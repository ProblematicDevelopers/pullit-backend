package com.pullit.exam.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "exam_statistics")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamStatistics {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "exam_id", nullable = false)
    private Long examId;
    
    @Column(name = "exam_type", nullable = false, length = 20)
    private String examType;  // SYSTEM, USER
    
    @Column(name = "stat_date", nullable = false)
    private LocalDate statDate;  // 통계 날짜
    
    // 일별 통계
    @Column(name = "daily_views")
    @Builder.Default
    private Integer dailyViews = 0;
    
    @Column(name = "daily_downloads")
    @Builder.Default
    private Integer dailyDownloads = 0;
    
    @Column(name = "daily_shares")
    @Builder.Default
    private Integer dailyShares = 0;
    
    @Column(name = "daily_prints")
    @Builder.Default
    private Integer dailyPrints = 0;
    
    @Column(name = "daily_completes")
    @Builder.Default
    private Integer dailyCompletes = 0;
    
    // 주간 통계 (최근 7일)
    @Column(name = "weekly_uses")
    @Builder.Default
    private Integer weeklyUses = 0;
    
    @Column(name = "weekly_views")
    @Builder.Default
    private Integer weeklyViews = 0;
    
    @Column(name = "weekly_downloads")
    @Builder.Default
    private Integer weeklyDownloads = 0;
    
    // 월간 통계 (최근 30일)
    @Column(name = "monthly_uses")
    @Builder.Default
    private Integer monthlyUses = 0;
    
    @Column(name = "monthly_views")
    @Builder.Default
    private Integer monthlyViews = 0;
    
    @Column(name = "monthly_downloads")
    @Builder.Default
    private Integer monthlyDownloads = 0;
    
    // 누적 통계
    @Column(name = "total_views")
    @Builder.Default
    private Long totalViews = 0L;
    
    @Column(name = "total_downloads")
    @Builder.Default
    private Long totalDownloads = 0L;
    
    @Column(name = "total_shares")
    @Builder.Default
    private Long totalShares = 0L;
    
    @Column(name = "total_prints")
    @Builder.Default
    private Long totalPrints = 0L;
    
    @Column(name = "total_completes")
    @Builder.Default
    private Long totalCompletes = 0L;
    
    @Column(name = "total_uses")
    @Builder.Default
    private Long totalUses = 0L;  // 전체 사용 횟수 (인기순 정렬용)
    
    // 평점 관련
    @Column(name = "rating_sum")
    @Builder.Default
    private Double ratingSum = 0.0;
    
    @Column(name = "rating_count")
    @Builder.Default
    private Integer ratingCount = 0;
    
    @Column(name = "average_rating")
    @Builder.Default
    private Double averageRating = 0.0;
    
    // 사용자 통계
    @Column(name = "unique_users")
    @Builder.Default
    private Integer uniqueUsers = 0;  // 고유 사용자 수
    
    @Column(name = "returning_users")
    @Builder.Default
    private Integer returningUsers = 0;  // 재방문 사용자 수
    
    // 메타 정보
    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;
    
    @Column(name = "last_updated")
    private LocalDateTime lastUpdated;
    
    @Column(name = "is_trending")
    @Builder.Default
    private Boolean isTrending = false;  // 급상승 여부
    
    @Column(name = "trend_score")
    @Builder.Default
    private Double trendScore = 0.0;  // 트렌드 점수
    
    // 통계 업데이트 메서드
    public void incrementView() {
        this.dailyViews++;
        this.weeklyViews++;
        this.monthlyViews++;
        this.totalViews++;
        this.totalUses++;
        this.weeklyUses++;
        this.monthlyUses++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void incrementDownload() {
        this.dailyDownloads++;
        this.weeklyDownloads++;
        this.monthlyDownloads++;
        this.totalDownloads++;
        this.totalUses++;
        this.weeklyUses++;
        this.monthlyUses++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void incrementShare() {
        this.dailyShares++;
        this.totalShares++;
        this.totalUses++;
        this.weeklyUses++;
        this.monthlyUses++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void incrementPrint() {
        this.dailyPrints++;
        this.totalPrints++;
        this.totalUses++;
        this.weeklyUses++;
        this.monthlyUses++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    public void incrementComplete() {
        this.dailyCompletes++;
        this.totalCompletes++;
        this.totalUses++;
        this.weeklyUses++;
        this.monthlyUses++;
        this.lastUsedAt = LocalDateTime.now();
        this.lastUpdated = LocalDateTime.now();
    }
    
    // 일별 초기화
    public void resetDailyStats() {
        this.dailyViews = 0;
        this.dailyDownloads = 0;
        this.dailyShares = 0;
        this.dailyPrints = 0;
        this.dailyCompletes = 0;
    }
    
    // 주간 통계 재계산 (배치로 처리)
    public void recalculateWeeklyStats() {
        // 최근 7일 데이터로 재계산
        this.weeklyViews = 0;
        this.weeklyDownloads = 0;
        this.weeklyUses = 0;
    }
    
    // 월간 통계 재계산 (배치로 처리)
    public void recalculateMonthlyStats() {
        // 최근 30일 데이터로 재계산
        this.monthlyViews = 0;
        this.monthlyDownloads = 0;
        this.monthlyUses = 0;
    }
    
    // 평점 업데이트
    public void addRating(Double rating) {
        this.ratingSum += rating;
        this.ratingCount++;
        this.averageRating = this.ratingSum / this.ratingCount;
        this.lastUpdated = LocalDateTime.now();
    }
    
    // 트렌드 점수 계산 (급상승 판단용)
    public void calculateTrendScore() {
        // 최근 사용량 증가율 기반 계산
        if (this.weeklyUses > 0 && this.monthlyUses > 0) {
            double weeklyAverage = this.weeklyUses / 7.0;
            double monthlyAverage = this.monthlyUses / 30.0;
            this.trendScore = (weeklyAverage / monthlyAverage) * 100;
            this.isTrending = this.trendScore > 150; // 150% 이상 증가시 트렌딩
        }
    }
}