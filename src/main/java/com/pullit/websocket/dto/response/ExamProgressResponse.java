package com.pullit.websocket.dto.response;

import lombok.*;

import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExamProgressResponse {
    private String channelName; // 채널 이름
    private Map<String, UserExamProgress> userProgresses; // 유저별 진행 상황
    private String timestamp; // 타임스탬프
    private String status; // SUCCESS, ERROR
    private String errorMessage;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserExamProgress {
        private Long userId; // 사용자 ID
        private Integer currentQuestion; // 현재 풀고 있는 문제 번호
        private String[] answeredQuestions; // 답변한 문제 목록
        private Map<String, Object> questionAnswers; // 문제별 답변 (문제번호: 답변)
        private Integer remainingTime; // 남은 시간 (초)
        private String timestamp; // 타임스탬프
    }
}
