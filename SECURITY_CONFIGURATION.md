# Pullit Application Security Configuration

## Overview
This document describes the security configuration for the Pullit application, including authentication, authorization, and deployment guidelines.

## Authentication System

### JWT (JSON Web Token) Authentication
- **Algorithm**: RS256 (RSA with SHA-256)
- **Access Token Expiry**: 1 hour (production) / 24 hours (development)
- **Refresh Token Expiry**: 30 days (production) / 7 days (development)
- **Key Storage**: RSA key pairs stored in `backend/src/main/resources/keys/`

### Token Claims Structure
```json
{
  "iss": "pullit-production",
  "sub": "userId",
  "username": "user's username",
  "email": "user's email",
  "fullName": "user's full name",
  "role": "ADMIN|TEACHER|STUDENT",
  "iat": "issued at timestamp",
  "exp": "expiration timestamp"
}
```

## User Roles and Authorities

### Available Roles
1. **ROLE_ADMIN**: System administrator with full access
2. **ROLE_TEACHER**: Teacher with class and student management capabilities
3. **ROLE_STUDENT**: Student with limited access to their own data

## Endpoint Security Configuration

### Public Endpoints (No Authentication Required)
- `/swagger-ui/**` - API documentation (disabled in production)
- `/v3/api-docs/**` - OpenAPI specification (disabled in production)
- `/api/auth/login` - User login
- `/api/auth/register` - User registration
- `/api/auth/refresh` - Token refresh
- `/api/auth/oauth2/**` - OAuth2 authentication
- `/api/users/check/**` - Username/email availability check
- `/api/verification/**` - Email/SMS verification
- `/api/images/proxy/**` - Public image proxy
- `/ws/**` - WebSocket handshake endpoints (authentication handled at message level)
- `/ws/notifications/**` - Notification WebSocket
- `/sockjs-node/**` - SockJS fallback support
- `/stomp/**` - STOMP protocol support

### Admin-Only Endpoints
- `/api/admin/**` - All admin operations
- `/api/schools/manage/**` - School management
- `/api/users/manage/**` - User management
- `/api/system/**` - System operations

### Teacher-Only Endpoints
- `/api/teacher/**` - Teacher dashboard and statistics
- `/api/teacher-live-exams/**` - Live exam management
- `/api/classes/manage/**` - Class management
- `/api/assignments/create` - Create assignments
- `/api/assignments/*/edit` - Edit assignments
- `/api/assignments/*/delete` - Delete assignments
- `/api/students/manage/**` - Student management
- `/api/exams/create` - Create exams
- `/api/exams/*/edit` - Edit exams
- `/api/exams/*/delete` - Delete exams
- `/api/item-process/**` - Item processing
- `/api/ocr/**` - OCR operations
- `/api/file-history/**` - File history management

### Student-Only Endpoints
- `/api/students/me/**` - Student's own data
- `/api/submissions/submit` - Submit assignments
- `/api/cbt/student/**` - Student CBT operations

### Teacher or Student Endpoints
- `/api/classes/**` - Class information
- `/api/assignments/**` - Assignment operations
- `/api/submissions/**` - Submission operations
- `/api/exams/**` - Exam operations
- `/api/user-exams/**` - User exam data
- `/api/cbt/**` - CBT operations
- `/api/reports/**` - Reports
- `/api/stats/**` - Statistics

### Authenticated User Endpoints (Any logged-in user)
- `/api/users/me` - Current user profile
- `/api/items/**` - Item operations
- `/api/chapters/**` - Chapter data
- `/api/subjects/**` - Subject data
- `/api/schools/**` - School information
- `/api/calendar/**` - Calendar operations
- `/api/notifications/**` - Notifications
- `/api/dashboard/**` - Dashboard data
- `/api/files/**` - File operations
- `/api/images/**` - Image operations
- `/api/auth/logout` - Logout

## CORS Configuration

### Development Environment
```yaml
allowed-origins:
  - http://localhost:5173
  - http://localhost:5174
  - http://localhost:5175
  - http://localhost:3000
```

### Production Environment
```yaml
allowed-origins: ${CORS_ALLOWED_ORIGINS}
# Set via environment variable, e.g.:
# - https://pullit.com
# - https://app.pullit.com
```

### Allowed Methods
- GET, POST, PUT, DELETE, OPTIONS

### Allowed Headers
- Authorization
- Content-Type
- X-Requested-With
- Accept
- Origin

## Environment Variables Required for Production

### Database
- `SPRING_DATASOURCE_URL`: MySQL database URL
- `SPRING_DATASOURCE_USERNAME`: Database username
- `SPRING_DATASOURCE_PASSWORD`: Database password

### Redis
- `SPRING_REDIS_HOST`: Redis server host
- `SPRING_REDIS_PORT`: Redis server port (default: 6379)
- `SPRING_REDIS_PASSWORD`: Redis password (optional)

### Security
- `CORS_ALLOWED_ORIGINS`: Comma-separated list of allowed origins

### OAuth2 Providers
- `GOOGLE_CLIENT_ID`: Google OAuth2 client ID
- `GOOGLE_CLIENT_SECRET`: Google OAuth2 client secret
- `KAKAO_CLIENT_ID`: Kakao OAuth2 client ID
- `KAKAO_CLIENT_SECRET`: Kakao OAuth2 client secret
- `NAVER_CLIENT_ID`: Naver OAuth2 client ID
- `NAVER_CLIENT_SECRET`: Naver OAuth2 client secret

