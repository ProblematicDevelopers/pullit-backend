package com.pullit.websocket.handler;

import com.pullit.websocket.dto.request.ChatMessageRequest;
import com.pullit.websocket.dto.request.OnlineStatusRequest;
import com.pullit.websocket.dto.response.ChatMessageResponse;
import com.pullit.websocket.dto.response.OnlineStatusResponse;
import com.pullit.websocket.service.ClassChatWebSocketService;
import com.pullit.websocket.service.OnlineStatusService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ClassChatWebSocketHandler {

    private final ClassChatWebSocketService chatService;
    private final OnlineStatusService onlineStatusService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat.sendMessage")
    public void sendMessage(@Payload ChatMessageRequest chatMessage) {
        log.info("Received chat message: {}", chatMessage);
        
        try {
            // 메시지 저장 및 처리
            ChatMessageResponse response = chatService.processMessage(chatMessage);
            
            // 채널 기반 메시지 브로드캐스트
            String topicDestination = getTopicDestination(chatMessage);
            messagingTemplate.convertAndSend(topicDestination, response);
            
        } catch (Exception e) {
            log.error("Error processing chat message", e);
            ChatMessageResponse errorResponse = ChatMessageResponse.builder()
                    .messageType("ERROR")
                    .status("ERROR")
                    .errorMessage("메시지 처리 중 오류가 발생했습니다.")
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
            
            String topicDestination = getTopicDestination(chatMessage);
            messagingTemplate.convertAndSend(topicDestination, errorResponse);
        }
    }

    @MessageMapping("/chat.addUser")
    public void addUser(@Payload ChatMessageRequest chatMessage, 
                       SimpMessageHeaderAccessor headerAccessor) {
        log.info("User joining chat: {}", chatMessage.getSenderName());
        
        // null 체크
        if (chatMessage.getSenderId() == null || chatMessage.getChannelName() == null || 
            chatMessage.getSenderName() == null || chatMessage.getSenderRole() == null) {
            log.error("Invalid chat message data: senderId={}, channelName={}, senderName={}, senderRole={}", 
                     chatMessage.getSenderId(), chatMessage.getChannelName(), 
                     chatMessage.getSenderName(), chatMessage.getSenderRole());
            return;
        }
        
        // WebSocket 세션에 사용자 정보 추가
        headerAccessor.getSessionAttributes().put("username", chatMessage.getSenderName());
        headerAccessor.getSessionAttributes().put("userId", chatMessage.getSenderId());
        headerAccessor.getSessionAttributes().put("channelName", chatMessage.getChannelName());
        
        // 접속 상태 업데이트
        OnlineStatusRequest onlineRequest = OnlineStatusRequest.builder()
                .channelName(chatMessage.getChannelName())
                .userId(chatMessage.getSenderId())
                .userName(chatMessage.getSenderName())
                .userRole(chatMessage.getSenderRole())
                .status("ONLINE")
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
        
        onlineStatusService.updateUserStatus(onlineRequest);
        
        // 입장 메시지 생성
        ChatMessageResponse joinMessage = ChatMessageResponse.builder()
                .messageType("JOIN")
                .content(chatMessage.getSenderName() + "님이 입장하셨습니다.")
                .channelName(chatMessage.getChannelName())
                .senderId(chatMessage.getSenderId())
                .senderName(chatMessage.getSenderName())
                .senderRole(chatMessage.getSenderRole())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .status("SUCCESS")
                .build();
        
        // 채널에 입장 메시지 브로드캐스트
        String topicDestination = getTopicDestination(chatMessage);
        messagingTemplate.convertAndSend(topicDestination, joinMessage);
        
        // 접속 상태 브로드캐스트
        broadcastOnlineStatus(chatMessage.getChannelName());
    }

    @MessageMapping("/chat.leaveUser")
    public void leaveUser(@Payload ChatMessageRequest chatMessage) {
        log.info("User leaving chat: {}", chatMessage.getSenderName());
        
        // null 체크
        if (chatMessage.getSenderId() == null || chatMessage.getChannelName() == null) {
            log.error("Invalid leave message data: senderId={}, channelName={}", 
                     chatMessage.getSenderId(), chatMessage.getChannelName());
            return;
        }
        
        // 접속 상태에서 제거
        onlineStatusService.removeUserFromClass(chatMessage.getChannelName(), chatMessage.getSenderId());
        
        // 퇴장 메시지 생성
        ChatMessageResponse leaveMessage = ChatMessageResponse.builder()
                .messageType("LEAVE")
                .content(chatMessage.getSenderName() + "님이 퇴장하셨습니다.")
                .channelName(chatMessage.getChannelName())
                .senderId(chatMessage.getSenderId())
                .senderName(chatMessage.getSenderName())
                .senderRole(chatMessage.getSenderRole())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .status("SUCCESS")
                .build();
        
        // 채널에 퇴장 메시지 브로드캐스트
        String topicDestination = getTopicDestination(chatMessage);
        messagingTemplate.convertAndSend(topicDestination, leaveMessage);
        
        // 접속 상태 브로드캐스트
        broadcastOnlineStatus(chatMessage.getChannelName());
    }

    @MessageMapping("/chat.sendNotice")
    public void sendNotice(@Payload ChatMessageRequest chatMessage) {
        log.info("Teacher sending notice: {}", chatMessage.getContent());
        
        // 교사만 공지사항을 보낼 수 있도록 검증
        if (!"TEACHER".equals(chatMessage.getSenderRole())) {
            ChatMessageResponse errorResponse = ChatMessageResponse.builder()
                    .messageType("ERROR")
                    .status("ERROR")
                    .errorMessage("교사만 공지사항을 보낼 수 있습니다.")
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .build();
            
            String topicDestination = getTopicDestination(chatMessage);
            messagingTemplate.convertAndSend(topicDestination, errorResponse);
            return;
        }
        
        // 공지사항 메시지 생성
        ChatMessageResponse noticeMessage = ChatMessageResponse.builder()
                .messageType("NOTICE")
                .content("[공지] " + chatMessage.getContent())
                .channelName(chatMessage.getChannelName())
                .senderId(chatMessage.getSenderId())
                .senderName(chatMessage.getSenderName())
                .senderRole(chatMessage.getSenderRole())
                .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .status("SUCCESS")
                .build();
        
        // 채널에 공지사항 브로드캐스트
        String topicDestination = getTopicDestination(chatMessage);
        messagingTemplate.convertAndSend(topicDestination, noticeMessage);
    }

    // 접속 상태 업데이트
    @MessageMapping("/online.updateStatus")
    public void updateOnlineStatus(@Payload OnlineStatusRequest request) {
        log.info("Updating online status: {}", request);
        
        // null 체크
        if (request == null || request.getChannelName() == null) {
            log.error("Invalid online status request: request={}, classId={}", 
                     request, request != null ? request.getChannelName() : null);
            return;
        }
        
        try {
            // 접속 상태 업데이트
            onlineStatusService.updateUserStatus(request);
            
            // 클래스 전체에 접속 상태 브로드캐스트
            broadcastOnlineStatus(request.getChannelName());
            
        } catch (Exception e) {
            log.error("Error updating online status", e);
        }
    }

    // 접속 상태 조회
    @MessageMapping("/online.getStatus")
    public void getOnlineStatus(@Payload OnlineStatusRequest request) {
        log.info("Getting online status for class: {}", request.getChannelName());
        
        // null 체크
        if (request == null || request.getChannelName() == null || request.getUserId() == null) {
            log.error("Invalid online status request: request={}, channelName={}, userId={}", 
                     request, request != null ? request.getChannelName() : null, 
                     request != null ? request.getUserId() : null);
            return;
        }
        
        try {
            String channelName = request.getChannelName();
            Long userId = request.getUserId();
            
            if (channelName == null || userId == null) {
                log.error("Null values detected in try block: channelName={}, userId={}", channelName, userId);
                return;
            }
            
            OnlineStatusResponse response = onlineStatusService.getClassOnlineStatus(channelName);
            
            // ✅ 채널 전체에 브로드캐스트 (모든 사용자가 동일한 정보를 받음)
            messagingTemplate.convertAndSend(
                "/topic/" + channelName + "/online",
                response
            );
            
        } catch (Exception e) {
            log.error("Error getting online status", e);
        }
    }

    // 채널 기반 토픽 목적지 생성 (내부 메서드)
    private String getTopicDestination(ChatMessageRequest chatMessage) {
        return "/topic/" + chatMessage.getChannelName();
    }

    // 접속 상태 브로드캐스트 (내부 메서드)
    private void broadcastOnlineStatus(String channelName) {
        // null 체크
        if (channelName == null) {
            log.error("ChannelName is null for broadcastOnlineStatus");
            return;
        }
        
        try {
            OnlineStatusResponse response = onlineStatusService.getClassOnlineStatus(channelName);
            messagingTemplate.convertAndSend("/topic/" + channelName + "/online", response);
        } catch (Exception e) {
            log.error("Error broadcasting online status", e);
        }
    }
}
