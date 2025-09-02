package com.pullit.notification.entity;

import com.pullit.common.converter.JsonMapConverter;
import jakarta.persistence.*;
import lombok.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "exam_notifications")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification implements Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private String id;
    
    @Column(name = "user_id", nullable = false)
    private Long userId;
    
    @Column(name = "type", length = 50)
    private String type;
    
    @Column(name = "title", length = 200)
    private String title;
    
    @Column(name = "message", columnDefinition = "TEXT")
    private String message;
    
    @Column(name = "is_read", nullable = false)
    private boolean isRead;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "read_at")
    private LocalDateTime readAt;
    
    @Column(name = "data", columnDefinition = "JSON")
    @Convert(converter = JsonMapConverter.class)
    private Map<String, Object> data;
    
    @Column(name = "target_url", length = 500)
    private String targetUrl;
    
    @Column(name = "priority", length = 20)
    private String priority;
    
    @PrePersist
    public void prePersist() {
        this.createdAt = LocalDateTime.now();
        this.isRead = false;
    }
}