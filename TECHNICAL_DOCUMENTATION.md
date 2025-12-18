# 경매 시스템 기술 문서

## 프로젝트 개요
Spring Boot 기반 실시간 중고 물품 경매 시스템

---

## 1. RESTful API

### 구현 방법
- Spring Boot의 `@RestController`와 `@RequestMapping` 어노테이션 사용
- HTTP 메소드(GET, POST, PUT, DELETE)를 활용한 리소스 기반 URL 설계

### 주요 API 엔드포인트

#### 물건 관리 API
```java
// ItemController.java
@RestController
@RequestMapping("/api/items")
public class ItemController {

    @GetMapping  // 물건 목록 조회
    public ResponseEntity<List<ItemResponse>> getAllItems()

    @GetMapping("/{id}")  // 특정 물건 조회
    public ResponseEntity<ItemResponse> getItem(@PathVariable Long id)

    @PostMapping  // 물건 등록
    public ResponseEntity<Item> createItem(@RequestBody ItemRequest request)

    @PutMapping("/{id}")  // 물건 수정
    public ResponseEntity<Item> updateItem(@PathVariable Long id, @RequestBody ItemRequest request)

    @DeleteMapping("/{id}")  // 물건 삭제
    public ResponseEntity<Void> deleteItem(@PathVariable Long id)
}
```

#### 사용자 API
```java
// UserController.java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @PostMapping("/signup")  // 회원가입
    public ResponseEntity<User> signup(@RequestBody SignupRequest request)

    @GetMapping("/profile")  // 프로필 조회
    public ResponseEntity<User> getProfile()

    @PutMapping("/profile")  // 프로필 수정
    public ResponseEntity<User> updateProfile(@RequestBody UpdateProfileRequest request)
}
```

#### 입찰 API
```java
// AuctionService.java
@Service
public class AuctionService {

    @Transactional
    public Bid placeBid(Long itemId, Long userId, Integer bidAmount)

    public List<Bid> getBidHistory(Long itemId)

    public Item getItemWithBids(Long itemId)
}
```

### RESTful 원칙 준수
- **리소스 기반 URL**: `/api/items`, `/api/users`
- **HTTP 메소드 의미**: GET(조회), POST(생성), PUT(수정), DELETE(삭제)
- **상태 코드**: 200(성공), 201(생성), 400(잘못된 요청), 404(없음), 500(서버 오류)
- **JSON 형식**: 요청/응답 데이터 형식 통일

---

## 2. 예외 처리 (Exception Handling)

### 구현 방법
- `@ControllerAdvice`를 사용한 전역 예외 처리
- 커스텀 예외 클래스 정의
- Spring Security 예외 처리

### 예외 처리 구조

#### 전역 예외 핸들러
```java
@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleEntityNotFound(EntityNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(new ErrorResponse(e.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(new ErrorResponse("서버 오류가 발생했습니다."));
    }
}
```

#### 서비스 레이어 예외 처리
```java
// AuctionService.java
@Transactional
public Bid placeBid(Long itemId, Long userId, Integer bidAmount) {
    Item item = itemRepository.findById(itemId)
        .orElseThrow(() -> new EntityNotFoundException("물건을 찾을 수 없습니다."));

    if (item.getStatus() != ItemStatus.ACTIVE) {
        throw new IllegalStateException("경매가 진행 중이 아닙니다.");
    }

    if (bidAmount <= item.getCurrentPrice()) {
        throw new IllegalArgumentException("현재가보다 높은 금액을 입력하세요.");
    }

    // 입찰 처리...
}
```

#### 컨트롤러 예외 처리
```java
// ItemController.java
@PostMapping
public ResponseEntity<?> createItem(@RequestBody ItemRequest request) {
    try {
        Item item = itemService.createItem(request);
        return ResponseEntity.ok(item);
    } catch (IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    } catch (Exception e) {
        return ResponseEntity.status(500).body("서버 오류가 발생했습니다.");
    }
}
```

---

## 3. 다국어 지원 (Internationalization, i18n)

### 구현 방법
- Spring의 `MessageSource`와 `LocaleResolver` 사용
- Thymeleaf의 `#{...}` 표현식으로 메시지 출력
- 쿠키 기반 언어 설정 저장

### 설정 파일

#### WebConfig.java
```java
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver();
        resolver.setDefaultLocale(Locale.KOREAN);
        resolver.setCookieName("lang");
        resolver.setCookieMaxAge(3600);
        return resolver;
    }

    @Bean
    public LocaleChangeInterceptor localeChangeInterceptor() {
        LocaleChangeInterceptor interceptor = new LocaleChangeInterceptor();
        interceptor.setParamName("lang");
        return interceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(localeChangeInterceptor());
    }
}
```

