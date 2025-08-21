package com.pullit.cbt.entity;

import com.pullit.cbt.dto.response.AttemptExamResponse;
import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.exam.entity.UserExam;
import com.pullit.user.dto.response.UserResponse;
import com.pullit.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(name = "exam_attempt")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = { "user", "exam", "attemptQuestions" })
public class AttemptExam extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "exam_id", nullable = false)
    private UserExam exam;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "remain_time")
    private Integer remainTime; // 남은 시간 (초 단위)

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AttemptStatus status = AttemptStatus.IN_PROGRESS;

    @OneToMany(mappedBy = "attemptExam", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AttemptExamQuestion> attemptQuestions = new ArrayList<>();

    // 시험 시작
    public void start() {
        this.startedAt = LocalDateTime.now();
        this.status = AttemptStatus.IN_PROGRESS;
        // 시험 시작 시 남은 시간을 시험 제한 시간으로 초기화 (분을 초로 변환)
        if (this.exam != null && this.exam.getTimeLimit() != null) {
            this.remainTime = this.exam.getTimeLimit() * 60; // 분을 초로 변환
        }
    }

    // 시험 완료
    public void complete() {
        this.completedAt = LocalDateTime.now();
        this.status = AttemptStatus.DONE;
    }

    // 응시 시간 계산 (분 단위)
    public Long getDurationMinutes() {
        if (startedAt == null) {
            return 0L;
        }
        LocalDateTime endTime = completedAt != null ? completedAt : LocalDateTime.now();
        return java.time.Duration.between(startedAt, endTime).toMinutes();
    }

    // 남은 시간 업데이트 (초 단위)
    public void updateRemainTime(int elapsedSeconds) {
        if (this.remainTime != null && this.remainTime > 0) {
            this.remainTime = Math.max(0, this.remainTime - elapsedSeconds);
        }
    }

    // 남은 시간 설정
    public void setRemainTime(Integer remainTime) {
        this.remainTime = remainTime;
    }

    // 시간 초과 여부 확인
    public boolean isTimeExpired() {
        return this.remainTime != null && this.remainTime <= 0;
    }

    // 남은 시간을 시:분:초 형식으로 반환
    public String getRemainTimeFormatted() {
        if (this.remainTime == null || this.remainTime <= 0) {
            return "00:00:00";
        }
        int hours = this.remainTime / 3600;
        int minutes = (this.remainTime % 3600) / 60;
        int seconds = this.remainTime % 60;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    // 남은 시간을 분:초 형식으로 반환 (시험용)
    public String getRemainTimeFormattedForExam() {
        if (this.remainTime == null || this.remainTime <= 0) {
            return "00:00";
        }
        int minutes = this.remainTime / 60;
        int seconds = this.remainTime % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // 문제 추가
    public void addAttemptQuestion(AttemptExamQuestion question) {
        attemptQuestions.add(question);
        question.setAttemptExam(this);
    }

    // 정답 개수 계산
    public long getCorrectAnswerCount() {
        return attemptQuestions.stream()
                .filter(AttemptExamQuestion::getIsCorrect)
                .count();
    }

    // 총점 계산
    public int getTotalScore() {
        return attemptQuestions.stream()
                .filter(AttemptExamQuestion::getIsCorrect)
                .mapToInt(question -> question.getPoints() != null ? question.getPoints() : 0)
                .sum();
    }

    // 진행률 계산
    public double getProgressPercentage() {
        if (exam == null || exam.getTotalItems() == 0) {
            return 0.0;
        }
        return (double) attemptQuestions.size() / exam.getTotalItems() * 100;
    }

    public enum AttemptStatus {
        IN_PROGRESS, DONE
    }

    // single converter
    public static AttemptExamResponse convertToResponseExclude(AttemptExam attemptExam) {
        return AttemptExamResponse.builder()
                .id(attemptExam.getId())
                .user(UserResponse.builder()
                        .id(attemptExam.getUser().getId())
                        .username(attemptExam.getUser().getUsername())
                        .email(attemptExam.getUser().getEmail())
                        .fullName(attemptExam.getUser().getFullName())
                        .role(attemptExam.getUser().getRole())
                        .build())
                .examId(attemptExam.getExam().getId())
                .examName(attemptExam.getExam().getExamName())
                .startedAt(attemptExam.getStartedAt())
                .completedAt(attemptExam.getCompletedAt())
                .status(attemptExam.getStatus())
                .build();
    }

    // 상세 정보 포함 converter
    public static AttemptExamResponse convertToResponseWithQuestions(AttemptExam attemptExam) {
        return AttemptExamResponse.builder()
                .id(attemptExam.getId())
                .user(UserResponse.builder()
                        .id(attemptExam.getUser().getId())
                        .username(attemptExam.getUser().getUsername())
                        .email(attemptExam.getUser().getEmail())
                        .fullName(attemptExam.getUser().getFullName())
                        .role(attemptExam.getUser().getRole())
                        .build())
                .examId(attemptExam.getExam().getId())
                .examName(attemptExam.getExam().getExamName())
                .startedAt(attemptExam.getStartedAt())
                .completedAt(attemptExam.getCompletedAt())
                .status(attemptExam.getStatus())
                .attemptQuestions(attemptExam.getAttemptQuestions().stream()
                        .map(q -> q.convertToResponse(q))
                        .collect(Collectors.toList()))
                .build();
    }

}
