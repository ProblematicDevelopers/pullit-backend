package com.pullit.notification.websocket;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class NotificationWebSocketConfig implements WebSocketConfigurer {
    
    private final NotificationWebSocketHandler notificationWebSocketHandler;
    
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
                .setAllowedOrigins(
                    "http://localhost:5173",
                    "http://localhost:5174", 
                    "http://localhost:3000"
                );
    }
}