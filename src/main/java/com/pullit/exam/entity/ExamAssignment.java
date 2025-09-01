package com.pullit.exam.entity;

import com.pullit.classes.entity.Classes;
import com.pullit.common.entity.FullAuditEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * 시험 출제 정보를 관리하는 엔티티
 * 특정 시험을 학급에 배정하고 시험 일정을 관리합니다.
 */
@Entity
@Table(name = "exam_assignments", 
    indexes = {
        @Index(name = "idx_exam_assignment_user_exam", columnList = "exam_id"),
        @Index(name = "idx_exam_assignment_class", columnList = "class_id"),
        @Index(name = "idx_exam_assignment_date", columnList = "exam_date"),
        @Index(name = "idx_exam_assignment_status", columnList = "status")
    })
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = {"exam", "classEntity"})
public class ExamAssignment extends FullAuditEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "assignment_id")
    private Long id;

    // Exam 대신 UserExam 사용
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private UserExam userExam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "class_id", nullable = false)
    private Classes classEntity;

    @Column(name = "exam_date", nullable = false)
    private LocalDate examDate;

    @Column(name = "exam_time", nullable = false)
    private LocalTime examTime;

    @Column(name = "time_limit", nullable = false)
    private Integer timeLimit; // 시험 시간 (분)

    @Column(name = "exam_start_datetime", nullable = false)
    private LocalDateTime examStartDateTime; // 실제 시험 시작 시간

    @Column(name = "exam_end_datetime", nullable = false)
    private LocalDateTime examEndDateTime; // 실제 시험 종료 시간

    @Column(name = "notification_sent")
    @Builder.Default
    private Boolean notificationSent = false;

    @Column(name = "notification_sent_at")
    private LocalDateTime notificationSentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20, nullable = false)
    @Builder.Default
    private ExamAssignmentStatus status = ExamAssignmentStatus.SCHEDULED;

    @Column(name = "max_attempts")
    @Builder.Default
    private Integer maxAttempts = 1; // 최대 시도 횟수

    @Column(name = "allow_review")
    @Builder.Default
    private Boolean allowReview = false; // 시험 후 리뷰 허용 여부

    @Column(name = "show_answer")
    @Builder.Default
    private Boolean showAnswer = false; // 정답 표시 여부

    @Column(name = "random_order")
    @Builder.Default
    private Boolean randomOrder = false; // 문제 순서 랜덤 여부

    @PrePersist
    @PreUpdate
    public void calculateExamDateTime() {
        if (examDate != null && examTime != null) {
            this.examStartDateTime = LocalDateTime.of(examDate, examTime);
            if (timeLimit != null) {
                this.examEndDateTime = examStartDateTime.plusMinutes(timeLimit);
            }
        }
    }

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return status == ExamAssignmentStatus.SCHEDULED &&
               examStartDateTime != null &&
               examEndDateTime != null &&
               now.isAfter(examStartDateTime) &&
               now.isBefore(examEndDateTime);
    }

    public boolean isUpcoming() {
        LocalDateTime now = LocalDateTime.now();
        return status == ExamAssignmentStatus.SCHEDULED &&
               examStartDateTime != null &&
               now.isBefore(examStartDateTime);
    }

    public boolean isCompleted() {
        return status == ExamAssignmentStatus.COMPLETED;
    }

    public void markNotificationSent() {
        this.notificationSent = true;
        this.notificationSentAt = LocalDateTime.now();
    }

    public void complete() {
        this.status = ExamAssignmentStatus.COMPLETED;
    }

    public void cancel() {
        this.status = ExamAssignmentStatus.CANCELLED;
    }

    public enum ExamAssignmentStatus {
        SCHEDULED,  // 예정됨
        IN_PROGRESS, // 진행 중
        COMPLETED,  // 완료됨
        CANCELLED   // 취소됨
    }
}