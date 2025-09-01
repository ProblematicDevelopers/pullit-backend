package com.pullit.schedule.entity;

import com.pullit.common.entity.FullAuditEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedules")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@SQLRestriction("deleted_date IS NULL")
public class Schedule extends FullAuditEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;
    
    @Column(name = "title", nullable = false, length = 200)
    private String title;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "type", length = 50)
    private String type; // "exam", "meeting", "deadline", "class", "event"
    
    @Column(name = "scheduled_date", nullable = false)
    private LocalDateTime scheduledDate;
    
    @Column(name = "end_date")
    private LocalDateTime endDate;
    
    @Column(name = "location", length = 200)
    private String location;
    
    @Column(name = "participants")
    private Integer participants;
    
    @Column(name = "status", length = 20)
    @Builder.Default
    private String status = "upcoming"; // "upcoming", "ongoing", "completed", "cancelled"
    
    @Column(name = "class_id")
    private Long classId;
    
    @Column(name = "exam_id")
    private Long examId; // 시험 일정인 경우
    
    @Column(name = "is_recurring")
    @Builder.Default
    private Boolean isRecurring = false;
    
    @Column(name = "recurrence_pattern", length = 50)
    private String recurrencePattern; // "daily", "weekly", "monthly"
    
    @Column(name = "reminder_enabled")
    @Builder.Default
    private Boolean reminderEnabled = false;
    
    @Column(name = "reminder_minutes")
    private Integer reminderMinutes; // 몇 분 전 알림
    
    @Column(name = "color", length = 7)
    @Builder.Default
    private String color = "#2563eb"; // 캘린더 표시 색상
    
    @Column(name = "deleted_date")
    private LocalDateTime deletedDate;
    
    @Column(name = "deleted_by")
    private Long deletedBy;
    
    // Soft Delete 메서드
    public void softDelete(Long deleterId) {
        this.deletedDate = LocalDateTime.now();
        this.deletedBy = deleterId;
    }
    
    public void restore() {
        this.deletedDate = null;
        this.deletedBy = null;
    }
    
    // 상태 변경 메서드
    public void updateStatus(String status) {
        this.status = status;
    }
    
    public void complete() {
        this.status = "completed";
    }
    
    public void cancel() {
        this.status = "cancelled";
    }
}