#### 메시지 파일 구조
```
resources/messages/
├── i18n.properties          (기본, 한국어)
├── i18n_ko.properties       (한국어)
└── i18n_en.properties       (영어)
```

#### 메시지 파일 예시
```properties
# i18n_ko.properties
nav.items=경매 목록
nav.register=물건 등록
nav.login=로그인
nav.signup=회원가입
register.title=물건 등록
register.success=물건이 등록되었습니다.

# i18n_en.properties
nav.items=Auction List
nav.register=Register Item
nav.login=Login
nav.signup=Sign Up
register.title=Register Item
register.success=Item has been registered successfully.
```

#### Thymeleaf 템플릿에서 사용
```html
<!-- register.html -->
<h1 th:text="#{register.title}">물건 등록</h1>
<label th:text="#{register.item.title}">제목</label>
<button th:text="#{register.submit}">등록하기</button>
```

#### JavaScript에서 사용
```javascript
const i18n = {
    success: /*[[#{register.success}]]*/ '물건이 등록되었습니다.',
    error: /*[[#{register.error}]]*/ '등록 실패'
};
```

---

## 4. 보안 (Spring Security)

### 구현 방법
- Spring Security를 사용한 인증/인가
- 세션 기반 인증
- BCrypt 암호화
- CSRF 보호

### SecurityConfig.java
```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/signup", "/css/**", "/js/**").permitAll()
                .requestMatchers("/api/items").permitAll()  // 목록 조회 허용
                .requestMatchers("/items/register", "/api/items/**").authenticated()  // 등록/수정은 인증 필요
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .defaultSuccessUrl("/")
                .failureUrl("/login?error=true")
                .permitAll()
            )
            .logout(logout -> logout
                .logoutSuccessUrl("/")
                .permitAll()
            )
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/api/**")  // REST API는 CSRF 제외
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();  // BCrypt 암호화
    }
}
```

### 사용자 인증 서비스
```java
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
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

### 비밀번호 암호화
```java
// UserService.java
@Service
public class UserService {

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User createUser(SignupRequest request) {
        User user = new User();
        user.setUsername(request.getUsername());
        user.setPassword(passwordEncoder.encode(request.getPassword()));  // BCrypt 암호화
        user.setEmail(request.getEmail());
        return userRepository.save(user);
    }

    public void updatePassword(Long userId, String currentPassword, String newPassword) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 현재 비밀번호 확인
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
        }

        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }
}
```

### 주요 보안 기능
1. **인증 (Authentication)**: 로그인 기능으로 사용자 신원 확인
2. **인가 (Authorization)**: URL 기반 접근 권한 제어
3. **비밀번호 암호화**: BCrypt로 단방향 해시 암호화
4. **세션 관리**: 로그인 상태 유지
5. **CSRF 보호**: 폼 기반 요청에 CSRF 토큰 자동 적용

---

## 5. 3계층 구조 (Layered Architecture)

### 구조 설명
```
Presentation Layer (Controller)
    ↓
Business Logic Layer (Service)
    ↓
Data Access Layer (Repository)
```

### 각 계층별 역할

#### 1. Presentation Layer (컨트롤러)
- **역할**: HTTP 요청/응답 처리, 데이터 검증
- **기술**: `@Controller`, `@RestController`

```java
@RestController
@RequestMapping("/api/items")
public class ItemController {

    @Autowired
    private ItemService itemService;  // Service 계층 의존

    @GetMapping("/{id}")
    public ResponseEntity<ItemResponse> getItem(@PathVariable Long id) {
        Item item = itemService.getItemById(id);  // Service 호출
        return ResponseEntity.ok(new ItemResponse(item));
    }
}
```

#### 2. Business Logic Layer (서비스)
- **역할**: 비즈니스 로직 처리, 트랜잭션 관리
- **기술**: `@Service`, `@Transactional`

```java
@Service
public class AuctionService {

    @Autowired
    private ItemRepository itemRepository;  // Repository 계층 의존

    @Autowired
    private BidRepository bidRepository;

    @Transactional
    public Bid placeBid(Long itemId, Long userId, Integer bidAmount) {
        // 1. 데이터 조회 (Repository 사용)
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new EntityNotFoundException("물건을 찾을 수 없습니다."));

