package com.pullit.cbt.entity;

import com.pullit.cbt.dto.response.AttemptExamQuestionResponse;
import com.pullit.common.entity.BaseTimeEntity;
import com.pullit.exam.entity.UserExamItem;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "exam_attempt_question")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@ToString(exclude = { "attemptExam", "examItem" })
public class AttemptExamQuestion extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_question_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attempt_id", nullable = false)
    private AttemptExam attemptExam;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private UserExamItem examItem;

    @Column(name = "user_answer", columnDefinition = "TEXT")
    private String userAnswer;

    @Column(name = "is_correct", nullable = false)
    @Builder.Default
    private Boolean isCorrect = false;

    @Column(name = "duration", nullable = false)
    @Builder.Default
    private Integer duration = 0; // 문제별 소요 시간 (초)

    @Column(name = "points")
    private Integer points; // 배점 (시험 생성 시 설정된 배점)

    @Column(name = "answered_at")
    private java.time.LocalDateTime answeredAt;

    // 답변 제출
    public void submitAnswer(String answer, boolean isCorrect, int durationSeconds) {
        this.userAnswer = answer;
        this.isCorrect = isCorrect;
        this.duration = durationSeconds;
        this.answeredAt = java.time.LocalDateTime.now();
    }

    // 답변 수정
    public void updateAnswer(String newAnswer, boolean isCorrect, int additionalDuration) {
        this.userAnswer = newAnswer;
        this.isCorrect = isCorrect;
        this.duration += additionalDuration;
        this.answeredAt = java.time.LocalDateTime.now();
    }

    // 배점 설정
    public void setPoints(Integer points) {
        this.points = points;
    }

    // 점수 계산
    public int getScore() {
        if (isCorrect && points != null) {
            return points;
        }
        return 0;
    }

    // 응답 여부 확인
    public boolean isAnswered() {
        return userAnswer != null && !userAnswer.trim().isEmpty();
    }

    // 소요 시간을 분:초 형식으로 반환
    public String getDurationFormatted() {
        int minutes = duration / 60;
        int seconds = duration % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }

    // single converter
    public AttemptExamQuestionResponse convertToResponse(AttemptExamQuestion attemptExamQuestion) {
        if (attemptExamQuestion == null) {
            return null;
        }

        return AttemptExamQuestionResponse.builder()
                .id(attemptExamQuestion.getId())
                .questionId(attemptExamQuestion.getExamItem().getId())
                .userAnswer(attemptExamQuestion.getUserAnswer())
                .isCorrect(attemptExamQuestion.getIsCorrect())
                .duration(attemptExamQuestion.getDuration())
                .points(attemptExamQuestion.getPoints())
                .answeredAt(attemptExamQuestion.getAnsweredAt())
                .build();
    }

}
