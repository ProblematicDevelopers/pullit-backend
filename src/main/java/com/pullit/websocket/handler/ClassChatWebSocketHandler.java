package com.pullit.websocket.handler;

import com.pullit.websocket.dto.request.ChatMessageRequest;
import com.pullit.websocket.dto.request.ExamProgressRequest;
import com.pullit.websocket.dto.request.OnlineStatusRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pullit.websocket.dto.response.ChatMessageResponse;
import com.pullit.websocket.dto.response.ExamProgressResponse;
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
import java.util.List;
import java.util.Map;

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

    // 상태 메시지 전송 (클라이언트의 sendStatus에 대응)
    @MessageMapping("/exam.sendStatus")
    public void sendStatus(@Payload ChatMessageRequest chatMessage) {
        log.info("Sending status message: {}", chatMessage);

        // null 체크
        if (chatMessage == null || chatMessage.getChannelName() == null || chatMessage.getContent() == null) {
            log.error("Invalid status request: request={}, channelName={}, content={}",
                    chatMessage, chatMessage != null ? chatMessage.getChannelName() : null,
                    chatMessage != null ? chatMessage.getContent() : null);
            return;
        }

        try {
            // content가 "GET_EXAM_STATUS"인 경우 시험 상태 조회 및 응답
            if ("GET_EXAM_STATUS".equals(chatMessage.getContent())) {
                // 시험 상태 조회
                String examStatus = onlineStatusService.getExamStatus(chatMessage.getChannelName());

                // 시험 상태 응답 생성
                ChatMessageResponse statusResponse = ChatMessageResponse.builder()
                        .messageType("EXAM_STATUS")
                        .content(examStatus != null ? examStatus : "NO_STATUS")
                        .channelName(chatMessage.getChannelName())
                        .senderId(chatMessage.getSenderId())
                        .senderName(chatMessage.getSenderName())
                        .senderRole(chatMessage.getSenderRole())
                        .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                        .status("SUCCESS")
                        .build();

                // 시험 상태를 채널에 브로드캐스트
                messagingTemplate.convertAndSend(
                        "/topic/" + chatMessage.getChannelName() + "/exam-status",
                        statusResponse);

                log.info("Exam status requested and sent for channel: {}", chatMessage.getChannelName());
            } else {
                // 일반 상태 메시지인 경우 시험 상태 업데이트
                broadcastExamStatus(chatMessage.getChannelName(), chatMessage.getContent());
            }

            log.info("Status message sent for channel: {}", chatMessage.getChannelName());

        } catch (Exception e) {
            log.error("Error sending status message", e);
        }
    }

    @MessageMapping("/exam.getStatus")
    public void getExamStatus(@Payload ChatMessageRequest examStatusRequest) {
        log.info("Getting exam status: {}", examStatusRequest);

        try {
            if (examStatusRequest == null || examStatusRequest.getChannelName() == null) {
                log.error("Invalid exam status request: request={}, channelName={}",
                        examStatusRequest, examStatusRequest != null ? examStatusRequest.getChannelName() : null);
                return;
            }

            String channelName = examStatusRequest.getChannelName();

            if (channelName == null) {
                log.error("Null values detected in try block: channelName={}", channelName);
                return;
            }

            String examStatus = onlineStatusService.getExamStatus(channelName);
            log.info("Exam status for channel {}: {}", channelName, examStatus);

            // 시험 상태 응답 생성
            ChatMessageResponse statusResponse = ChatMessageResponse.builder()
                    .messageType("EXAM_STATUS")
                    .content(examStatus != null ? examStatus : "NO_STATUS")
                    .channelName(channelName)
                    .senderId(examStatusRequest.getSenderId())
                    .senderName(examStatusRequest.getSenderName())
                    .senderRole(examStatusRequest.getSenderRole())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("SUCCESS")
                    .build();

            // ✅ 시험 상태를 채널에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/" + channelName + "/exam-status",
                    statusResponse);

        } catch (Exception e) {
            log.error("Error getting exam status", e);
        }
    }

    @MessageMapping("/exam.sendProgress")
    public void sendProgress(@Payload Map<String, Object> request) {
        log.info("Sending progress: {}", request);

        // null 체크
        if (request == null || request.get("channelName") == null || request.get("content") == null) {
            log.error("Invalid progress request: request={}, channelName={}, content={}",
                    request, request != null ? request.get("channelName") : null,
                    request != null ? request.get("content") : null);
            return;
        }

        try {
            String channelName = (String) request.get("channelName");
            String contentStr = (String) request.get("content");

            // content 문자열을 JSON으로 파싱
            ObjectMapper objectMapper = new ObjectMapper();
            Map<String, Object> content = objectMapper.readValue(contentStr, Map.class);

            // answeredQuestions 안전하게 변환
            String[] answeredQuestions = new String[0];
            if (content.get("answeredQuestions") != null) {
                List<?> answeredList = (List<?>) content.get("answeredQuestions");
                answeredQuestions = answeredList.stream()
                        .map(Object::toString)
                        .toArray(String[]::new);
            }

            // ExamProgressRequest로 변환
            ExamProgressRequest examProgressRequest = ExamProgressRequest.builder()
                    .channelName(channelName)
                    .userId(Long.valueOf(content.get("userId").toString()))
                    .currentQuestion((Integer) content.get("currentQuestion"))
                    .answeredQuestions(answeredQuestions)
                    .questionAnswers((Map<String, Object>) content.get("questionAnswers"))
                    .remainingTime((Integer) content.get("remainingTime"))
                    .timestamp((String) content.get("timestamp"))
                    .build();

            // 시험 진행 상황 업데이트
            onlineStatusService.updateExamProgress(examProgressRequest);

            // 시험 진행 상황 브로드캐스트
            broadcastExamProgress(channelName);

            log.info("Exam progress updated and broadcasted for channel: {}", channelName);

        } catch (Exception e) {
            log.error("Error sending exam progress", e);
        }
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
                    response);

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

    private void broadcastExamProgress(String channelName) {
        if (channelName == null) {
            log.error("ChannelName is null for broadcastExamProgress");
            return;
        }

        try {
            // 시험 진행 상황 조회
            ExamProgressResponse examProgress = onlineStatusService.getExamProgress(channelName);

            // 시험 진행 상황을 채널에 브로드캐스트
            messagingTemplate.convertAndSend(
                    "/topic/" + channelName + "/exam-progress",
                    examProgress);

            log.info("Exam progress broadcasted for channel: {}", channelName);

        } catch (Exception e) {
            log.error("Error broadcasting exam progress", e);
        }
    }

    private void broadcastExamStatus(String channelName, String examStatus) {
        if (channelName == null) {
            log.error("ChannelName is null for broadcastExamStatus");
            return;
        }

        try {
            // Redis에 시험 상태 저장
            onlineStatusService.updateExamStatus(channelName, examStatus);

            // 시험 상태 응답 생성
            ChatMessageResponse statusResponse = ChatMessageResponse.builder()
                    .messageType("EXAM_STATUS")
                    .content(examStatus)
                    .channelName(channelName)
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("SUCCESS")
                    .build();

            // 시험 상태를 채널에 브로드캐스트
            messagingTemplate.convertAndSend("/topic/" + channelName + "/exam-status", statusResponse);

            log.info("Exam status broadcasted: channel={}, status={}", channelName, examStatus);
        } catch (Exception e) {
            log.error("Error broadcasting exam status", e);
        }
    }
}
