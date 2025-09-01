package com.pullit.notification.dto.response;

import lombok.*;
import java.time.LocalDateTime;
import java.util.Map;
import com.pullit.notification.entity.Notification;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private String id;
    private Long userId;
    private String type;
    private String title;
    private String message;
    private boolean isRead;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
    private Map<String, Object> data;
    private String targetUrl;
    private String priority;
    
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getId())
                .userId(notification.getUserId())
                .type(notification.getType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .isRead(notification.isRead())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .data(notification.getData())
                .targetUrl(notification.getTargetUrl())
                .priority(notification.getPriority())
                .build();
    }
}