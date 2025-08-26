package com.pullit.websocket.dto.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageRequest {
    private String messageType; // MESSAGE, JOIN, LEAVE, NOTICE
    private String content;
    private String channelName; // 채널 이름 (예: "class-1", "exam-123", "general")
    private Long senderId;
    private String senderName; // 발신자 이름
    private String senderRole; // TEACHER, STUDENT
    private String timestamp;
}
