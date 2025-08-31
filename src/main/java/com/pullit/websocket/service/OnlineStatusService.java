package com.pullit.websocket.service;

import com.pullit.websocket.dto.request.ExamProgressRequest;
import com.pullit.websocket.dto.request.OnlineStatusRequest;
import com.pullit.websocket.dto.response.ExamProgressResponse;
import com.pullit.websocket.dto.response.OnlineStatusResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class OnlineStatusService {

    private final RedisTemplate<String, Object> redisTemplate;

    // Redis 키 패턴
    private static final String CHANNEL_USERS_KEY_PATTERN = "online:channel:%s:users";
    private static final String USER_INFO_KEY_PATTERN = "online:user:%s:info";
    private static final String CHANNEL_LIST_KEY = "online:channels";

    // 시험 상태 관련 키 패턴
    private static final String EXAM_STATUS_KEY_PATTERN = "exam:status:%s"; // 채널별 시험 상태
    private static final String EXAM_PROGRESS_KEY_PATTERN = "exam:progress:%s"; // 채널별 시험 진행 상황

    // TTL 설정 (초)
    private static final long USER_TTL = 300; // 5분
    private static final long CHANNEL_TTL = 3600; // 1시간
    private static final long EXAM_STATUS_TTL = 7200; // 2시간 (시험 상태는 더 오래 유지)

    public void updateUserStatus(OnlineStatusRequest request) {

        // null 체크
        if (request == null || request.getChannelName() == null || request.getUserId() == null) {
            log.error("Invalid request data: request={}, channelName={}, userId={}",
                    request, request != null ? request.getChannelName() : null,
                    request != null ? request.getUserId() : null);
            return;
        }

        String channelName = request.getChannelName();
        Long userId = request.getUserId();

        try {
            // 사용자 정보 생성
            UserOnlineInfo userInfo = UserOnlineInfo.builder()
                    .userId(userId)
                    .userName(request.getUserName())
                    .userRole(request.getUserRole())
                    .status(request.getStatus())
                    .lastSeen(LocalDateTime.now())
                    .build();

            // Redis에 사용자 정보 저장
            String userInfoKey = String.format(USER_INFO_KEY_PATTERN, userId);
            redisTemplate.opsForValue().set(userInfoKey, userInfo, USER_TTL, TimeUnit.SECONDS);

            // 채널에 사용자 추가
            String channelUsersKey = String.format(CHANNEL_USERS_KEY_PATTERN, channelName);
            redisTemplate.opsForSet().add(channelUsersKey, userId.toString());
            redisTemplate.expire(channelUsersKey, CHANNEL_TTL, TimeUnit.SECONDS);

            // 채널 목록에 추가
            redisTemplate.opsForSet().add(CHANNEL_LIST_KEY, channelName);
            redisTemplate.expire(CHANNEL_LIST_KEY, CHANNEL_TTL, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Failed to update user status in Redis: userId={}, channelName={}", userId, channelName, e);
        }
    }

    public void removeUserFromClass(String channelName, Long userId) {

        // null 체크
        if (channelName == null || userId == null) {
            log.error("Invalid parameters for removeUserFromClass: channelName={}, userId={}", channelName, userId);
            return;
        }

        try {
            // 채널에서 사용자 제거
            String channelUsersKey = String.format(CHANNEL_USERS_KEY_PATTERN, channelName);
            redisTemplate.opsForSet().remove(channelUsersKey, userId.toString());

            // 사용자 정보 삭제
            String userInfoKey = String.format(USER_INFO_KEY_PATTERN, userId);
            redisTemplate.delete(userInfoKey);

            // 채널이 비어있으면 채널 목록에서도 제거
            if (getOnlineUserCount(channelName) == 0) {
                redisTemplate.opsForSet().remove(CHANNEL_LIST_KEY, channelName);
                redisTemplate.delete(channelUsersKey);
            }

        } catch (Exception e) {
            log.error("Failed to remove user from Redis: userId={}, channelName={}", userId, channelName, e);
        }
    }

    public OnlineStatusResponse getClassOnlineStatus(String channelName) {

        // null 체크
        if (channelName == null) {
            log.error("ChannelName is null for getClassOnlineStatus");
            return OnlineStatusResponse.builder()
                    .channelName(null)
                    .onlineUsers(List.of())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("ERROR")
                    .errorMessage("ChannelName is null")
                    .build();
        }

        try {
            String channelUsersKey = String.format(CHANNEL_USERS_KEY_PATTERN, channelName);
            Set<Object> userIds = redisTemplate.opsForSet().members(channelUsersKey);

            List<OnlineStatusResponse.UserOnlineStatus> onlineUsers = userIds.stream()
                    .map(userId -> {
                        try {
                            String userInfoKey = String.format(USER_INFO_KEY_PATTERN, userId);
                            UserOnlineInfo userInfo = (UserOnlineInfo) redisTemplate.opsForValue().get(userInfoKey);

                            if (userInfo != null) {
                                return convertToUserOnlineStatus(userInfo);
                            }
                        } catch (Exception e) {
                            log.error("Failed to get user info for userId: {}", userId, e);
                        }
                        return null;
                    })
                    .filter(userStatus -> userStatus != null)
                    .collect(Collectors.toList());

            return OnlineStatusResponse.builder()
                    .channelName(channelName)
                    .onlineUsers(onlineUsers)
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("SUCCESS")
                    .build();

        } catch (Exception e) {
            log.error("Failed to get online status from Redis: channelName={}", channelName, e);
            return OnlineStatusResponse.builder()
                    .channelName(channelName)
                    .onlineUsers(List.of())
                    .timestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("ERROR")
                    .errorMessage("Failed to get online status from Redis")
                    .build();
        }
    }

    public int getOnlineUserCount(String channelName) {
        // null 체크
        if (channelName == null) {
            log.error("ChannelName is null for getOnlineUserCount");
            return 0;
        }

        try {
            String channelUsersKey = String.format(CHANNEL_USERS_KEY_PATTERN, channelName);
            Long size = redisTemplate.opsForSet().size(channelUsersKey);
            return size != null ? size.intValue() : 0;
        } catch (Exception e) {
            log.error("Failed to get online user count from Redis: channelName={}", channelName, e);
            return 0;
        }
    }

    /**
     * 모든 채널의 온라인 사용자 수 조회
     */
    public Map<String, Integer> getAllChannelUserCounts() {
        try {
            Set<Object> channels = redisTemplate.opsForSet().members(CHANNEL_LIST_KEY);
            if (channels == null) {
                return Map.of();
            }

            return channels.stream()
                    .map(channel -> channel.toString())
                    .collect(Collectors.toMap(
                            channel -> channel,
                            this::getOnlineUserCount));
        } catch (Exception e) {
            log.error("Failed to get all channel user counts from Redis", e);
            return Map.of();
        }
    }

    /**
     * 만료된 사용자 정리 (정기적으로 호출)
     */
    public void cleanupExpiredUsers() {
        try {
            Set<Object> channels = redisTemplate.opsForSet().members(CHANNEL_LIST_KEY);
            if (channels == null) {
                return;
            }

            for (Object channelObj : channels) {
                String channelName = channelObj.toString();
                String channelUsersKey = String.format(CHANNEL_USERS_KEY_PATTERN, channelName);
                Set<Object> userIds = redisTemplate.opsForSet().members(channelUsersKey);

                if (userIds != null) {
                    for (Object userIdObj : userIds) {
                        String userId = userIdObj.toString();
                        String userInfoKey = String.format(USER_INFO_KEY_PATTERN, userId);

                        // 사용자 정보가 없으면 채널에서 제거
                        if (!redisTemplate.hasKey(userInfoKey)) {
                            redisTemplate.opsForSet().remove(channelUsersKey, userId);
                        }
                    }

                    // 채널이 비어있으면 채널 목록에서 제거
                    if (getOnlineUserCount(channelName) == 0) {
                        redisTemplate.opsForSet().remove(CHANNEL_LIST_KEY, channelName);
                        redisTemplate.delete(channelUsersKey);
                    }
                }
            }
        } catch (Exception e) {
            log.error("Failed to cleanup expired users", e);
        }
    }

    private OnlineStatusResponse.UserOnlineStatus convertToUserOnlineStatus(UserOnlineInfo userInfo) {
        return OnlineStatusResponse.UserOnlineStatus.builder()
                .userId(userInfo.getUserId())
                .userName(userInfo.getUserName())
                .userRole(userInfo.getUserRole())
                .status(userInfo.getStatus())
                .lastSeen(userInfo.getLastSeen().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                .build();
    }
    // ===== 시험 상태 관련 메서드들 =====

    /**
     * 시험 상태 업데이트 (간소화)
     */
    public void updateExamStatus(String channelName, String status) {

        // null 체크
        if (channelName == null || status == null) {
            log.error("Invalid exam status request: channelName={}, status={}", channelName, status);
            return;
        }

        try {
            // 시험 상태 정보 생성
            ExamStatusInfo examStatusInfo = ExamStatusInfo.builder()
                    .channelName(channelName)
                    .status(status)
                    .lastSeen(LocalDateTime.now())
                    .build();

            // Redis에 시험 상태 저장
            String examStatusKey = String.format(EXAM_STATUS_KEY_PATTERN, channelName);
            redisTemplate.opsForValue().set(examStatusKey, examStatusInfo, EXAM_STATUS_TTL, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Failed to update exam status in Redis: channelName={}", channelName, e);
        }
    }

    /**
     * 시험 상태 조회 (간소화)
     */
    public String getExamStatus(String channelName) {

        // null 체크
        if (channelName == null) {
            log.error("ChannelName is null for getExamStatus");
            return null;
        }

        try {
            String examStatusKey = String.format(EXAM_STATUS_KEY_PATTERN, channelName);
            ExamStatusInfo examStatusInfo = (ExamStatusInfo) redisTemplate.opsForValue().get(examStatusKey);

            if (examStatusInfo != null) {
                return examStatusInfo.getStatus();
            } else {
                return null;
            }

        } catch (Exception e) {
            log.error("Failed to get exam status from Redis: channelName={}", channelName, e);
            return null;
        }
    }

    /**
     * 시험 상태 제거
     */
    public void removeExamStatus(String channelName) {

        // null 체크
        if (channelName == null) {
            log.error("ChannelName is null for removeExamStatus");
            return;
        }

        try {
            String examStatusKey = String.format(EXAM_STATUS_KEY_PATTERN, channelName);
            redisTemplate.delete(examStatusKey);

        } catch (Exception e) {
            log.error("Failed to remove exam status from Redis: channelName={}", channelName, e);
        }
    }

    /**
     * 시험 진행 상황 업데이트
     */
    public void updateExamProgress(ExamProgressRequest request) {

        // null 체크
        if (request == null || request.getChannelName() == null || request.getUserId() == null) {
            log.error("Invalid exam progress request: request={}", request);
            return;
        }

        try {
            // 기존 진행 상황 조회
            Map<String, ExamProgressResponse.UserExamProgress> existingProgress = getExamProgressMap(
                    request.getChannelName());
            if (existingProgress == null) {
                existingProgress = new java.util.HashMap<>();
            }

            // 기존 사용자 진행 상황 조회
            ExamProgressResponse.UserExamProgress existingUserProgress = existingProgress
                    .get(request.getUserId().toString());
            Map<String, Object> mergedQuestionAnswers = new java.util.HashMap<>();

            // 기존 답변들 유지
            if (existingUserProgress != null && existingUserProgress.getQuestionAnswers() != null) {
                mergedQuestionAnswers.putAll(existingUserProgress.getQuestionAnswers());
            }

            // 새로운 답변들 추가/업데이트
            if (request.getQuestionAnswers() != null) {
                mergedQuestionAnswers.putAll(request.getQuestionAnswers());
            }

            // 새로운 진행 상황 생성
            ExamProgressResponse.UserExamProgress userProgress = ExamProgressResponse.UserExamProgress.builder()
                    .userId(request.getUserId())
                    .currentQuestion(request.getCurrentQuestion())
                    .answeredQuestions(request.getAnsweredQuestions())
                    .questionAnswers(mergedQuestionAnswers)
                    .remainingTime(request.getRemainingTime())
                    .timestamp(request.getTimestamp())
                    .build();

            // 기존 진행 상황에 추가/업데이트
            existingProgress.put(request.getUserId().toString(), userProgress);

            // Redis에 시험 진행 상황 저장
            String examProgressKey = String.format(EXAM_PROGRESS_KEY_PATTERN, request.getChannelName());
            redisTemplate.opsForValue().set(examProgressKey, existingProgress, EXAM_STATUS_TTL, TimeUnit.SECONDS);

        } catch (Exception e) {
            log.error("Failed to update exam progress in Redis: channelName={}", request.getChannelName(), e);
        }
    }

    /**
     * 시험 진행 상황 조회 (Map 형태)
     */
    private Map<String, ExamProgressResponse.UserExamProgress> getExamProgressMap(String channelName) {

        // null 체크
        if (channelName == null) {
            log.error("Invalid exam progress request: channelName={}", channelName);
            return null;
        }

        try {
            // Redis에서 시험 진행 상황 조회
            String examProgressKey = String.format(EXAM_PROGRESS_KEY_PATTERN, channelName);
            Map<String, ExamProgressResponse.UserExamProgress> examProgress = (Map<String, ExamProgressResponse.UserExamProgress>) redisTemplate
                    .opsForValue().get(examProgressKey);

            if (examProgress != null) {
                return examProgress;
            } else {
                return new java.util.HashMap<>();
            }

        } catch (Exception e) {
            log.error("Failed to get exam progress from Redis: channelName={}", channelName, e);
            return new java.util.HashMap<>();
        }
    }

    /**
     * 시험 진행 상황 조회 (Response DTO 형태)
     */
    public ExamProgressResponse getExamProgress(String channelName) {

        // null 체크
        if (channelName == null) {
            log.error("Invalid exam progress request: channelName={}", channelName);
            return ExamProgressResponse.builder()
                    .channelName(channelName)
                    .userProgresses(new java.util.HashMap<>())
                    .timestamp(java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("ERROR")
                    .errorMessage("Channel name is null")
                    .build();
        }

        try {
            // Redis에서 시험 진행 상황 조회
            Map<String, ExamProgressResponse.UserExamProgress> userProgresses = getExamProgressMap(channelName);

            return ExamProgressResponse.builder()
                    .channelName(channelName)
                    .userProgresses(userProgresses != null ? userProgresses : new java.util.HashMap<>())
                    .timestamp(java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("SUCCESS")
                    .build();

        } catch (Exception e) {
            log.error("Failed to get exam progress from Redis: channelName={}", channelName, e);
            return ExamProgressResponse.builder()
                    .channelName(channelName)
                    .userProgresses(new java.util.HashMap<>())
                    .timestamp(java.time.LocalDateTime.now()
                            .format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .status("ERROR")
                    .errorMessage("Failed to get exam progress: " + e.getMessage())
                    .build();
        }
    }

    // 내부 클래스: 사용자 온라인 정보
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class UserOnlineInfo {
        private Long userId;
        private String userName;
        private String userRole;
        private String status;
        private LocalDateTime lastSeen;
    }

    // 내부 클래스: 시험 상태 정보 (간소화)
    @lombok.Data
    @lombok.Builder
    @lombok.AllArgsConstructor
    @lombok.NoArgsConstructor
    private static class ExamStatusInfo {
        private String channelName;
        private String status;
        private LocalDateTime lastSeen;
    }
}