        User user = userRepository.findById(userId)
            .orElseThrow(() -> new EntityNotFoundException("사용자를 찾을 수 없습니다."));

        // 2. 비즈니스 로직 검증
        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalStateException("경매가 진행 중이 아닙니다.");
        }

        if (bidAmount <= item.getCurrentPrice()) {
            throw new IllegalArgumentException("현재가보다 높은 금액을 입력하세요.");
        }

        // 3. 데이터 저장
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

#### 3. Data Access Layer (리포지토리)
- **역할**: 데이터베이스 접근, CRUD 연산
- **기술**: Spring Data JPA `@Repository`

```java
@Repository
public interface ItemRepository extends JpaRepository<Item, Long> {
    List<Item> findByStatus(ItemStatus status);

    @Query("SELECT i FROM Item i WHERE i.seller.id = :userId")
    List<Item> findBySellerId(@Param("userId") Long userId);

    @Query("SELECT i FROM Item i JOIN i.bids b WHERE b.bidder.id = :userId")
    List<Item> findByBidderId(@Param("userId") Long userId);
}

@Repository
public interface BidRepository extends JpaRepository<Bid, Long> {
    List<Bid> findByItemIdOrderByBidTimeDesc(Long itemId);
}
```

### 계층 간 의존성
```
Controller → Service → Repository
     ↓          ↓           ↓
   View     Business     Database
          Processing
```

### 3계층 구조의 장점
1. **관심사 분리**: 각 계층이 명확한 책임을 가짐
2. **유지보수성**: 한 계층의 변경이 다른 계층에 영향을 최소화
3. **테스트 용이성**: 각 계층을 독립적으로 테스트 가능
4. **재사용성**: Service 계층은 여러 Controller에서 재사용 가능

---

## 6. WebSocket (실시간 경매)

### 구현 방법
- Spring WebSocket과 STOMP 프로토콜 사용
- SockJS를 통한 브라우저 호환성 확보
- 메시지 브로커를 통한 pub/sub 패턴 구현

### WebSocketConfig.java
```java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic");  // 메시지 브로커 prefix
        config.setApplicationDestinationPrefixes("/app");  // 애플리케이션 prefix
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")  // WebSocket 엔드포인트
                .setAllowedOrigins("*")
                .withSockJS();  // SockJS fallback
    }
}
```

### WebSocket 컨트롤러
```java
@Controller
public class WebSocketAuctionController {

    @Autowired
    private AuctionService auctionService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // 클라이언트 → 서버: 입찰 요청
    @MessageMapping("/bid/{itemId}")
    public void handleBid(@DestinationVariable Long itemId,
                         @Payload BidRequest bidRequest,
                         Principal principal) {
        try {
            // 1. 입찰 처리
            User user = ((CustomUserDetails) ((Authentication) principal).getPrincipal()).getUser();
            Bid bid = auctionService.placeBid(itemId, user.getId(), bidRequest.getBidAmount());

            // 2. 모든 구독자에게 브로드캐스트
            BidResponse response = new BidResponse(
                bid.getBidAmount(),
                user.getUsername(),
                bid.getBidTime(),
                true
            );

            messagingTemplate.convertAndSend("/topic/auction/" + itemId, response);

        } catch (Exception e) {
            // 에러 메시지 전송
            BidResponse errorResponse = new BidResponse(null, null, null, false, e.getMessage());
            messagingTemplate.convertAndSendToUser(
                principal.getName(),
                "/queue/errors",
                errorResponse
            );
        }
    }
}
```

### 클라이언트 (JavaScript)
```javascript
// auction-room.html
let stompClient = null;
let itemId = /*[[${item.id}]]*/ 1;

// WebSocket 연결
function connect() {
    const socket = new SockJS('/auction/ws');
    stompClient = Stomp.over(socket);

    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);

        // 특정 경매방 구독
        stompClient.subscribe('/topic/auction/' + itemId, function(message) {
            const bidData = JSON.parse(message.body);
            updateAuctionUI(bidData);  // UI 업데이트
        });
    });
}

// 입찰하기
function placeBid() {
    const bidAmount = document.getElementById('bidAmount').value;

    stompClient.send('/app/bid/' + itemId, {}, JSON.stringify({
        bidAmount: parseInt(bidAmount)
    }));
}

// UI 업데이트
function updateAuctionUI(bidData) {
    if (bidData.success) {
        document.getElementById('currentPrice').textContent =
            bidData.bidAmount.toLocaleString() + '원';
        document.getElementById('currentWinner').textContent = bidData.bidderName;

        // 입찰 내역 추가
        const historyItem = `
            <div class="bid-item">
                <span>${bidData.bidderName}</span>
                <span>${bidData.bidAmount.toLocaleString()}원</span>
                <span>${new Date(bidData.bidTime).toLocaleTimeString()}</span>
            </div>
        `;
        document.getElementById('bidHistory').insertAdjacentHTML('afterbegin', historyItem);
    } else {
        alert('입찰 실패: ' + bidData.message);
    }
}

// 페이지 로드 시 연결
window.onload = connect;
```

