package com.pullit.notification.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum NotificationType {
    EXAM_ASSIGNED("시험 배정", "새로운 시험이 배정되었습니다", "HIGH"),
    EXAM_COMPLETED("시험 완료", "시험이 완료되었습니다", "MEDIUM"),
    EXAM_RESULT("시험 결과", "시험 결과가 발표되었습니다", "HIGH"),
    CLASS_INVITATION("반 초대", "새로운 반 초대가 있습니다", "HIGH"),
    CLASS_ANNOUNCEMENT("반 공지", "새로운 공지사항이 있습니다", "MEDIUM"),
    HOMEWORK_ASSIGNED("과제 배정", "새로운 과제가 배정되었습니다", "HIGH"),
    HOMEWORK_DEADLINE("과제 마감", "과제 마감이 임박했습니다", "HIGH"),
    GRADE_UPDATED("성적 업데이트", "성적이 업데이트되었습니다", "MEDIUM"),
    MESSAGE_RECEIVED("메시지 수신", "새로운 메시지가 있습니다", "LOW"),
    SYSTEM_NOTICE("시스템 공지", "시스템 공지사항입니다", "LOW"),
    ACHIEVEMENT_UNLOCKED("성취 달성", "새로운 성취를 달성했습니다", "LOW"),
    REMINDER("리마인더", "예약된 알림입니다", "MEDIUM");
    
    private final String title;
    private final String defaultMessage;
    private final String priority;
}