package com.pullit.exam.entity;

import com.pullit.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_usage_history")
@Getter
@Setter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class ExamUsageHistory extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "exam_id", nullable = false)
    private Long examId;
    
    @Column(name = "exam_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private ExamType examType;  // SYSTEM, USER
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "usage_type", nullable = false, length = 20)
    @Enumerated(EnumType.STRING)
    private UsageType usageType;
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 500)
    private String userAgent;
    
    @Column(name = "session_id", length = 100)
    private String sessionId;
    
    // 추가 컨텍스트 정보
    @Column(name = "school_id")
    private Long schoolId;
    
    @Column(name = "class_id")
    private Long classId;
    
    @Column(name = "grade_code", length = 10)
    private String gradeCode;  // 07, 08, 09
    
    @Column(name = "area_code", length = 10)
    private String areaCode;   // MA, KO, EN, SC, SO, HS, MO
    
    @Column(name = "duration_seconds")
    private Integer durationSeconds;  // 사용 시간 (초)
    
    @Column(name = "referrer", length = 500)
    private String referrer;  // 유입 경로
    
    public enum ExamType {
        SYSTEM("시스템 시험지"),
        USER("사용자 시험지");
        
        private final String description;
        
        ExamType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    public enum UsageType {
        VIEW("조회"),
        DOWNLOAD("다운로드"),
        PRINT("인쇄"),
        EDIT("수정"),
        SHARE("공유"),
        COMPLETE("완료"),
        COPY("복사"),
        EXPORT_PDF("PDF 내보내기"),
        EXPORT_ANSWER("정답지 내보내기");
        
        private final String description;
        
        UsageType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    // 편의 메서드
    public static ExamUsageHistory createViewHistory(Long examId, ExamType examType, Long userId) {
        return ExamUsageHistory.builder()
            .examId(examId)
            .examType(examType)
            .userId(userId)
            .usageType(UsageType.VIEW)
            .build();
    }
    
    public static ExamUsageHistory createDownloadHistory(Long examId, ExamType examType, Long userId) {
        return ExamUsageHistory.builder()
            .examId(examId)
            .examType(examType)
            .userId(userId)
            .usageType(UsageType.DOWNLOAD)
            .build();
    }
}