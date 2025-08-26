package com.pullit.websocket.dto.response;

import lombok.*;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessageResponse {
    private Long messageId;
    private String messageType; // MESSAGE, JOIN, LEAVE, NOTICE
    private String content;
    private String channelName; // 채널 이름
    private Long senderId;
    private String senderName;
    private String senderRole; // TEACHER, STUDENT
    private String timestamp;
    private String status; // SUCCESS, ERROR
    private String errorMessage;
}
