# 알림 시스템 (Notification System)

## 개요
Redis와 WebSocket을 활용한 실시간 알림 시스템입니다. 커스텀 어노테이션을 통해 자동으로 알림을 생성하고, 실시간으로 사용자에게 전달합니다.

## 주요 기능

### 1. 실시간 알림 전송
- WebSocket을 통한 실시간 알림 푸시
- Redis Pub/Sub을 활용한 서버 간 알림 동기화
- 읽지 않은 알림 개수 실시간 업데이트

### 2. 알림 저장 및 관리
- Redis를 사용한 빠른 알림 데이터 저장/조회
- TTL 설정으로 오래된 알림 자동 삭제 (30일)
- 사용자별 알림 리스트 관리 (최대 100개)

### 3. 커스텀 어노테이션 (@NotificationTrigger)
메소드에 어노테이션을 추가하여 자동으로 알림 생성:

```java
@NotificationTrigger(
    type = NotificationType.CLASS_INVITATION,
    multipleUsers = true,
    userIdsExpression = "#result.invitedStudentIds",
    message = "#result.className + ' 반에 초대되었습니다'",
    targetUrl = "'/student/class-room/my-class'"
)
public StudentInvitationResponse inviteStudents(...) {
    // 메소드 실행 후 자동으로 알림 생성
}
```

## 알림 타입

```java
public enum NotificationType {
    EXAM_ASSIGNED,      // 시험 배정
    EXAM_COMPLETED,     // 시험 완료
    EXAM_RESULT,        // 시험 결과
    CLASS_INVITATION,   // 반 초대
    CLASS_ANNOUNCEMENT, // 반 공지
    HOMEWORK_ASSIGNED,  // 과제 배정
    HOMEWORK_DEADLINE,  // 과제 마감
    GRADE_UPDATED,      // 성적 업데이트
    MESSAGE_RECEIVED,   // 메시지 수신
    SYSTEM_NOTICE,      // 시스템 공지
    ACHIEVEMENT_UNLOCKED, // 성취 달성
    REMINDER           // 리마인더
}
```

## API 엔드포인트

### 알림 조회
```
GET /api/notifications?page=0&size=20
```

### 읽지 않은 알림 개수
```
GET /api/notifications/unread-count
```

### 알림 읽음 처리
```
PUT /api/notifications/{notificationId}/read
```

### 모든 알림 읽음 처리
```
PUT /api/notifications/read-all
```

### 알림 삭제
```
DELETE /api/notifications/{notificationId}
```

## WebSocket 연결

### 연결 URL
```
ws://localhost:8080/ws/notifications?userId={userId}
```

### 메시지 타입
- `CONNECTION`: 연결 성공
- `NOTIFICATION`: 새 알림 수신
- `UNREAD_COUNT_UPDATE`: 읽지 않은 알림 개수 업데이트
- `PING/PONG`: 연결 상태 확인

## 프론트엔드 사용법

### 1. NotificationBell 컴포넌트 추가
```vue
<template>
  <Header>
    <NotificationBell />
  </Header>
</template>
```

### 2. 알림 API 사용
```javascript
import notificationApi from '@/services/notificationApi'

// 알림 목록 조회
const notifications = await notificationApi.getNotifications()

// 읽지 않은 개수 조회
const { count } = await notificationApi.getUnreadCount()

// 알림 읽음 처리
await notificationApi.markAsRead(notificationId)
```

## 설정 및 환경변수

### Redis 설정 (application.yml)
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60000ms
      lettuce:
        pool:
          max-active: 10
          max-idle: 10
          min-idle: 5
```

### WebSocket CORS 설정
```java
registry.addHandler(notificationWebSocketHandler, "/ws/notifications")
    .setAllowedOrigins(
        "http://localhost:5173",
        "http://localhost:3000"
    );
```

## 테스트

### 테스트 알림 생성
```bash
curl -X POST http://localhost:8080/api/notifications/test \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer {token}" \
  -d '{
    "userId": 1,
    "type": "EXAM_ASSIGNED",
    "customTitle": "테스트 알림",
    "customMessage": "테스트 알림 메시지입니다"
  }'
```

## 주의사항

1. Redis 서버가 실행 중이어야 합니다
2. WebSocket 연결 시 사용자 인증이 필요합니다
3. 알림은 최대 30일간 보관됩니다
4. 사용자당 최대 100개의 알림만 저장됩니다
5. 프론트엔드에서 WebSocket 재연결 로직이 구현되어 있습니다