### WebSocket 주요 기능
1. **실시간 입찰 업데이트**: 한 사용자가 입찰하면 모든 참가자에게 즉시 반영
2. **양방향 통신**: 서버 ↔ 클라이언트 실시간 메시지 교환
3. **경매방별 채널**: `/topic/auction/{itemId}`로 경매방 격리
4. **에러 처리**: 입찰 실패 시 개별 사용자에게만 에러 메시지 전송

### 메시지 흐름
```
사용자 A: 입찰 → /app/bid/123 → 서버 → 입찰 처리
                                        ↓
                          /topic/auction/123 (브로드캐스트)
                                        ↓
                          사용자 A, B, C 모두 수신 → UI 업데이트
```

---

## 7. AI 가격 추천 (Python Flask + 크롤링)

### 시스템 구조
```
Spring Boot (Java)  ←→  Flask API (Python)  →  당근마켓 크롤링
    ↓                        ↓                      ↓
register.html         price_analyzer.py        crawler.py
```

### 구현 방법

#### 1. Flask REST API 서버 (app.py)
```python
from flask import Flask, request, jsonify
from flask_cors import CORS
from crawler import DaangnCrawler
from price_analyzer import PriceAnalyzer

app = Flask(__name__)
CORS(app)  # Spring Boot에서 호출 가능하도록 CORS 허용

crawler = DaangnCrawler()
analyzer = PriceAnalyzer()

@app.route('/api/recommend-price', methods=['POST'])
def recommend_price():
    """
    가격 추천 API

    Request:  {"title": "아이폰 13", "description": "3년 사용, 스크래치 있음"}
    Response: {"success": true, "data": {...}}
    """
    try:
        data = request.get_json()
        title = data.get('title', '').strip()
        description = data.get('description', '').strip()

        if not title:
            return jsonify({'success': False, 'error': 'Title is required'}), 400

        # 1. 크롤링으로 가격 데이터 수집
        prices = crawler.get_price_data(title, description)

        # 2. 가격 분석 및 추천
        result = analyzer.analyze(prices, description)

        return jsonify({'success': True, 'data': result})

    except Exception as e:
        return jsonify({'success': False, 'error': str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000, debug=True)
```

#### 2. 웹 크롤러 (crawler.py)
```python
import requests
from bs4 import BeautifulSoup
import json
from typing import List, Dict

class DaangnCrawler:
    def __init__(self):
        self.base_url = "https://www.daangn.com"
        self.headers = {
            'User-Agent': 'Mozilla/5.0 ...'
        }

    def extract_keywords(self, title: str, description: str) -> str:
        """
        제목에서 검색 키워드 추출
        설명은 상태 분석에만 사용하고 검색어에는 영향을 주지 않음
        """
        keywords = title.strip()
        return keywords

    def search_items(self, keywords: str, max_results: int = 10) -> List[Dict]:
        """
        당근마켓에서 키워드로 물건 검색
        JSON-LD 구조화된 데이터에서 가격 정보 추출
        """
        search_url = f"{self.base_url}/search/{requests.utils.quote(keywords)}"
        response = requests.get(search_url, headers=self.headers, timeout=10)
        soup = BeautifulSoup(response.text, 'html.parser')

        items = []

        # JSON-LD script 태그 찾기
        json_ld_scripts = soup.find_all('script', type='application/ld+json')

        for script in json_ld_scripts:
            data = json.loads(script.string)

            # ItemList 타입 찾기
            if isinstance(data, dict) and data.get('@type') == 'ItemList':
                item_list = data.get('itemListElement', [])

                for item_data in item_list[:max_results]:
                    product = item_data.get('item', {})
                    title = product.get('name', '')
                    offers = product.get('offers', {})
                    price_str = offers.get('price', '0')

                    price = int(float(price_str))

                    if price > 0 and title:
                        items.append({'title': title, 'price': price})

        return items

    def get_price_data(self, title: str, description: str) -> List[int]:
        """제목과 설명으로 유사 물건들의 가격 리스트 반환"""
        keywords = self.extract_keywords(title, description)
        items = self.search_items(keywords, max_results=15)
        prices = [item['price'] for item in items if item['price'] > 0]
        return prices
```

