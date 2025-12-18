# 패키지 구조 설명

## 📁 `com.auction.auction` 패키지 구조

```
src/main/java/com/auction/auction/
│
├── AuctionApplication.java          # Spring Boot 메인 애플리케이션 클래스
│
├── config/                          # 설정 클래스 모음
│   ├── SecurityConfig.java         # Spring Security 보안 설정
│   ├── WebConfig.java               # 다국어(i18n), CORS 등 웹 설정
│   └── WebSocketConfig.java         # WebSocket 실시간 통신 설정
│
├── controller/                      # 컨트롤러 클래스 모음 (Presentation Layer)
│   ├── AuctionController.java       # 경매 페이지 컨트롤러 (View)
│   ├── ItemController.java          # 물건 REST API 컨트롤러
│   ├── UserController.java          # 사용자 REST API 컨트롤러
│   ├── ViewController.java          # 메인/로그인/회원가입 등 View 컨트롤러
│   ├── WebSocketAuctionController.java  # WebSocket 실시간 입찰 컨트롤러
│   └── FileUploadController.java    # 이미지 업로드 컨트롤러
│
├── dto/                             # Data Transfer Object (데이터 전송 객체)
│   ├── ItemRequest.java             # 물건 등록/수정 요청 DTO
│   ├── ItemResponse.java            # 물건 응답 DTO
│   ├── LoginRequest.java            # 로그인 요청 DTO
│   ├── LoginResponse.java           # 로그인 응답 DTO
│   ├── SignupRequest.java           # 회원가입 요청 DTO
│   └── UserResponse.java            # 사용자 응답 DTO
│
├── exception/                       # 예외 처리 클래스 모음
│   └── GlobalExceptionHandler.java  # 전역 예외 핸들러 (@ControllerAdvice)
│
├── filter/                          # 필터 클래스 모음
│   └── JwtAuthenticationFilter.java # JWT 인증 필터 (현재 미사용)
│
├── model/                           # 엔티티 클래스 모음 (Data Layer)
│   ├── User.java                    # 사용자 엔티티
│   ├── Item.java                    # 물건 엔티티
│   ├── Bid.java                     # 입찰 엔티티
│   └── AuctionParticipant.java      # 경매 참가자 엔티티
│
├── repository/                      # JPA Repository 인터페이스 모음 (Data Access Layer)
│   ├── UserRepository.java          # 사용자 데이터 접근
│   ├── ItemRepository.java          # 물건 데이터 접근
│   ├── BidRepository.java           # 입찰 데이터 접근
│   └── AuctionParticipantRepository.java  # 경매 참가자 데이터 접근
│
├── scheduler/                       # 스케줄러 클래스 모음
│   └── AuctionStatusScheduler.java  # 경매 자동 종료 스케줄러
│
├── service/                         # 서비스 클래스 모음 (Business Logic Layer)
│   ├── UserService.java             # 사용자 비즈니스 로직
│   ├── ItemService.java             # 물건 비즈니스 로직
│   ├── AuctionService.java          # 경매 비즈니스 로직
│   └── CustomUserDetailsService.java  # Spring Security 인증 서비스
│
└── util/                            # 유틸리티 클래스 모음
    └── JwtUtil.java                 # JWT 토큰 생성/검증 유틸 (현재 미사용)
```

---

## 📦 각 패키지별 상세 설명

### 1. `config` - 설정 클래스

**역할**: 애플리케이션의 전역 설정을 담당

#### SecurityConfig.java
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // Spring Security 인증/인가 설정
    // - URL 기반 접근 권한 제어
    // - 로그인/로그아웃 설정
    // - 비밀번호 암호화 (BCrypt)
    // - CSRF 보호
}
```

#### WebConfig.java
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {
    // 웹 관련 설정
    // - 다국어(i18n) LocaleResolver
    // - CORS 설정
    // - 인터셉터 등록
}
```

