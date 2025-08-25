package com.pullit.websocket.service;

import com.pullit.websocket.dto.request.ChatMessageRequest;
import com.pullit.websocket.dto.response.ChatMessageResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class ClassChatWebSocketService {

    private final AtomicLong messageIdCounter = new AtomicLong(1);

    public ChatMessageResponse processMessage(ChatMessageRequest request) {
        log.info("Processing chat message: {}", request);
        
        // 메시지 유효성 검증
        validateMessage(request);
        
        // 메시지 ID 생성
        Long messageId = messageIdCounter.getAndIncrement();
        
        // 현재 시간 설정
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        // 메시지 타입에 따른 처리
        switch (request.getMessageType()) {
            case "MESSAGE":
                return processChatMessage(request, messageId, timestamp);
            case "JOIN":
                return processJoinMessage(request, messageId, timestamp);
            case "LEAVE":
                return processLeaveMessage(request, messageId, timestamp);
            case "NOTICE":
                return processNoticeMessage(request, messageId, timestamp);
            default:
                throw new IllegalArgumentException("지원하지 않는 메시지 타입입니다: " + request.getMessageType());
        }
    }

    private void validateMessage(ChatMessageRequest request) {
        if (request.getContent() == null || request.getContent().trim().isEmpty()) {
            throw new IllegalArgumentException("메시지 내용이 비어있습니다.");
        }
        
        if (request.getClassId() == null) {
            throw new IllegalArgumentException("클래스 ID가 필요합니다.");
        }
        
        if (request.getSenderId() == null) {
            throw new IllegalArgumentException("발신자 ID가 필요합니다.");
        }
        
        if (request.getSenderName() == null || request.getSenderName().trim().isEmpty()) {
            throw new IllegalArgumentException("발신자 이름이 필요합니다.");
        }
        
        if (request.getSenderRole() == null || request.getSenderRole().trim().isEmpty()) {
            throw new IllegalArgumentException("발신자 역할이 필요합니다.");
        }
        
        // 메시지 길이 제한 (500자)
        if (request.getContent().length() > 500) {
            throw new IllegalArgumentException("메시지는 500자를 초과할 수 없습니다.");
        }
    }

    private ChatMessageResponse processChatMessage(ChatMessageRequest request, Long messageId, String timestamp) {
        // 부적절한 단어 필터링 (간단한 예시)
        String filteredContent = filterInappropriateWords(request.getContent());
        
        return ChatMessageResponse.builder()
                .messageId(messageId)
                .messageType("MESSAGE")
                .content(filteredContent)
                .classId(request.getClassId())
                .senderId(request.getSenderId())
                .senderName(request.getSenderName())
                .senderRole(request.getSenderRole())
                .timestamp(timestamp)
                .status("SUCCESS")
                .build();
    }

    private ChatMessageResponse processJoinMessage(ChatMessageRequest request, Long messageId, String timestamp) {
        return ChatMessageResponse.builder()
                .messageId(messageId)
                .messageType("JOIN")
                .content(request.getSenderName() + "님이 입장하셨습니다.")
                .classId(request.getClassId())
                .senderId(request.getSenderId())
                .senderName(request.getSenderName())
                .senderRole(request.getSenderRole())
                .timestamp(timestamp)
                .status("SUCCESS")
                .build();
    }

    private ChatMessageResponse processLeaveMessage(ChatMessageRequest request, Long messageId, String timestamp) {
        return ChatMessageResponse.builder()
                .messageId(messageId)
                .messageType("LEAVE")
                .content(request.getSenderName() + "님이 퇴장하셨습니다.")
                .classId(request.getClassId())
                .senderId(request.getSenderId())
                .senderName(request.getSenderName())
                .senderRole(request.getSenderRole())
                .timestamp(timestamp)
                .status("SUCCESS")
                .build();
    }

    private ChatMessageResponse processNoticeMessage(ChatMessageRequest request, Long messageId, String timestamp) {
        // 교사 권한 확인
        if (!"TEACHER".equals(request.getSenderRole())) {
            throw new IllegalArgumentException("교사만 공지사항을 보낼 수 있습니다.");
        }
        
        return ChatMessageResponse.builder()
                .messageId(messageId)
                .messageType("NOTICE")
                .content("[공지] " + request.getContent())
                .classId(request.getClassId())
                .senderId(request.getSenderId())
                .senderName(request.getSenderName())
                .senderRole(request.getSenderRole())
                .timestamp(timestamp)
                .status("SUCCESS")
                .build();
    }

    private String filterInappropriateWords(String content) {
        // 간단한 부적절한 단어 필터링 (실제로는 더 정교한 필터링 필요)
        String[] inappropriateWords = {"욕설", "비속어", "스팸"};
        
        String filteredContent = content;
        for (String word : inappropriateWords) {
            if (filteredContent.contains(word)) {
                filteredContent = filteredContent.replace(word, "***");
            }
        }
        
        return filteredContent;
    }

    // 클래스별 활성 사용자 수 관리 (간단한 메모리 기반 구현)
    public void addUserToClass(Long classId, Long userId) {
        log.info("User {} joined class {}", userId, classId);
        // TODO: Redis나 DB를 사용하여 실제 구현
    }

    public void removeUserFromClass(Long classId, Long userId) {
        log.info("User {} left class {}", userId, classId);
        // TODO: Redis나 DB를 사용하여 실제 구현
    }

    public int getActiveUsersInClass(Long classId) {
        // TODO: Redis나 DB에서 실제 사용자 수 조회
        return 0;
    }
}