#### 3. 가격 분석기 (price_analyzer.py)
```python
import statistics
import re
from typing import List, Dict

class PriceAnalyzer:
    def __init__(self):
        self.base_discount_rate = 0.1  # 기본 할인율 10%

    def detect_condition(self, description: str) -> float:
        """
        설명에서 물건 상태를 분석하여 추가 할인율 계산

        Returns: 추가 할인율 (0.0 ~ 0.3)
        """
        description_lower = description.lower()
        additional_discount = 0.0

        # 손상/결함 관련 키워드 (-15% 추가)
        damage_keywords = ['스크래치', '긁힘', '파손', '고장', '오류', '문제', '불량', '흠']
        if any(k in description_lower for k in damage_keywords):
            additional_discount += 0.15

        # 오래 사용 (-10% 추가)
        year_match = re.search(r'(\d+)\s*년', description_lower)
        if year_match:
            years = int(year_match.group(1))
            if years >= 2:
                additional_discount += 0.10

        # 배터리 문제 (-10% 추가)
        battery_keywords = ['배터리', '베터리']
        bad_battery_keywords = ['약함', '부족', '교체', '낮음', '소모']
        if any(b in description_lower for b in battery_keywords):
            if any(bad in description_lower for bad in bad_battery_keywords):
                additional_discount += 0.10

        return min(additional_discount, 0.3)  # 최대 30%

    def analyze(self, prices: List[int], description: str = "") -> Dict:
        """
        가격 리스트를 분석하여 추천가 계산

        Returns:
            {
                'recommended_price': int,
                'average_price': int,
                'min_price': int,
                'max_price': int,
                'count': int,
                'message': str
            }
        """
        if not prices:
            return {
                'recommended_price': 0,
                'average_price': 0,
                'min_price': 0,
                'max_price': 0,
                'count': 0,
                'message': '검색 결과가 없습니다.'
            }

        # 이상치 제거 (평균의 3배 이상인 값 제거)
        mean = statistics.mean(prices)
        filtered_prices = [p for p in prices if p <= mean * 3]

        if not filtered_prices:
            filtered_prices = prices

        avg_price = int(statistics.mean(filtered_prices))
        min_price = min(filtered_prices)
        max_price = max(filtered_prices)

        # 상태별 추가 할인율 계산
        condition_discount = self.detect_condition(description)
        total_discount = self.base_discount_rate + condition_discount

        # 추천가: 평균가 * (1 - 총 할인율)
        recommended = int(avg_price * (1 - total_discount))

        message = f'{len(filtered_prices)}개의 유사 물품을 분석했습니다.'
        if condition_discount > 0:
            message += f' (상태 분석: 추가 {int(condition_discount * 100)}% 할인 적용)'

        return {
            'recommended_price': recommended,
            'average_price': avg_price,
            'min_price': min_price,
            'max_price': max_price,
            'count': len(filtered_prices),
            'condition_discount': int(condition_discount * 100),
            'message': message
        }
```

#### 4. Spring Boot 연동 (register.html)
```javascript
// AI 가격 추천 받기
async function getPriceRecommendation() {
    const title = document.getElementById('title').value.trim();
    const description = document.getElementById('description').value.trim();

    if (!title) {
        alert('제목을 먼저 입력해주세요.');
        return;
    }

    // 로딩 표시
    const button = event.target;
    button.innerHTML = '⏳ 분석 중...';
    button.disabled = true;

    try {
        // Flask API 호출
        const response = await fetch('http://localhost:5000/api/recommend-price', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                title: title,
                description: description
            })
        });

        const result = await response.json();

        if (result.success && result.data) {
            // 결과 표시
            document.getElementById('recommendationMessage').textContent = result.data.message;
            document.getElementById('recommendedPrice').textContent =
                formatPrice(result.data.recommended_price);
            document.getElementById('averagePrice').textContent =
                formatPrice(result.data.average_price);
            document.getElementById('minPrice').textContent =
                formatPrice(result.data.min_price);
            document.getElementById('maxPrice').textContent =
                formatPrice(result.data.max_price);

            document.getElementById('priceRecommendation').style.display = 'block';
        }
    } catch (error) {
        console.error('Error:', error);
        showError('가격 추천 서버에 연결할 수 없습니다.');
    } finally {
        button.innerHTML = '💡 AI 가격 추천 받기';
        button.disabled = false;
    }
}

// 추천가 적용
function applyRecommendedPrice() {
    if (recommendationData && recommendationData.recommended_price > 0) {
        document.getElementById('startPrice').value =
            recommendationData.recommended_price;
        alert('추천 시작가가 적용되었습니다!');
    }
}
```