#### WebSocketConfig.java
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig {
    // WebSocket 실시간 통신 설정
    // - STOMP 메시지 브로커 설정
    // - WebSocket 엔드포인트 등록 (/ws)
}
```

---

### 2. `controller` - 컨트롤러 클래스

**역할**: HTTP 요청을 받아 처리하고 응답을 반환 (Presentation Layer)

#### ViewController.java
```java
@Controller  // View를 반환
public class ViewController {
    // 화면(HTML) 반환 컨트롤러
    // - GET /          → index.html (메인 페이지)
    // - GET /login     → login.html
    // - GET /signup    → signup.html
}
```

#### ItemController.java
```java
@RestController  // JSON 데이터 반환
@RequestMapping("/api/items")
public class ItemController {
    // 물건 관련 REST API
    // - GET    /api/items          → 물건 목록 조회
    // - GET    /api/items/{id}     → 물건 상세 조회
    // - POST   /api/items          → 물건 등록
    // - PUT    /api/items/{id}     → 물건 수정
    // - DELETE /api/items/{id}     → 물건 삭제
}
```

#### UserController.java
```java
@RestController
@RequestMapping("/api/users")
public class UserController {
    // 사용자 관련 REST API
    // - POST /api/users/signup     → 회원가입
    // - GET  /api/users/profile    → 프로필 조회
    // - PUT  /api/users/profile    → 프로필 수정
}
```

#### AuctionController.java
```java
@Controller
public class AuctionController {
    // 경매 관련 View 컨트롤러
    // - GET /auction-room/{itemId} → 경매방 페이지
    // - GET /auction-rooms         → 내 경매방 목록
}
```

#### WebSocketAuctionController.java
```java
@Controller
public class WebSocketAuctionController {
    // WebSocket 실시간 입찰 처리
    // - @MessageMapping("/bid/{itemId}")  → 입찰 요청 처리
    // - /topic/auction/{itemId}로 브로드캐스트
}
```

#### FileUploadController.java
```java
@RestController
@RequestMapping("/api")
public class FileUploadController {
    // 이미지 업로드 처리
    // - POST /api/upload  → 이미지 파일 업로드
}
```

---

### 3. `dto` - Data Transfer Object

**역할**: 계층 간 데이터 전송을 위한 객체

#### 요청 DTO (Request)
```java
// 클라이언트 → 서버로 전송되는 데이터
public class ItemRequest {
    private String title;
    private String description;
    private Integer startPrice;
    private LocalDateTime endTime;
    private String imageUrl;
}

public class LoginRequest {
    private String username;
    private String password;
}

public class SignupRequest {
    private String username;
    private String password;
    private String email;
    private String name;
}
```

#### 응답 DTO (Response)
```java
// 서버 → 클라이언트로 전송되는 데이터
public class ItemResponse {
    private Long id;
    private String title;
    private Integer currentPrice;
    private ItemStatus status;
    private String sellerName;
    // Entity를 직접 노출하지 않고 필요한 필드만 선택
}

public class LoginResponse {
    private String token;  // JWT 토큰
    private String username;
}
```

**DTO를 사용하는 이유:**
1. **보안**: Entity의 민감한 정보(비밀번호 등) 노출 방지
2. **성능**: 필요한 데이터만 전송
3. **유연성**: API 변경 시 Entity에 영향 없음

---

### 4. `exception` - 예외 처리

**역할**: 애플리케이션 전역 예외 처리

#### GlobalExceptionHandler.java
```java
@ControllerAdvice  // 모든 컨트롤러에 적용
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<?> handleNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(404).body(e.getMessage());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(IllegalArgumentException e) {
        return ResponseEntity.status(400).body(e.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception e) {
        return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
    }
}
```

**장점:**
- 중복 코드 제거 (각 컨트롤러마다 try-catch 불필요)
- 일관된 에러 응답 형식
- 유지보수 용이

---

### 5. `filter` - 필터

**역할**: HTTP 요청/응답 전처리/후처리

#### JwtAuthenticationFilter.java
```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // JWT 토큰 검증 필터
    // 현재 프로젝트에서는 세션 기반 인증을 사용하므로 미사용
    // JWT 방식으로 전환 시 사용 가능
}
```

**필터 동작 순서:**
```
클라이언트 요청
    ↓
필터 체인 (Filter)
    ↓
DispatcherServlet
    ↓
