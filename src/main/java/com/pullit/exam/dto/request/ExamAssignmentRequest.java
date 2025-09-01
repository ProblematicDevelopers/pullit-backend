package com.pullit.exam.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * 시험 출제 요청 DTO
 * 여러 학급에 시험을 동시에 배정하기 위한 정보를 담습니다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class ExamAssignmentRequest {

    @NotNull(message = "시험 ID는 필수입니다.")
    @Positive(message = "시험 ID는 양수여야 합니다.")
    private Long examId;

    @NotEmpty(message = "최소 하나 이상의 학급을 선택해야 합니다.")
    @Size(min = 1, max = 50, message = "학급은 1개 이상 50개 이하로 선택할 수 있습니다.")
    private List<@NotNull @Positive Long> classIds;

    @NotNull(message = "시험 날짜는 필수입니다.")
    @FutureOrPresent(message = "시험 날짜는 오늘 이후여야 합니다.")
    private LocalDate examDate;

    @NotNull(message = "시험 시간은 필수입니다.")
    private LocalTime examTime;

    @NotNull(message = "제한 시간은 필수입니다.")
    @Min(value = 10, message = "제한 시간은 최소 10분 이상이어야 합니다.")
    @Max(value = 300, message = "제한 시간은 최대 300분(5시간)까지 설정할 수 있습니다.")
    private Integer timeLimit; // 분 단위

    @NotNull(message = "알림 발송 여부는 필수입니다.")
    @Builder.Default
    private Boolean sendNotification = true;

    // 추가 옵션들
    @Min(value = 1, message = "최대 시도 횟수는 1 이상이어야 합니다.")
    @Max(value = 5, message = "최대 시도 횟수는 5회까지 설정할 수 있습니다.")
    @Builder.Default
    private Integer maxAttempts = 1;

    @Builder.Default
    private Boolean allowReview = false; // 시험 후 리뷰 허용 여부

    @Builder.Default
    private Boolean showAnswer = false; // 정답 표시 여부

    @Builder.Default
    private Boolean randomOrder = false; // 문제 순서 랜덤 여부

    // 알림 관련 추가 옵션
    @Min(value = 0, message = "알림 전송 시간은 0분 이상이어야 합니다.")
    @Max(value = 10080, message = "알림 전송 시간은 최대 1주일(10080분) 전까지 설정할 수 있습니다.")
    @Builder.Default
    private Integer notificationMinutesBefore = 60; // 시험 몇 분 전에 알림을 보낼지 (기본값: 60분)

    private String notificationMessage; // 커스텀 알림 메시지 (선택사항)

    /**
     * 요청 데이터 유효성 검증
     */
    public boolean isValid() {
        if (examDate == null || examTime == null) {
            return false;
        }
        
        // 시험 시작 시간이 현재 시간 이후인지 확인
        LocalDate today = LocalDate.now();
        LocalTime now = LocalTime.now();
        
        if (examDate.isBefore(today)) {
            return false;
        }
        
        if (examDate.isEqual(today) && examTime.isBefore(now)) {
            return false;
        }
        
        return true;
    }

    /**
     * 알림 발송 시간 계산
     */
    public LocalDate getNotificationDate() {
        if (examDate == null || examTime == null || notificationMinutesBefore == null) {
            return null;
        }
        
        // 알림 시간 계산
        LocalTime notificationTime = examTime.minusMinutes(notificationMinutesBefore);
        LocalDate notificationDate = examDate;
        
        // 시간이 전날로 넘어가는 경우 처리
        if (notificationTime.isAfter(examTime)) {
            notificationDate = examDate.minusDays(1);
        }
        
        return notificationDate;
    }

    public LocalTime getNotificationTime() {
        if (examTime == null || notificationMinutesBefore == null) {
            return null;
        }
        
        return examTime.minusMinutes(notificationMinutesBefore);
    }
}