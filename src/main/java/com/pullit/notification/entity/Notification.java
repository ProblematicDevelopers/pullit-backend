package com.pullit.notification.entity;

import jakarta.persistence.PrePersist;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;
    
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
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
}