컨트롤러 (Controller)
```

---

### 6. `model` - 엔티티 (도메인 모델)

**역할**: 데이터베이스 테이블과 매핑되는 클래스 (JPA Entity)

#### User.java
```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;
    private String password;  // BCrypt 암호화
    private String email;
    private String name;

    // 양방향 관계
    @OneToMany(mappedBy = "seller")
    private List<Item> sellingItems;  // 판매 중인 물건

    @OneToMany(mappedBy = "bidder")
    private List<Bid> bids;  // 입찰 내역
}
```

#### Item.java
```java
@Entity
@Table(name = "items")
public class Item {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String description;
    private Integer startPrice;
    private Integer currentPrice;
    private LocalDateTime endTime;
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    private ItemStatus status;  // ACTIVE, CLOSED

    @ManyToOne
    @JoinColumn(name = "seller_id")
    private User seller;

    @ManyToOne
    @JoinColumn(name = "winner_id")
    private User currentWinner;

    @OneToMany(mappedBy = "item", cascade = CascadeType.ALL)
    private List<Bid> bids;  // 입찰 내역
}
```

#### Bid.java
```java
@Entity
@Table(name = "bids")
public class Bid {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Integer bidAmount;
    private LocalDateTime bidTime;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    @ManyToOne
    @JoinColumn(name = "bidder_id")
    private User bidder;
}
```

#### AuctionParticipant.java
```java
@Entity
@Table(name = "auction_participants")
public class AuctionParticipant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "item_id")
    private Item item;

    private LocalDateTime joinedAt;
}
```

**엔티티 관계:**
```
User (1) ──── (N) Item (판매자)
User (1) ──── (N) Bid (입찰자)
Item (1) ──── (N) Bid
User (N) ──── (N) Item (경매 참가자, 중간 테이블: AuctionParticipant)
```

---

### 7. `repository` - 데이터 접근 계층

**역할**: 데이터베이스 CRUD 작업 (Data Access Layer)

#### UserRepository.java
```java
@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
}
```

#### ItemRepository.java
```java
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByStatus(ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.seller.id = :sellerId")
    List<Item> findBySellerId(@Param("sellerId") Long sellerId);

    @Query("SELECT DISTINCT i FROM Item i JOIN i.bids b WHERE b.bidder.id = :userId")
    List<Item> findByBidderId(@Param("userId") Long userId);
}
```

#### BidRepository.java
```java
@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByItemIdOrderByBidTimeDesc(Long itemId);
    Optional<Bid> findTopByItemIdOrderByBidAmountDesc(Long itemId);
}
```

**Spring Data JPA 장점:**
- 메소드 이름으로 쿼리 자동 생성 (`findByUsername`)
- 복잡한 쿼리는 `@Query`로 작성
- 페이징, 정렬 자동 지원

---

### 8. `scheduler` - 스케줄러

**역할**: 주기적으로 실행되는 작업

#### AuctionStatusScheduler.java
```java
@Component
public class AuctionStatusScheduler {

    @Autowired
    private ItemRepository itemRepository;

