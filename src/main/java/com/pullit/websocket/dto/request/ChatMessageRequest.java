package com.pullit.websocket.dto.request;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageRequest {
    private String messageType; // MESSAGE, JOIN, LEAVE, NOTICE
    private String content;
    private Long classId; // 클래스 ID
    private Long senderId; // 발신자 ID
    private String senderName; // 발신자 이름
    private String senderRole; // TEACHER, STUDENT
    private String timestamp;
}