### External Services
- `AWS_ACCESS_KEY_ID`: AWS access key
- `AWS_SECRET_ACCESS_KEY`: AWS secret key
- `AWS_S3_BUCKET_NAME`: S3 bucket name
- `CLOVA_INVOKE_URL`: Clova OCR URL
- `CLOVA_SECRET_KEY`: Clova OCR secret
- `MATHPIX_APP_URL`: Mathpix API URL
- `MATHPIX_APP_ID`: Mathpix app ID
- `MATHPIX_APP_KEY`: Mathpix app key
- `COOLSMS_API_KEY`: CoolSMS API key
- `COOLSMS_API_SECRET`: CoolSMS API secret
- `COOLSMS_FROM_NUMBER`: SMS sender number

### URLs
- `FRONTEND_BASE_URL`: Frontend application URL
- `BACKEND_BASE_URL`: Backend API URL

## Deployment Checklist

### Before Deployment
1. ✅ Set Spring profile to `prod`: `SPRING_PROFILES_ACTIVE=prod`
2. ✅ Configure all required environment variables
3. ✅ Ensure RSA key pairs are properly generated and stored
4. ✅ Update CORS allowed origins for production domains
5. ✅ Disable Swagger UI in production (already configured)
6. ✅ Configure proper logging levels
7. ✅ Set up log file rotation
8. ✅ Configure database connection pooling
9. ✅ Set up Redis with password protection
10. ✅ Configure HTTPS/TLS (at reverse proxy level)

### Security Best Practices
1. **Never commit sensitive data**: Keep all secrets in environment variables
2. **Use HTTPS**: Always use HTTPS in production
3. **Rotate keys regularly**: Periodically rotate JWT signing keys
4. **Monitor logs**: Set up log monitoring for security events
5. **Rate limiting**: Consider implementing rate limiting for API endpoints
6. **Input validation**: All inputs are validated at controller level
7. **SQL injection protection**: JPA/Hibernate provides protection
8. **XSS protection**: Ensure frontend sanitizes all user inputs
9. **CSRF protection**: Disabled for stateless JWT authentication
10. **Session management**: Stateless sessions with JWT

## Testing Security Configuration

### Local Testing
```bash
# Start with development profile
./gradlew bootRun --args='--spring.profiles.active=local'

# Test authentication
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"test","password":"test123"}'

# Test protected endpoint with token
curl -X GET http://localhost:8080/api/users/me \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### Production Testing
```bash
# Start with production profile
java -jar pullit-backend.jar --spring.profiles.active=prod

# Verify security headers
curl -I https://api.pullit.com/api/auth/login

# Check CORS
curl -H "Origin: https://pullit.com" \
  -H "Access-Control-Request-Method: GET" \
  -H "Access-Control-Request-Headers: Authorization" \
  -X OPTIONS https://api.pullit.com/api/users/me
```

## Troubleshooting

### Common Issues

1. **401 Unauthorized**
   - Check if JWT token is expired
   - Verify token is included in Authorization header
   - Ensure "Bearer " prefix is included

2. **403 Forbidden**
   - Verify user has required role for endpoint
   - Check role authorities in SecurityConfig

3. **CORS Errors**
   - Verify origin is in allowed origins list
   - Check preflight OPTIONS requests are working
   - Ensure credentials are included if needed

4. **JWT Validation Errors**
   - Verify RSA keys are properly configured
   - Check token expiration time
   - Ensure issuer matches configuration

## Monitoring and Logging

### Security Events to Monitor
- Failed login attempts
- Token validation failures
- Access denied events
- Unusual API usage patterns

### Log Locations
- Development: `logs/pullit.log`
- Production: `/var/log/pullit/pullit.log`

### Health Check Endpoints
- `/actuator/health` - Application health
- `/actuator/health/liveness` - Kubernetes liveness probe
- `/actuator/health/readiness` - Kubernetes readiness probe
- `/actuator/metrics` - Application metrics

## WebSocket Security

### WebSocket Configuration
WebSocket connections require special handling in Spring Security:

1. **HTTP Handshake**: Initial WebSocket handshake is allowed without authentication (`permitAll()`)
2. **Message-Level Security**: Actual authentication happens at the STOMP message level
3. **Token Transmission**: JWT tokens should be sent as STOMP headers during connection

### WebSocket Authentication Flow
```javascript
// Frontend WebSocket connection example
const socket = new SockJS('/ws/notifications');
const stompClient = Stomp.over(socket);

stompClient.connect({
    'Authorization': 'Bearer ' + jwtToken
}, function(frame) {
    // Connected
    stompClient.subscribe('/user/queue/notifications', function(notification) {
        // Handle notification
    });
});
```

### WebSocket Endpoints
- `/ws/notifications` - Real-time notifications
- `/topic/class/**` - Class-specific broadcasts (authenticated users)
- `/topic/admin/**` - Admin broadcasts (ROLE_ADMIN only)
- `/topic/teacher/**` - Teacher broadcasts (ROLE_TEACHER only)
- `/user/queue/notifications` - User-specific notifications

### WebSocket Security Rules
- All STOMP subscriptions require authentication
- Message destinations are protected by role
- Cross-origin WebSocket connections follow CORS configuration

## Contact and Support
For security issues or questions, contact the development team immediately.
Do not post security vulnerabilities in public issue trackers.