    // 매 1분마다 실행
    @Scheduled(fixedRate = 60000)
    public void updateExpiredAuctions() {
        LocalDateTime now = LocalDateTime.now();
        List<Item> activeItems = itemRepository.findByStatus(ItemStatus.ACTIVE);

        for (Item item : activeItems) {
            if (item.getEndTime().isBefore(now)) {
                item.setStatus(ItemStatus.CLOSED);
                itemRepository.save(item);
                System.out.println("경매 종료: " + item.getTitle());
            }
        }
    }
}
```

**활용:**
- 경매 자동 종료
- 배치 작업 (데이터 정리, 통계 생성 등)

---

### 9. `service` - 비즈니스 로직 계층

**역할**: 핵심 비즈니스 로직 처리 (Business Logic Layer)

#### UserService.java
```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(SignupRequest request) {
        // 1. 유효성 검증
        if (userRepository.existsByUsername(request.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
        }

        // 2. 비밀번호 암호화
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(request.getEmail());
        user.setName(request.getName());

        // 3. 저장
        return userRepository.save(user);
    }
}
```

#### AuctionService.java
```java
@Service
public class AuctionService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BidRepository bidRepository;

    @Transactional
    public Bid placeBid(Long itemId, Long userId, Integer bidAmount) {
        // 1. 데이터 조회
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("물건을 찾을 수 없습니다."));

        // 2. 비즈니스 로직 검증
        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalStateException("경매가 진행 중이 아닙니다.");
        }

        if (bidAmount <= item.getCurrentPrice()) {
            throw new IllegalArgumentException("현재가보다 높은 금액을 입력하세요.");
        }

        // 3. 입찰 처리
        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(user);
        bid.setBidAmount(bidAmount);
        bid.setBidTime(LocalDateTime.now());

        item.setCurrentPrice(bidAmount);
        item.setCurrentWinner(user);

        bidRepository.save(bid);
        itemRepository.save(item);

        return bid;
    }
}
```

#### CustomUserDetailsService.java
```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) {
        // Spring Security 인증 시 호출
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다."));

        return org.springframework.security.core.userdetails.User
            .withUsername(user.getUsername())
            .password(user.getPassword())
            .roles("USER")
            .build();
    }
}
```

**서비스 계층의 특징:**
- `@Transactional`: 트랜잭션 관리 (DB 작업 원자성 보장)
- 비즈니스 로직 검증
- 여러 Repository를 조합하여 사용

---

### 10. `util` - 유틸리티

**역할**: 공통으로 사용되는 헬퍼 메소드

#### JwtUtil.java
```java
@Component
public class JwtUtil {
    // JWT 토큰 생성/검증 유틸리티
    // 현재 프로젝트에서는 세션 기반 인증 사용으로 미사용
    // 향후 JWT 방식으로 전환 시 사용 가능

    public String generateToken(String username) { ... }
    public boolean validateToken(String token) { ... }
    public String getUsernameFromToken(String token) { ... }
}
```

---

## 🔄 계층 간 호출 흐름

```
HTTP 요청
    ↓
Controller (Presentation Layer)
    - HTTP 요청 수신
    - 데이터 검증
    - Service 호출
    ↓
Service (Business Logic Layer)
    - 비즈니스 로직 처리
    - 트랜잭션 관리
    - Repository 호출
    ↓
Repository (Data Access Layer)
    - 데이터베이스 접근
    - CRUD 연산
    ↓
Database (MySQL)
```

**예시: 입찰 요청 흐름**
```
1. WebSocketAuctionController
   - @MessageMapping("/bid/{itemId}")
   - 입찰 요청 수신

2. AuctionService
   - placeBid(itemId, userId, bidAmount)
   - 비즈니스 로직 검증
   - 입찰 처리

3. ItemRepository, BidRepository
   - save() 메소드로 데이터 저장

4. WebSocketAuctionController
   - /topic/auction/{itemId}로 결과 브로드캐스트
```

---

## 📚 패키지 설계 원칙

### 1. 단일 책임 원칙 (SRP)
- 각 클래스는 하나의 책임만 가짐
- Controller: HTTP 처리
- Service: 비즈니스 로직
- Repository: 데이터 접근

### 2. 의존성 역전 원칙 (DIP)
- 상위 계층이 하위 계층에 의존
- Controller → Service → Repository
- 인터페이스를 통한 느슨한 결합

### 3. 관심사 분리 (SoC)
- 각 패키지는 명확한 역할 분리
- 유지보수 및 테스트 용이성 향상

---

## 정리

| 패키지 | 역할 | 주요 기술 |
|--------|------|----------|
| **config** | 애플리케이션 설정 | Spring Security, WebSocket, i18n |
| **controller** | HTTP 요청/응답 처리 | @Controller, @RestController, @MessageMapping |
| **dto** | 데이터 전송 객체 | Request/Response DTO |
| **exception** | 예외 처리 | @ControllerAdvice, @ExceptionHandler |
| **filter** | 요청/응답 전처리 | OncePerRequestFilter (JWT 필터) |
| **model** | 도메인 모델 (Entity) | @Entity, JPA 관계 매핑 |
| **repository** | 데이터 접근 | Spring Data JPA, @Repository |
| **scheduler** | 주기적 작업 | @Scheduled (경매 자동 종료) |
| **service** | 비즈니스 로직 | @Service, @Transactional |
| **util** | 유틸리티 | JWT, 공통 헬퍼 메소드 |

이 구조는 **3계층 아키텍처** (Presentation - Business - Data)를 따르며, Spring Boot의 모범 사례(Best Practice)를 준수합니다.
