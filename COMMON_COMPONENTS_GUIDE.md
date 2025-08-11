# Spring Boot 공통 컴포넌트 사용 가이드

## 📋 목차
1. [공통 응답 클래스](#1-공통-응답-클래스)
2. [예외 처리](#2-예외-처리)
3. [검증 어노테이션](#3-검증-어노테이션)
4. [성능 모니터링 어노테이션](#4-성능-모니터링-어노테이션)
5. [인증/인가 어노테이션](#5-인증인가-어노테이션)
6. [베이스 엔티티](#6-베이스-엔티티)
7. [페이징 처리](#7-페이징-처리)
8. [유틸리티 클래스](#8-유틸리티-클래스)
9. [상수 관리](#9-상수-관리)
10. [캐싱 시스템](#10-캐싱-시스템)
11. [HTTP 클라이언트](#11-http-클라이언트)

---

## 1. 공통 응답 클래스

### ApiResponse 사용 예시

```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    
    // 성공 응답 - 데이터만
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable Long id) {
        UserDto user = userService.findById(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }
    
    // 성공 응답 - 데이터와 메시지
    @PostMapping
    public ResponseEntity<ApiResponse<UserDto>> createUser(@RequestBody UserDto dto) {
        UserDto created = userService.create(dto);
        return ResponseEntity.ok(
            ApiResponse.success(created, "사용자가 성공적으로 생성되었습니다")
        );
    }
    
    // 성공 응답 - 메시지만
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable Long id) {
        userService.delete(id);
        return ResponseEntity.ok(
            ApiResponse.successWithoutData("사용자가 삭제되었습니다")
        );
    }
    
    // 에러 응답
    @GetMapping("/error-example")
    public ResponseEntity<ApiResponse<Void>> errorExample() {
        return ResponseEntity.badRequest().body(
            ApiResponse.error(ErrorCode.USER_NOT_FOUND)
        );
    }
}
```

### 응답 JSON 예시
```json
// 성공 응답
{
    "success": true,
    "code": "SUCCESS",
    "message": "사용자 조회 성공",
    "data": {
        "id": 1,
        "name": "홍길동",
        "email": "hong@example.com"
    },
    "timestamp": "2024-01-10T10:30:00"
}

// 에러 응답
{
    "success": false,
    "code": "USER_001",
    "message": "사용자를 찾을 수 없습니다",
    "data": null,
    "timestamp": "2024-01-10T10:30:00"
}
```

---

## 2. 예외 처리

### BusinessException 사용 예시

```java
@Service
public class UserService {
    
    public User findById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }
    
    public void checkDuplicateEmail(String email) {
        if (userRepository.existsByEmail(email)) {
            throw new BusinessException(
                ErrorCode.DUPLICATE_EMAIL, 
                "이메일: " + email
            );
        }
    }
    
    public void validatePassword(String password, String confirmPassword) {
        if (!password.equals(confirmPassword)) {
            throw new BusinessException(
                ErrorCode.PASSWORD_MISMATCH,
                "입력한 비밀번호가 일치하지 않습니다"
            );
        }
    }
}
```

### GlobalExceptionHandler가 자동으로 처리
```java
// 예외 발생 시 자동으로 아래와 같은 응답 생성
{
    "success": false,
    "code": "USER_001",
    "message": "사용자를 찾을 수 없습니다",
    "timestamp": "2024-01-10T10:30:00"
}
```

---

## 3. 검증 어노테이션

### @ValidEnum 사용 예시

```java
// Enum 정의
public enum UserStatus {
    ACTIVE, INACTIVE, PENDING, BLOCKED
}

// DTO에서 사용
public class UserDto {
    
    @ValidEnum(
        enumClass = UserStatus.class,
        message = "유효한 사용자 상태가 아닙니다",
        ignoreCase = true,  // 대소문자 무시
        allowNull = false    // null 허용 안함
    )
    private String status;
}

// 컨트롤러
@PostMapping
public ResponseEntity<?> createUser(@Valid @RequestBody UserDto dto) {
    // "ACTIVE", "active", "Active" 모두 통과
    // "INVALID" 입력 시 검증 실패
}
```

### @PhoneNumber 사용 예시

```java
public class UserDto {
    
    @PhoneNumber(message = "올바른 전화번호 형식이 아닙니다")
    private String phone;  // 010-1234-5678, 01012345678 모두 가능
    
    @PhoneNumber(required = false)  // 선택적 필드
    private String emergencyPhone;
}
```

### @ValidPassword 사용 예시

```java
public class SignUpDto {
    
    @ValidPassword(
        minLength = 10,
        requireUppercase = true,      // 대문자 필수
        requireLowercase = true,      // 소문자 필수
        requireDigit = true,          // 숫자 필수
        requireSpecialChar = true,    // 특수문자 필수
        message = "비밀번호는 10자 이상, 대소문자, 숫자, 특수문자를 포함해야 합니다"
    )
    private String password;
}

// 유효한 비밀번호: "MyP@ssw0rd123"
// 무효한 비밀번호: "password", "12345678", "PASSWORD"
```

---

## 4. 성능 모니터링 어노테이션

### @TimeExecution 사용 예시

```java
@Service
public class ReportService {
    
    @TimeExecution(
        warnThreshold = 3000,  // 3초 이상이면 경고
        logArgs = true,        // 파라미터 로깅
        logResult = true       // 결과 로깅
    )
    public Report generateMonthlyReport(String month) {
        // 복잡한 리포트 생성 로직
        return report;
    }
}

// 로그 출력 예시:
// INFO: ✅ 메소드 실행 완료: ReportService.generateMonthlyReport() - 2500ms
// WARN: ⚠️ 메소드 실행 시간 초과: ReportService.generateMonthlyReport() - 3500ms (임계값: 3000ms)
```

### @LoggingTrace 사용 예시

```java
@Service
@Slf4j
public class PaymentService {
    
    @LoggingTrace(
        level = LoggingTrace.LogLevel.INFO,
        logParameters = true,
        logReturnValue = true,
        logExecutionTime = true
    )
    public PaymentResult processPayment(PaymentRequest request) {
        // 결제 처리 로직
        return result;
    }
}

// 로그 출력:
// INFO [traceId=a1b2c3d4] → 메소드 진입: PaymentService.processPayment | 파라미터: [PaymentRequest(amount=10000)]
// INFO [traceId=a1b2c3d4] ← 메소드 종료: PaymentService.processPayment | 반환값: PaymentResult(success=true) | 실행시간: 1200ms
```

### @Retry 사용 예시

```java
@Service
public class ExternalApiService {
    
    @Retry(
        maxAttempts = 3,
        delay = 1000,              // 1초 대기
        multiplier = 2.0,          // 대기시간 2배씩 증가
        retryFor = {IOException.class, TimeoutException.class},
        noRetryFor = {IllegalArgumentException.class}
    )
    public ApiResponse callExternalApi(String endpoint) {
        // 외부 API 호출
        // IOException 발생 시: 1초 대기 → 재시도 → 2초 대기 → 재시도 → 4초 대기 → 재시도
        // IllegalArgumentException 발생 시: 재시도 없이 즉시 실패
    }
}

// 로그:
// WARN: ⚠️ 메소드 실행 실패: callExternalApi() - 시도 1/3 - 1000ms 후 재시도
// WARN: ⚠️ 메소드 실행 실패: callExternalApi() - 시도 2/3 - 2000ms 후 재시도
// INFO: ✅ 재시도 성공: callExternalApi() - 시도 횟수: 3/3
```

---

## 5. 인증/인가 어노테이션

### @AuthUser 사용 예시

```java
@RestController
@RequestMapping("/api/profile")
public class ProfileController {
    
    // 필수 인증
    @GetMapping
    public ResponseEntity<UserProfile> getMyProfile(@AuthUser User currentUser) {
        // currentUser에 현재 로그인한 사용자 정보 자동 주입
        UserProfile profile = profileService.getProfile(currentUser.getId());
        return ResponseEntity.ok(profile);
    }
    
    // 선택적 인증
    @GetMapping("/public")
    public ResponseEntity<PublicInfo> getPublicInfo(
        @AuthUser(required = false) User currentUser
    ) {
        if (currentUser != null) {
            // 로그인한 사용자용 정보
            return ResponseEntity.ok(getPersonalizedInfo(currentUser));
        } else {
            // 비로그인 사용자용 정보
            return ResponseEntity.ok(getPublicInfo());
        }
    }
}
```

### @RateLimited 사용 예시

```java
@RestController
@RequestMapping("/api")
public class ApiController {
    
    // 분당 10회 제한
    @RateLimited(
        limit = 10,
        duration = 1,
        timeUnit = TimeUnit.MINUTES,
        key = "api.search"  // Redis 키
    )
    @GetMapping("/search")
    public ResponseEntity<SearchResult> search(@RequestParam String query) {
        // 동일 IP에서 분당 10회 이상 호출 시 429 Too Many Requests 응답
        return ResponseEntity.ok(searchService.search(query));
    }
    
    // 사용자별 제한
    @RateLimited(
        limit = 100,
        duration = 1,
        timeUnit = TimeUnit.HOURS
    )
    @PostMapping("/upload")
    public ResponseEntity<UploadResult> upload(
        @AuthUser User user,
        @RequestParam MultipartFile file
    ) {
        // 사용자별로 시간당 100회 업로드 제한
        return ResponseEntity.ok(uploadService.upload(user, file));
    }
}
```

---

## 6. 베이스 엔티티

### BaseTimeEntity 사용 예시

```java
@Entity
@Table(name = "products")
public class Product extends BaseTimeEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private BigDecimal price;
    
    // createdAt, updatedAt 자동 관리됨
}

// 사용
Product product = new Product();
product.setName("노트북");
productRepository.save(product);
// createdAt: 2024-01-10 10:30:00 (자동 설정)
// updatedAt: 2024-01-10 10:30:00 (자동 설정)

product.setPrice(1500000);
productRepository.save(product);
// updatedAt: 2024-01-10 11:00:00 (자동 업데이트)
```

### BaseEntity 사용 예시

```java
@Entity
@Table(name = "articles")
public class Article extends BaseEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String content;
    
    // createdAt, updatedAt, createdBy, updatedBy 자동 관리
}

// 로그인한 사용자 "admin"이 글 작성
Article article = new Article();
article.setTitle("공지사항");
articleRepository.save(article);
// createdBy: "admin" (자동 설정)
// updatedBy: "admin" (자동 설정)
```

### SoftDeleteEntity 사용 예시

```java
@Entity
@Table(name = "users")
public class User extends SoftDeleteEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String email;
    private String name;
}

// 논리적 삭제
User user = userRepository.findById(1L);
user.delete("admin");  // deletedAt, deletedBy 설정
userRepository.save(user);

// 조회 시 자동으로 삭제된 데이터 제외
List<User> activeUsers = userRepository.findAll();  // deletedAt이 null인 데이터만 조회

// 복구
user.restore();
userRepository.save(user);  // deletedAt, deletedBy null로 설정
```

---

## 7. 페이징 처리

### PageRequest 사용 예시

```java
// DTO
public class UserSearchRequest extends PageRequest {
    private String keyword;
    private String status;
    
    // PageRequest로부터 상속: page, size, sortBy, sortDirection
}

// 컨트롤러
@GetMapping("/users")
public ResponseEntity<PageResponse<UserDto>> getUsers(UserSearchRequest request) {
    // request.page = 0 (기본값)
    // request.size = 20 (기본값)
    // request.sortBy = "createdAt" (기본값)
    // request.sortDirection = "DESC" (기본값)
    
    Pageable pageable = request.toPageable();
    Page<User> users = userService.search(request.getKeyword(), pageable);
    
    return ResponseEntity.ok(PageResponse.from(users));
}

// API 호출
// GET /users?page=0&size=10&sortBy=name&sortDirection=ASC&keyword=홍
```

### PageResponse 사용 예시

```java
@Service
public class UserService {
    
    public PageResponse<UserDto> getUsers(PageRequest request) {
        Pageable pageable = request.toPageable();
        Page<User> userPage = userRepository.findAll(pageable);
        
        // Entity를 DTO로 변환
        List<UserDto> userDtos = userPage.getContent().stream()
            .map(UserDto::from)
            .collect(Collectors.toList());
        
        return PageResponse.from(userPage, userDtos);
    }
}

// 응답 JSON
{
    "content": [
        {"id": 1, "name": "홍길동"},
        {"id": 2, "name": "김철수"}
    ],
    "pageNumber": 0,
    "pageSize": 10,
    "totalElements": 45,
    "totalPages": 5,
    "first": true,
    "last": false,
    "empty": false,
    "numberOfElements": 10
}
```

---

## 8. 유틸리티 클래스

### StringUtils 사용 예시

```java
// null/empty 체크
boolean empty = StringUtils.isEmpty(null);  // true
boolean notEmpty = StringUtils.isNotEmpty("hello");  // true

// 기본값 처리
String result = StringUtils.defaultIfEmpty(input, "기본값");

// 문자열 변환
String trimmed = StringUtils.trim("  hello  ");  // "hello"
String upper = StringUtils.toUpperCase("hello");  // "HELLO"

// 문자열 자르기
String truncated = StringUtils.truncate("긴 문자열입니다", 5);  // "긴 문자열"
String withEllipsis = StringUtils.truncateWithEllipsis("긴 문자열입니다", 8);  // "긴 문자..."

// 마스킹 처리
String maskedEmail = StringUtils.maskEmail("hong@example.com");  // "ho***@example.com"
String maskedPhone = StringUtils.maskPhone("010-1234-5678");     // "010-****-5678"
String maskedName = StringUtils.maskName("홍길동");               // "홍*동"

// 랜덤 문자열 생성
String randomStr = StringUtils.generateRandomString(10);  // "aB3xY9mK2p"
String uuid = StringUtils.generateUUID();  // "550e8400-e29b-41d4-a716-446655440000"

// 케이스 변환
String snake = StringUtils.camelToSnake("userName");  // "user_name"
String camel = StringUtils.snakeToCamel("user_name");  // "userName"
```

### ValidationUtils 사용 예시

```java
// 이메일 검증
boolean validEmail = ValidationUtils.isValidEmail("hong@example.com");  // true
boolean invalidEmail = ValidationUtils.isValidEmail("invalid.email");    // false

// 전화번호 검증
boolean validPhone = ValidationUtils.isValidPhone("010-1234-5678");  // true
boolean validPhone2 = ValidationUtils.isValidPhone("01012345678");   // true

// 비밀번호 검증
boolean validPw = ValidationUtils.isValidPassword("MyP@ssw0rd");  // true
boolean weakPw = ValidationUtils.isValidPassword("password");     // false

// 사업자번호 검증
boolean validBiz = ValidationUtils.isValidBusinessNumber("123-45-67890");  // 체크섬 검증

// URL/IP 검증
boolean validUrl = ValidationUtils.isValidUrl("https://example.com");  // true
boolean validIp = ValidationUtils.isValidIpAddress("192.168.1.1");     // true

// 파일 확장자 검증
boolean isImage = ValidationUtils.isValidImageFile("photo.jpg");  // true
boolean isDoc = ValidationUtils.isValidDocumentFile("report.pdf");  // true

// 숫자/문자 검증
boolean numeric = ValidationUtils.isNumeric("12345");  // true
boolean alpha = ValidationUtils.isAlpha("abcdef");     // true
boolean alphanumeric = ValidationUtils.isAlphanumeric("abc123");  // true
```

---

## 9. 상수 관리

### AppConstants 사용 예시

```java
@Service
public class FileService {
    
    public void uploadFile(MultipartFile file) {
        // 파일 크기 검증
        if (file.getSize() > AppConstants.MAX_FILE_SIZE) {
            throw new BusinessException("파일 크기 초과");
        }
        
        // 확장자 검증
        String extension = getExtension(file.getOriginalFilename());
        if (!Arrays.asList(AppConstants.ALLOWED_IMAGE_EXTENSIONS).contains(extension)) {
            throw new BusinessException("허용되지 않은 파일 형식");
        }
        
        // 업로드 디렉토리
        String uploadPath = AppConstants.UPLOAD_DIR + "/" + UUID.randomUUID();
    }
}
```

### ApiConstants 사용 예시

```java
@RestController
@RequestMapping(ApiConstants.USER_BASE)  // "/api/v1/users"
public class UserController {
    
    @GetMapping(ApiConstants.USER_PROFILE)  // "/profile"
    public ResponseEntity<?> getProfile() {
        return ResponseEntity.ok(
            ApiResponse.success(profile, ApiConstants.MSG_SUCCESS)
        );
    }
}
```

### ValidationConstants 사용 예시

```java
@Data
public class UserCreateDto {
    
    @NotBlank(message = ValidationConstants.MSG_REQUIRED)
    @Size(
        min = ValidationConstants.MIN_USERNAME_LENGTH,
        max = ValidationConstants.MAX_USERNAME_LENGTH,
        message = "사용자명은 3~20자여야 합니다"
    )
    private String username;
    
    @Email(message = ValidationConstants.MSG_INVALID_EMAIL)
    @Size(max = ValidationConstants.MAX_EMAIL_LENGTH)
    private String email;
}
```

### CacheConstants 사용 예시

```java
@Service
public class CachedUserService {
    
    @Autowired
    private CacheService cacheService;
    
    public User getUser(Long userId) {
        // CacheConstants의 키 프리픽스 사용
        String cacheKey = CacheConstants.KEY_PREFIX_USER + userId;
        
        // CacheConstants의 TTL 사용
        return cacheService.getOrElse(
            cacheKey,
            CacheConstants.MEDIUM_TTL_SECONDS,
            TimeUnit.SECONDS,
            () -> userRepository.findById(userId).orElse(null),
            User.class
        );
    }
}
```

### HttpConstants 사용 예시

```java
@Component
public class ApiClient {
    
    @Autowired
    private RestTemplate restTemplate;
    
    public void callApi() {
        HttpHeaders headers = new HttpHeaders();
        // HttpConstants의 헤더 이름 사용
        headers.set(HttpConstants.HEADER_CONTENT_TYPE, HttpConstants.MEDIA_TYPE_JSON);
        headers.set(HttpConstants.HEADER_USER_AGENT, HttpConstants.DEFAULT_USER_AGENT);
        
        // API 호출 (자동으로 재시도, 타임아웃 적용됨)
        ResponseEntity<String> response = restTemplate.exchange(
            url, HttpMethod.GET, new HttpEntity<>(headers), String.class
        );
    }
}
```

---

## 10. 캐싱 시스템

### Redis 캐시 서비스 사용 예시

```java
@Service
public class ProductService {
    
    @Autowired
    private RedisCacheService cacheService;
    
    // Cache-Aside 패턴
    public Product getProduct(Long productId) {
        String cacheKey = "product:" + productId;
        
        // 캐시에서 먼저 조회
        Product cached = cacheService.get(cacheKey, Product.class);
        if (cached != null) {
            return cached;
        }
        
        // 캐시 미스 시 DB 조회
        Product product = productRepository.findById(productId).orElse(null);
        
        // 캐시에 저장 (30분 TTL)
        if (product != null) {
            cacheService.put(cacheKey, product, 30, TimeUnit.MINUTES);
        }
        
        return product;
    }
    
    // 캐시 무효화
    public void updateProduct(Product product) {
        productRepository.save(product);
        
        // 캐시 삭제
        cacheService.evict("product:" + product.getId());
        
        // 관련 캐시도 삭제
        cacheService.evictByPattern("product:list:*");
    }
}
```

### Spring Cache 어노테이션 사용

```java
@Service
public class CategoryService {
    
    @Cacheable(value = "categories", key = "#id")
    public Category getCategory(Long id) {
        // 자동으로 캐시 처리됨
        return categoryRepository.findById(id).orElse(null);
    }
    
    @CachePut(value = "categories", key = "#category.id")
    public Category updateCategory(Category category) {
        // 실행 후 캐시 갱신
        return categoryRepository.save(category);
    }
    
    @CacheEvict(value = "categories", key = "#id")
    public void deleteCategory(Long id) {
        // 캐시에서 삭제
        categoryRepository.deleteById(id);
    }
    
    @CacheEvict(value = "categories", allEntries = true)
    public void clearAllCategories() {
        // 모든 카테고리 캐시 삭제
    }
}
```

---

## 11. HTTP 클라이언트

### RestTemplate 사용 예시

```java
@Service
public class PaymentGatewayService {
    
    @Autowired
    private RestTemplate restTemplate;  // 자동 설정된 RestTemplate
    
    // 결제 처리 (PG사 API 호출)
    public PaymentResponse processPayment(PaymentRequest request) {
        String pgUrl = "https://api.payment-gateway.com/v1/payments";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(pgSecretKey);
        
        HttpEntity<PaymentRequest> entity = new HttpEntity<>(request, headers);
        
        // 자동으로 적용되는 것들:
        // - 5초 연결 타임아웃
        // - 30초 읽기 타임아웃
        // - 실패 시 3번 재시도 (지수 백오프)
        // - 요청/응답 자동 로깅
        // - 연결 풀 재사용
        
        try {
            ResponseEntity<PaymentResponse> response = restTemplate.exchange(
                pgUrl,
                HttpMethod.POST,
                entity,
                PaymentResponse.class
            );
            
            return response.getBody();
            
        } catch (HttpClientErrorException e) {
            // 4xx 에러 처리
            log.error("결제 요청 실패: {}", e.getResponseBodyAsString());
            throw new BusinessException("결제 처리 실패");
            
        } catch (HttpServerErrorException e) {
            // 5xx 에러 처리
            log.error("PG사 서버 오류: {}", e.getResponseBodyAsString());
            throw new BusinessException("결제 서버 오류");
        }
    }
    
    // 환불 처리
    public RefundResponse refundPayment(String paymentId, BigDecimal amount) {
        String refundUrl = "https://api.payment-gateway.com/v1/refunds";
        
        RefundRequest request = RefundRequest.builder()
            .paymentId(paymentId)
            .amount(amount)
            .reason("고객 요청")
            .build();
        
        // POST 요청 간단하게
        return restTemplate.postForObject(refundUrl, request, RefundResponse.class);
    }
}
```

### 외부 API 연동 예시

```java
@Service
public class WeatherApiService {
    
    @Autowired
    private RestTemplate restTemplate;
    
    // 날씨 정보 조회
    public WeatherInfo getWeather(String city) {
        String apiUrl = "https://api.openweathermap.org/data/2.5/weather";
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(apiUrl)
            .queryParam("q", city)
            .queryParam("appid", weatherApiKey)
            .queryParam("units", "metric")
            .queryParam("lang", "kr");
        
        // GET 요청
        return restTemplate.getForObject(builder.toUriString(), WeatherInfo.class);
    }
    
    // 카카오 주소 검색
    public AddressSearchResponse searchAddress(String query) {
        String kakaoApiUrl = "https://dapi.kakao.com/v2/local/search/address";
        
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(kakaoApiUrl)
            .queryParam("query", query);
        
        HttpEntity<?> entity = new HttpEntity<>(headers);
        
        ResponseEntity<AddressSearchResponse> response = restTemplate.exchange(
            builder.toUriString(),
            HttpMethod.GET,
            entity,
            AddressSearchResponse.class
        );
        
        return response.getBody();
    }
}
```

---

## 🎯 실제 사용 시나리오

### 회원가입 API 구현 예시

```java
// DTO
@Data
public class SignUpRequest {
    
    @NotBlank(message = ValidationConstants.MSG_REQUIRED)
    @Email(message = ValidationConstants.MSG_INVALID_EMAIL)
    private String email;
    
    @ValidPassword
    private String password;
    
    @PhoneNumber
    private String phone;
    
    @ValidEnum(enumClass = Gender.class, allowNull = true)
    private String gender;
}

// 컨트롤러
@RestController
@RequestMapping(ApiConstants.AUTH_BASE)
@Slf4j
public class AuthController {
    
    @PostMapping(ApiConstants.AUTH_REGISTER)
    @TimeExecution(warnThreshold = 2000)
    @LoggingTrace
    @RateLimited(limit = 5, duration = 10, timeUnit = TimeUnit.MINUTES)
    public ResponseEntity<ApiResponse<UserDto>> signUp(
        @Valid @RequestBody SignUpRequest request
    ) {
        try {
            UserDto user = authService.signUp(request);
            return ResponseEntity.ok(
                ApiResponse.success(user, "회원가입이 완료되었습니다")
            );
        } catch (BusinessException e) {
            return ResponseEntity.badRequest().body(
                ApiResponse.error(e.getErrorCode())
            );
        }
    }
}

// 서비스
@Service
@Transactional
public class AuthService {
    
    @Autowired
    private RedisCacheService cacheService;
    
    @Autowired
    private RestTemplate restTemplate;
    
    @Retry(maxAttempts = 3, retryFor = {DataAccessException.class})
    public UserDto signUp(SignUpRequest request) {
        // 중복 체크
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
        
        // 사용자 생성
        User user = new User();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        
        User saved = userRepository.save(user);
        
        // 캐시에 저장
        cacheService.put(
            CacheConstants.KEY_PREFIX_USER + saved.getId(),
            saved,
            CacheConstants.LONG_TTL_SECONDS,
            TimeUnit.SECONDS
        );
        
        // 외부 API 호출 (환영 이메일 발송)
        sendWelcomeEmail(saved.getEmail());
        
        return UserDto.from(saved);
    }
    
    private void sendWelcomeEmail(String email) {
        EmailRequest request = new EmailRequest();
        request.setTo(email);
        request.setSubject("환영합니다!");
        request.setTemplate("welcome");
        
        // RestTemplate으로 이메일 서비스 호출
        restTemplate.postForObject(
            "https://api.email-service.com/send",
            request,
            EmailResponse.class
        );
    }
}
```

---

## 📝 팀 개발 규칙

1. **모든 API 응답은 `ApiResponse`로 감싸서 반환**
2. **비즈니스 로직 예외는 `BusinessException` 사용**
3. **검증이 필요한 DTO 필드는 커스텀 어노테이션 활용**
4. **외부 API 호출은 `@Retry` 어노테이션 필수**
5. **중요 메소드는 `@LoggingTrace`로 추적**
6. **성능이 중요한 메소드는 `@TimeExecution`으로 모니터링**
7. **모든 엔티티는 적절한 베이스 엔티티 상속**
8. **상수는 Constants 클래스에서 중앙 관리**
9. **캐시 가능한 데이터는 Redis 활용**
10. **외부 API 호출은 RestTemplate 사용**

---

## 🔧 설정 요구사항

### 필수 의존성
- Spring Boot 3.x
- Spring AOP
- Spring Validation
- Spring Data JPA
- Spring Data Redis
- Apache HttpClient 5
- Lombok

### application.yml 설정
```yaml
spring:
  jpa:
    properties:
      hibernate:
        jdbc:
          batch_size: 100
  
  redis:
    host: localhost
    port: 6379
    lettuce:
      pool:
        max-active: 10
        max-idle: 10
        min-idle: 5
        
  cache:
    type: redis
    redis:
      time-to-live: 3600000  # 1시간
    
logging:
  level:
    com.pullit.common.aspect: DEBUG
    com.pullit.common.http: DEBUG
```

---

작성일: 2024-01-10  
수정일: 2024-01-11  
버전: 2.0.0  
작성자: Pullit 개발팀