### AI 가격 추천 알고리즘

#### 1단계: 데이터 수집
- 사용자가 입력한 제목으로 당근마켓 검색
- JSON-LD 구조화된 데이터에서 가격 정보 추출
- 최대 15개 유사 물품 가격 수집

#### 2단계: 이상치 제거
- 평균의 3배를 초과하는 가격 제외
- 통계적 정확도 향상

#### 3단계: 기본 할인 적용
- 경매 특성상 기본 10% 할인

#### 4단계: 상태 분석 추가 할인
- **손상 키워드** (스크래치, 긁힘, 파손 등): +15% 할인
- **오래 사용** (2년 이상): +10% 할인
- **배터리 문제**: +10% 할인
- 최대 30% 추가 할인 (총 40% 할인 가능)

#### 5단계: 추천가 계산
```
추천가 = 평균가 × (1 - 총 할인율)
```

### 예시
```
제목: "아이폰 13"
설명: "3년 정도 사용하고 후면에 스크래치 있습니다"

1. 크롤링: 아이폰 13 관련 15개 물품 → 평균가 354,333원
2. 기본 할인: 10%
3. 상태 분석:
   - "3년" → +10%
   - "스크래치" → +15%
   - 총 추가 할인: 25%
4. 최종 할인율: 35%
5. 추천가: 354,333 × (1 - 0.35) = 230,216원
```

### AI 사용 기술
- **웹 크롤링**: BeautifulSoup4로 HTML 파싱
- **자연어 처리**: 정규표현식과 키워드 매칭으로 상태 분석
- **통계 분석**: 평균, 이상치 제거로 정확도 향상
- **머신러닝 개념**: 키워드 기반 할인율 학습 (규칙 기반)

---

## 8. 스케줄러 (경매 자동 종료)

### 구현 방법
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

---

## 9. 파일 업로드

### 구현 방법
```java
@RestController
@RequestMapping("/api")
public class FileController {

    @Value("${upload.path}")
    private String uploadPath;

    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            String filename = UUID.randomUUID().toString() + "_" + file.getOriginalFilename();
            Path path = Paths.get(uploadPath, filename);
            Files.write(path, file.getBytes());

            String url = "/uploads/" + filename;
            return ResponseEntity.ok(Map.of("url", url));
        } catch (IOException e) {
            return ResponseEntity.status(500).body(Map.of("error", "업로드 실패"));
        }
    }
}
```

---

## 기술 스택 요약

### Backend
- **Spring Boot 3.x**: 메인 프레임워크
- **Spring Security**: 인증/인가
- **Spring Data JPA**: ORM
- **Spring WebSocket**: 실시간 통신
- **MySQL**: 데이터베이스
- **Python Flask**: AI 가격 추천 API

### Frontend
- **Thymeleaf**: 템플릿 엔진
- **JavaScript (ES6)**: 클라이언트 로직
- **SockJS + STOMP**: WebSocket 클라이언트
- **HTML5 + CSS3**: UI

### AI/Data
- **BeautifulSoup4**: 웹 크롤링
- **Python Requests**: HTTP 클라이언트
- **정규표현식**: 텍스트 분석
- **통계 분석**: 가격 추천 알고리즘

---

## 주요 기능 정리

1. ✅ **RESTful API**: GET/POST/PUT/DELETE 기반 리소스 관리
2. ✅ **예외 처리**: @ControllerAdvice 전역 예외 핸들러
3. ✅ **다국어 지원**: MessageSource + LocaleResolver (한국어/영어)
4. ✅ **보안**: Spring Security + BCrypt 암호화
5. ✅ **3계층 구조**: Controller → Service → Repository
6. ✅ **WebSocket**: STOMP 기반 실시간 입찰 시스템
7. ✅ **AI 가격 추천**: Flask + 크롤링 + 상태 분석 알고리즘
8. ✅ **스케줄러**: 경매 자동 종료
9. ✅ **파일 업로드**: 이미지 업로드 및 저장
10. ✅ **트랜잭션 관리**: @Transactional로 데이터 일관성 보장
