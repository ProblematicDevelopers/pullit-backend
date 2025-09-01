package com.pullit.notification.controller;

import com.pullit.auth.authentication.CustomUserDetails;
import com.pullit.common.annotation.AuthUser;
import com.pullit.common.dto.response.ApiResponse;
import com.pullit.notification.dto.request.NotificationCreateRequest;
import com.pullit.notification.dto.response.NotificationResponse;
import com.pullit.notification.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "Notification", description = "알림 관리 API")
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    
    private final NotificationService notificationService;
    
    @Operation(summary = "알림 목록 조회", description = "현재 사용자의 알림 목록을 조회합니다")
    @GetMapping
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getNotifications(
            @AuthUser CustomUserDetails user,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        List<NotificationResponse> notifications = 
                notificationService.getUserNotifications(user.getUserId(), page, size);
        
        return ResponseEntity.ok(ApiResponse.success(notifications));
    }
    
    @Operation(summary = "읽지 않은 알림 개수 조회", description = "현재 사용자의 읽지 않은 알림 개수를 조회합니다")
    @GetMapping("/unread-count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> getUnreadCount(
            @AuthUser CustomUserDetails user) {
        
        long count = notificationService.getUnreadCount(user.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success(
                Map.of("count", count)
        ));
    }
    
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다")
    @PutMapping("/{notificationId}/read")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @AuthUser CustomUserDetails user,
            @PathVariable String notificationId) {
        
        notificationService.markAsRead(user.getUserId(), notificationId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @Operation(summary = "모든 알림 읽음 처리", description = "모든 알림을 읽음 처리합니다")
    @PutMapping("/read-all")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthUser CustomUserDetails user) {
        
        notificationService.markAllAsRead(user.getUserId());
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @Operation(summary = "알림 삭제", description = "특정 알림을 삭제합니다")
    @DeleteMapping("/{notificationId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotification(
            @AuthUser CustomUserDetails user,
            @PathVariable String notificationId) {
        
        notificationService.deleteNotification(user.getUserId(), notificationId);
        
        return ResponseEntity.ok(ApiResponse.success(null));
    }
    
    @Operation(summary = "테스트 알림 생성", description = "테스트용 알림을 생성합니다 (개발 환경에서만 사용)")
    @PostMapping("/test")
    public ResponseEntity<ApiResponse<NotificationResponse>> createTestNotification(
            @AuthUser CustomUserDetails user,
            @RequestBody NotificationCreateRequest request) {
        
        // 개발 환경에서만 허용
        if (request.getUserId() == null) {
            request.setUserId(user.getUserId());
        }
        
        NotificationResponse notification = notificationService.createNotification(request);
        
        return ResponseEntity.ok(ApiResponse.success(notification));
    }
}