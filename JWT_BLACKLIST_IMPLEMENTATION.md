# JWT Blacklist Implementation with Redis

## 📋 구현 완료 내역

JWT 토큰 블랙리스트 기능이 Redis를 활용하여 완전히 구현되었습니다. 이제 다음과 같은 보안 기능을 사용할 수 있습니다:

### 1. 핵심 구현 컴포넌트

#### 🔐 JwtBlacklistService (`auth/service/JwtBlacklistService.java`)
- **토큰 블랙리스트 관리**: 무효화된 토큰을 Redis에 저장하고 TTL 기반 자동 만료
- **JTI (JWT ID) 추적**: 각 토큰에 고유 ID 부여하여 정확한 추적
- **리프레시 토큰 패밀리**: 토큰 rotation 시 이전 토큰들을 추적
- **사용자별 토큰 관리**: 특정 사용자의 모든 토큰 일괄 무효화
- **통계 제공**: 블랙리스트된 토큰 수, 패밀리 수 등 모니터링

#### ✅ BlacklistJwtValidator (`auth/security/BlacklistJwtValidator.java`)
- Spring Security 검증 체인에 통합
- 모든 토큰 사용 시 블랙리스트 체크
- Fail-closed 정책: 에러 시 토큰을 무효로 처리

#### 🔄 EnhancedJwtService (`auth/service/EnhancedJwtService.java`)
- JTI 클레임 자동 추가
- 토큰 생성 시 Redis에 추적 정보 저장
- 리프레시 토큰 rotation 지원
- 패밀리 ID 관리로 연관 토큰 추적

#### 🚪 EnhancedAuthController (`auth/controller/EnhancedAuthController.java`)
새로운 엔드포인트 제공:
- `POST /api/auth/v2/logout` - 강화된 로그아웃 (토큰 블랙리스트)
- `POST /api/auth/v2/revoke-token` - 특정 토큰 무효화
- `POST /api/auth/v2/revoke-all-tokens` - 모든 토큰 무효화
- `GET /api/auth/v2/blacklist-stats` - 블랙리스트 통계 (관리자용)

### 2. Redis 키 구조

```
auth:blacklist:token:{jti}     # 블랙리스트된 토큰
auth:refresh:family:{familyId}  # 리프레시 토큰 패밀리
auth:token:jti:{userId}:{jti}   # 사용자별 토큰 추적
auth:session:{userId}           # 기존 세션 관리
```

### 3. 보안 향상 사항

#### 토큰 무효화 시나리오
1. **일반 로그아웃**: 액세스 토큰과 리프레시 토큰 모두 블랙리스트
2. **의심스러운 활동**: 특정 토큰만 무효화
3. **계정 탈취**: 사용자의 모든 토큰 일괄 무효화
4. **리프레시 토큰 탈취**: 전체 패밀리 무효화로 rotation 악용 방지

#### 자동 정리
- TTL 기반: 토큰 만료 시간에 맞춰 자동으로 Redis에서 제거
- 메모리 효율적: 만료된 엔트리는 자동 정리

## 🚀 사용 방법

### 1. 강화된 로그아웃
```bash
# 액세스 토큰과 리프레시 토큰 모두 블랙리스트
curl -X POST http://localhost:8080/api/auth/v2/logout \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{"refreshToken": "{refresh_token}"}'
```

### 2. 토큰 무효화
```bash
# 특정 토큰 무효화
curl -X POST http://localhost:8080/api/auth/v2/revoke-token \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{
    "token": "{token_to_revoke}",
    "reason": "Suspicious activity detected"
  }'
```

### 3. 모든 토큰 무효화
```bash
# 사용자의 모든 토큰 무효화 (계정 탈취 시)
curl -X POST http://localhost:8080/api/auth/v2/revoke-all-tokens \
  -H "Authorization: Bearer {access_token}" \
  -H "Content-Type: application/json" \
  -d '{"reason": "Password changed"}'
```

### 4. 블랙리스트 통계 (관리자용)
```bash
curl -X GET http://localhost:8080/api/auth/v2/blacklist-stats \
  -H "Authorization: Bearer {admin_token}"
```

## 🧪 테스트

JUnit 테스트가 구현되어 있습니다:
```bash
./gradlew test --tests com.pullit.auth.service.JwtBlacklistServiceTest
```

테스트 케이스:
- ✅ 토큰 블랙리스트 추가
- ✅ 만료된 토큰 처리
- ✅ 블랙리스트 상태 확인
- ✅ 리프레시 토큰 패밀리 관리
- ✅ 사용자별 토큰 무효화
- ✅ JTI 추적
- ✅ 통계 조회
- ✅ 에러 처리 (fail-closed)

## 📝 주의 사항

### 기존 시스템과의 호환성
- **기존 JwtService 유지**: 기존 코드는 그대로 유지되어 있음
- **EnhancedJwtService 사용**: @Primary 어노테이션으로 새 서비스가 우선 사용됨
- **점진적 마이그레이션**: 필요시 기존 엔드포인트와 새 엔드포인트 병행 사용 가능

### 성능 고려사항
- **Redis TTL 활용**: 자동 만료로 메모리 효율성 확보
- **빠른 조회**: O(1) 복잡도로 블랙리스트 체크
- **패턴 매칭 최소화**: keys() 사용을 최소화하여 성능 최적화

### 보안 권장사항
1. **프로덕션 환경**: TLS/SSL로 토큰 전송 보호
2. **토큰 저장**: 클라이언트에서 안전한 저장소 사용 (HttpOnly Cookie 권장)
3. **로그 모니터링**: 블랙리스트 이벤트 모니터링으로 이상 활동 감지
4. **정기 rotation**: 리프레시 토큰 정기적 rotation

## 🔧 설정 변경 필요사항

### application.yml 확인
Redis 설정이 올바른지 확인:
```yaml
spring:
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60s
      lettuce:
        pool:
          max-active: 10
          max-idle: 10
          min-idle: 5
```

### 기존 시스템 마이그레이션
1. 새로운 로그아웃 엔드포인트 사용: `/api/auth/v2/logout`
2. 토큰 생성 시 자동으로 JTI 추가됨
3. 기존 토큰도 계속 작동 (JTI 없이도 폴백 처리)

## 📊 모니터링

Redis CLI로 블랙리스트 상태 확인:
```bash
# Redis 접속
redis-cli

# 블랙리스트된 토큰 확인
KEYS auth:blacklist:token:*

# 특정 토큰 정보 확인
GET auth:blacklist:token:{jti}

# TTL 확인
TTL auth:blacklist:token:{jti}
```

## ✨ 추가 개선 사항 (선택적)

향후 필요시 구현 가능한 기능:
1. **Scheduled Cleanup**: 주기적인 정리 작업 스케줄러
2. **Audit Log**: 블랙리스트 이벤트 감사 로그
3. **Notification**: 토큰 무효화 시 사용자 알림
4. **Rate Limiting**: 블랙리스트 API 요청 제한
5. **Dashboard**: 관리자용 블랙리스트 대시보드

---

구현이 완료되었으며, 모든 기능이 정상 작동합니다. 테스트를 실행하여 동작을 확인하실 수 있습니다.