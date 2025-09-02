package com.pullit.exam.entity;

import com.pullit.classes.entity.Classes;
import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "teacher_live_exams")
@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TeacherLiveExam extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "exam_name", nullable = false, length = 500)
    private String examName;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classes examClass;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "teacher_id", nullable = false)
    private User teacher;
    
    @Column(name = "exam_type", length = 50)
    @Builder.Default
    private String examType = "LIVE_EXAM";
    
    @Enumerated(EnumType.STRING)
    @Column(name = "exam_status", length = 20)
    @Builder.Default
    private ExamStatus examStatus = ExamStatus.CREATED;
    
    @Column(name = "total_items")
    @Builder.Default
    private Integer totalItems = 0;
    
    @Column(name = "total_points")
    @Builder.Default
    private Integer totalPoints = 100;
    
    @Column(name = "time_limit")
    private Integer timeLimit; // 분 단위
    
    @Column(name = "scheduled_date")
    private LocalDate scheduledDate;
    
    @Column(name = "scheduled_time")
    private LocalTime scheduledTime;
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;
    
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Column(name = "grade_code", length = 10)
    private String gradeCode;
    
    @Column(name = "term_code", length = 10)
    private String termCode;
    
    @Column(name = "subject_code", length = 20)
    private String subjectCode;
    
    @OneToMany(mappedBy = "liveExam", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("itemOrder ASC")
    @Builder.Default
    private List<TeacherLiveExamItem> examItems = new ArrayList<>();
    
    // 시험 상태 enum
    public enum ExamStatus {
        CREATED,    // 생성됨
        STARTED,    // 시작됨
        ENDED,      // 종료됨
        CANCELLED   // 취소됨
    }
    
    // 비즈니스 메서드
    public void startExam() {
        this.examStatus = ExamStatus.STARTED;
        this.startedAt = LocalDateTime.now();
    }
    
    public void endExam() {
        this.examStatus = ExamStatus.ENDED;
        this.endedAt = LocalDateTime.now();
    }
    
    public void cancelExam() {
        this.examStatus = ExamStatus.CANCELLED;
    }
    
    public boolean isActive() {
        return this.examStatus == ExamStatus.STARTED;
    }
    
    public boolean canStart() {
        return this.examStatus == ExamStatus.CREATED;
    }
    
    public boolean canTake() {
        return this.examStatus == ExamStatus.STARTED || this.examStatus == ExamStatus.CREATED;
    }
    
    public void addExamItem(TeacherLiveExamItem item) {
        examItems.add(item);
        item.setLiveExam(this);
        this.totalItems = examItems.size();
        recalculateTotalPoints();
    }
    
    public void removeExamItem(TeacherLiveExamItem item) {
        examItems.remove(item);
        item.setLiveExam(null);
        this.totalItems = examItems.size();
        recalculateTotalPoints();
    }
    
    private void recalculateTotalPoints() {
        this.totalPoints = examItems.stream()
                .mapToInt(TeacherLiveExamItem::getPoints)
                .sum();
    }
    
    public LocalDateTime getScheduledDateTime() {
        if (scheduledDate != null && scheduledTime != null) {
            return LocalDateTime.of(scheduledDate, scheduledTime);
        }
        return null;
    }
}