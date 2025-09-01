package com.pullit.notification.dto.request;

import lombok.*;
import java.util.Map;
import com.pullit.notification.enums.NotificationType;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateRequest {
    private Long userId;
    private NotificationType type;
    private String customTitle;
    private String customMessage;
    private Map<String, Object> data;
    private String targetUrl;
}