# WebSocket 실시간 경매 시스템 실행 과정

## 개요

이 프로젝트는 **Spring WebSocket + STOMP 프로토콜**을 사용하여 실시간 경매 입찰 시스템을 구현했습니다.

---

## 전체 아키텍처

```
┌─────────────┐                   ┌─────────────┐
│  사용자 A   │                   │  사용자 B   │
│ (경매 참가) │                   │ (경매 참가) │
└──────┬──────┘                   └──────┬──────┘
       │                                  │
       │  WebSocket 연결                  │
       │  (SockJS + STOMP)               │
       ├──────────────┬───────────────────┤
       │              │                   │
       ▼              ▼                   ▼
┌─────────────────────────────────────────────┐
│           Spring Boot 서버                   │
│  ┌────────────────────────────────────┐    │
│  │  WebSocketConfig                    │    │
│  │  - Message Broker 설정              │    │
│  │  - STOMP Endpoint 설정              │    │
│  └────────────────────────────────────┘    │
│                    ↓                        │
│  ┌────────────────────────────────────┐    │
│  │  WebSocketAuctionController        │    │
│  │  - @MessageMapping("/auction/{id}/bid") │
│  │  - @SendTo("/topic/auction/{id}")  │    │
│  └────────────────────────────────────┘    │
│                    ↓                        │
│  ┌────────────────────────────────────┐    │
│  │  AuctionService                     │    │
│  │  - placeBid() 비즈니스 로직         │    │
│  └────────────────────────────────────┘    │
│                    ↓                        │
│  ┌────────────────────────────────────┐    │
│  │  Database (MySQL)                   │    │
│  │  - items, bids, users 테이블        │    │
│  └────────────────────────────────────┘    │
└─────────────────────────────────────────────┘
       │              │                   │
       │  Broadcast   │  Broadcast        │
       ▼              ▼                   ▼
┌──────────────┐ ┌──────────────┐ ┌──────────────┐
│  사용자 A    │ │  사용자 B    │ │  사용자 C    │
│  (UI 업데이트)│ │  (UI 업데이트)│ │  (UI 업데이트)│
└──────────────┘ └──────────────┘ └──────────────┘
```

---

## 1단계: WebSocket 설정 (WebSocketConfig.java)

### 1-1. 서버 설정

```java
@Configuration
@EnableWebSocketMessageBroker  // WebSocket 메시지 브로커 활성화
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        // ===== 메시지 브로커 설정 =====

        // 1. Simple Broker 활성화
        config.enableSimpleBroker("/topic", "/queue");
        // /topic: 브로드캐스트 메시지 (1:N, 모든 구독자에게 전송)
        // /queue: 개인 메시지 (1:1, 특정 사용자에게만 전송)

        // 2. Application Destination Prefix
        config.setApplicationDestinationPrefixes("/app");
        // 클라이언트가 서버로 메시지 보낼 때 사용하는 prefix
        // 예: /app/auction/123/bid

        // 3. User Destination Prefix
        config.setUserDestinationPrefix("/user");
        // 개인 메시지 전송 시 사용
        // 예: /user/queue/errors
    }

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // ===== STOMP 엔드포인트 등록 =====

        registry.addEndpoint("/ws-auction")  // WebSocket 연결 URL
                .setAllowedOriginPatterns("*")  // CORS 허용
                .withSockJS();  // SockJS fallback 지원

        // 최종 연결 URL: http://localhost:8080/auction/ws-auction
    }
}
```

### 1-2. STOMP란?

**STOMP (Simple Text Oriented Messaging Protocol)**
- WebSocket 위에서 동작하는 메시징 프로토콜
- pub/sub (발행/구독) 패턴 지원
- 텍스트 기반 프로토콜 (HTTP와 유사)

**STOMP 프레임 구조:**
```
COMMAND
header1:value1
header2:value2

Body^@
```

**예시:**
```
SEND
destination:/app/auction/123/bid
content-type:application/json

{"bidAmount":50000}^@
```

### 1-3. SockJS란?

**SockJS (Simple Javascript WebSocket)**
- WebSocket을 지원하지 않는 브라우저를 위한 Fallback 라이브러리
- WebSocket이 안 되면 자동으로 HTTP Polling으로 전환
- 개발자는 WebSocket API만 사용하면 됨 (자동 처리)

**동작 방식:**
```
1차 시도: WebSocket
2차 시도: HTTP Streaming
3차 시도: HTTP Long Polling
4차 시도: HTTP Polling
```

---

## 2단계: 클라이언트 연결 (auction-room.html)

### 2-1. 라이브러리 로드

```html
<!-- WebSocket 라이브러리 (CDN) -->
<script src="https://cdn.jsdelivr.net/npm/sockjs-client@1/dist/sockjs.min.js"></script>
<script src="https://cdnjs.cloudflare.com/ajax/libs/stomp.js/2.3.3/stomp.min.js"></script>
```

- **sockjs-client**: SockJS 클라이언트 라이브러리
- **stomp.js**: STOMP 프로토콜 JavaScript 클라이언트

### 2-2. WebSocket 연결

```javascript
let stompClient = null;

// WebSocket 연결 함수
function connect() {
    // 1. SockJS 객체 생성
    const socket = new SockJS(basePath + '/ws-auction');
    // basePath = '/auction'
    // 최종 URL: http://localhost:8080/auction/ws-auction

    // 2. STOMP 클라이언트 생성
    stompClient = Stomp.over(socket);

    // 3. 서버에 연결
    stompClient.connect({}, function(frame) {
        console.log('Connected: ' + frame);

        // ===== 4. 메시지 구독 =====

        // 4-1. 경매방 입찰 메시지 구독 (브로드캐스트)
        stompClient.subscribe('/topic/auction/' + itemId, function(message) {
            const bidMessage = JSON.parse(message.body);
            handleBidMessage(bidMessage);
        });
        // 예: /topic/auction/123
        // 이 경매방(itemId=123)의 모든 입찰 정보를 받음

        // 4-2. 개인 에러 메시지 구독 (개인 메시지)
        stompClient.subscribe('/user/queue/errors', function(message) {
            const errorMessage = JSON.parse(message.body);
            if (!errorMessage.success && errorMessage.errorMessage) {
                alert('입찰 실패: ' + errorMessage.errorMessage);
            }
        });
        // 자신에게만 전송되는 에러 메시지 수신

    }, function(error) {
        // 연결 실패 시 처리
        console.error('WebSocket connection error:', error);
        setTimeout(connect, 5000); // 5초 후 재연결 시도
    });
}

// 페이지 로드 시 자동 연결
window.onload = function() {
    connect();           // WebSocket 연결
    loadItemInfo();      // 물건 정보 로드
    loadBidHistory();    // 입찰 내역 로드
};
```

**연결 과정 (시퀀스 다이어그램):**
```
클라이언트                  서버
   |                        |
   |---- new SockJS() ----->|
   |                        | (WebSocket 핸드셰이크)
   |<----- 연결 승인 --------|
   |                        |
   |-- STOMP CONNECT ----->|
   |                        | (STOMP 프로토콜 연결)
   |<--- STOMP CONNECTED ---|
   |                        |
   |--- SUBSCRIBE --------->|
   | (/topic/auction/123)  |
   |<--- SUBSCRIBED --------|
```

---

## 3단계: 입찰 요청 (클라이언트 → 서버)

### 3-1. 입찰 폼 제출

```javascript
// 입찰 폼 제출 이벤트
document.getElementById('bidForm').addEventListener('submit', function(e) {
    e.preventDefault();

    // 1. 입력값 가져오기
    const bidAmount = parseInt(document.getElementById('bidAmount').value);

    // 2. 물건 정보 확인
    if (!currentItem) {
        alert('물건 정보를 불러오는 중입니다.');
        return;
    }

    // 3. 클라이언트 측 검증
    if (bidAmount <= currentItem.currentPrice) {
        alert('입찰가는 현재가보다 높아야 합니다.');
        return;
    }

    // 4. WebSocket으로 입찰 메시지 전송
    if (stompClient && stompClient.connected) {
        stompClient.send(
            '/app/auction/' + itemId + '/bid',  // 목적지
            {},                                   // 헤더 (없음)
            JSON.stringify({ bidAmount: bidAmount })  // 본문 (JSON)
        );

        // 5. 입력 필드 초기화
        document.getElementById('bidAmount').value = '';
    } else {
        alert('서버와 연결이 끊어졌습니다. 페이지를 새로고침해주세요.');
    }
});
```

**실제 전송되는 STOMP 프레임:**
```
SEND
destination:/app/auction/123/bid
content-length:22

{"bidAmount":50000}^@
```

---

## 4단계: 서버 처리 (WebSocketAuctionController.java)

### 4-1. 메시지 수신 및 처리

```java
@Controller
@RequiredArgsConstructor
@Slf4j
public class WebSocketAuctionController {

    private final AuctionService auctionService;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;

    /**
     * 입찰 메시지 처리
     *
     * @MessageMapping: 클라이언트가 보낸 메시지 수신
     * @SendTo: 처리 결과를 특정 토픽으로 브로드캐스트
     */
    @MessageMapping("/auction/{itemId}/bid")
    @SendTo("/topic/auction/{itemId}")
    public BidMessage handleBid(
            @DestinationVariable("itemId") Long itemId,  // URL 경로 변수
            @Payload BidRequest request,                  // 요청 본문
            Principal principal) {                        // 인증 정보

        log.info("==== WebSocket 입찰 요청 수신 ====");
        log.info("물건 ID: {}", itemId);
        log.info("입찰 금액: {}", request != null ? request.getBidAmount() : "null");
        log.info("사용자: {}", principal != null ? principal.getName() : "null");

        try {
            // ===== 1. 인증 확인 =====
            if (principal == null) {
                log.error("인증 정보가 없습니다 (principal is null)");
                throw new IllegalStateException("로그인이 필요합니다.");
            }

            // ===== 2. 입찰 금액 확인 =====
            if (request == null || request.getBidAmount() == null) {
                log.error("입찰 금액이 없습니다 (request or bidAmount is null)");
                throw new IllegalArgumentException("입찰 금액을 입력해주세요.");
            }

            // ===== 3. 사용자 조회 =====
            String username = principal.getName();
            log.info("사용자 조회 중: {}", username);
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

            // ===== 4. 입찰 처리 (비즈니스 로직) =====
            log.info("입찰 처리 시작 - 사용자: {}, 금액: {}", user.getUsername(), request.getBidAmount());
            Bid bid = auctionService.placeBid(itemId, user, request.getBidAmount());

            log.info("입찰 성공! 입찰 ID: {}, 금액: {}", bid.getId(), bid.getBidAmount());

            // ===== 5. 입찰 성공 메시지 생성 =====
            BidMessage message = new BidMessage();
            message.setItemId(itemId);
            message.setBidAmount(bid.getBidAmount());
            message.setBidderUsername(user.getUsername());
            message.setBidderName(user.getName());
            message.setBidTime(bid.getBidTime().toString());
            message.setSuccess(true);

            log.info("입찰 메시지 브로드캐스트 준비 완료");
            log.info("====================================");

            // ===== 6. 반환 (자동으로 /topic/auction/{itemId}로 브로드캐스트) =====
            return message;

        } catch (Exception e) {
            log.error("==== 입찰 실패 ====");
            log.error("예외 타입: {}", e.getClass().getName());
            log.error("오류 메시지: {}", e.getMessage());
            log.error("스택 트레이스:", e);
            log.error("====================");

            // ===== 에러 처리: 개인 메시지로 전송 =====
            BidMessage errorMessage = new BidMessage();
            errorMessage.setItemId(itemId);
            errorMessage.setSuccess(false);
            errorMessage.setErrorMessage(e.getMessage());

            // 에러 메시지는 요청한 사용자에게만 전송
            if (principal != null) {
                messagingTemplate.convertAndSendToUser(
                    principal.getName(),
                    "/queue/errors",
                    errorMessage
                );
            }

            // null 반환하여 브로드캐스트 하지 않음
            return null;
        }
    }

    // ===== DTO 클래스 =====

    // 요청 DTO
    public static class BidRequest {
        private Long bidAmount;

        public Long getBidAmount() {
            return bidAmount;
        }

        public void setBidAmount(Long bidAmount) {
            this.bidAmount = bidAmount;
        }
    }

    // 응답 DTO
    public static class BidMessage {
        private Long itemId;
        private Long bidAmount;
        private String bidderUsername;
        private String bidderName;
        private String bidTime;
        private boolean success;
        private String errorMessage;

        // Getters and Setters...
    }
}
```

### 4-2. 메시지 흐름

**성공 케이스:**
```
1. 클라이언트 A: /app/auction/123/bid로 입찰 메시지 전송
                 {"bidAmount": 50000}

2. 서버: @MessageMapping("/auction/{itemId}/bid")가 메시지 수신
        - itemId = 123
        - request.bidAmount = 50000
        - principal.name = "user1"

3. 서버: AuctionService.placeBid() 호출
        - 비즈니스 로직 검증
        - DB에 입찰 정보 저장
        - Bid 객체 반환

4. 서버: BidMessage 생성 및 반환
        {
          "itemId": 123,
          "bidAmount": 50000,
          "bidderUsername": "user1",
          "bidderName": "홍길동",
          "bidTime": "2025-01-15T10:30:00",
          "success": true
        }

5. 서버: @SendTo("/topic/auction/123")에 의해 자동 브로드캐스트
        → /topic/auction/123을 구독한 모든 클라이언트에게 전송

6. 클라이언트 A, B, C: 메시지 수신
        → handleBidMessage() 호출
        → UI 업데이트 (현재가, 입찰 내역)
```

**실패 케이스:**
```
1. 클라이언트 A: /app/auction/123/bid로 입찰 메시지 전송
                 {"bidAmount": 30000}  (현재가보다 낮음)

2. 서버: @MessageMapping("/auction/{itemId}/bid")가 메시지 수신

3. 서버: AuctionService.placeBid() 호출
        - 검증 실패: "현재가(40000원)보다 높은 금액을 입력하세요."
        - IllegalArgumentException 발생

4. 서버: catch 블록에서 예외 처리
        - 에러 메시지 생성
        - messagingTemplate.convertAndSendToUser()로 개인 메시지 전송
        - 목적지: /user/{username}/queue/errors
        - null 반환 (브로드캐스트 하지 않음)

5. 클라이언트 A: /user/queue/errors에서 메시지 수신
        → alert('입찰 실패: 현재가(40000원)보다 높은 금액을 입력하세요.')

6. 클라이언트 B, C: 아무 메시지도 받지 않음 (다른 사용자에게 영향 없음)
```

---

## 5단계: 입찰 처리 (AuctionService.java)

```java
@Service
public class AuctionService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BidRepository bidRepository;

    @Autowired
    private UserRepository userRepository;

    @Transactional
    public Bid placeBid(Long itemId, User user, Long bidAmount) {
        // ===== 1. 경매 물건 조회 =====
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("물건을 찾을 수 없습니다."));

        // ===== 2. 경매 상태 확인 =====
        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalStateException("경매가 진행 중이 아닙니다.");
        }

        // ===== 3. 종료 시간 확인 =====
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new IllegalStateException("경매가 종료되었습니다.");
        }

        // ===== 4. 판매자 본인 입찰 방지 =====
        if (item.getSeller().getId().equals(user.getId())) {
            throw new IllegalArgumentException("본인의 물건에는 입찰할 수 없습니다.");
        }

        // ===== 5. 입찰가 검증 =====
        if (bidAmount <= item.getCurrentPrice()) {
            throw new IllegalArgumentException(
                "현재가(" + item.getCurrentPrice() + "원)보다 높은 금액을 입력하세요."
            );
        }

        // ===== 6. 사용자 잔액 확인 =====
        if (user.getBalance() < bidAmount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // ===== 7. 입찰 정보 저장 =====
        Bid bid = new Bid();
        bid.setItem(item);
        bid.setBidder(user);
        bid.setBidAmount(bidAmount);
        bid.setBidTime(LocalDateTime.now());

        // ===== 8. 물건 정보 업데이트 =====
        item.setCurrentPrice(bidAmount);
        item.setCurrentWinner(user);

        // ===== 9. DB 저장 =====
        bidRepository.save(bid);
        itemRepository.save(item);

        return bid;
    }
}
```

---

## 6단계: 브로드캐스트 (서버 → 모든 클라이언트)

### 6-1. @SendTo 동작 방식

```java
@MessageMapping("/auction/{itemId}/bid")
@SendTo("/topic/auction/{itemId}")
public BidMessage handleBid(...) {
    // 입찰 처리...
    return bidMessage;  // 이 객체가 자동으로 /topic/auction/{itemId}로 전송됨
}
```

**내부 동작:**
1. 메소드가 `BidMessage` 객체 반환
2. Spring이 자동으로 JSON으로 변환
3. `/topic/auction/{itemId}`를 구독한 모든 클라이언트에게 전송

**실제 전송되는 STOMP 프레임:**
```
MESSAGE
destination:/topic/auction/123
content-type:application/json
subscription:sub-0

{"itemId":123,"bidAmount":50000,"bidderUsername":"user1",...}^@
```

### 6-2. 개인 메시지 전송 (에러)

```java
messagingTemplate.convertAndSendToUser(
    principal.getName(),  // "user1"
    "/queue/errors",
    errorMessage
);
```

**내부 동작:**
1. 특정 사용자(user1)에게만 전송
2. 목적지: `/user/user1/queue/errors`
3. Spring이 자동으로 사용자별 큐 생성

---

## 7단계: UI 업데이트 (클라이언트)

### 7-1. 입찰 메시지 처리

```javascript
// 입찰 메시지 처리 함수
function handleBidMessage(bidMessage) {
    // null 체크 (에러 발생 시 null 반환됨)
    if (!bidMessage) {
        return;
    }

    if (bidMessage.success) {
        // ===== 1. 현재가 업데이트 =====
        document.getElementById('currentPrice').textContent =
            bidMessage.bidAmount.toLocaleString() + '원';
        // 예: "50,000원"

        // ===== 2. 입찰 내역에 추가 =====
        addBidToHistory(bidMessage);
    }
}

// 입찰 내역에 추가하는 함수
function addBidToHistory(bidMessage) {
    const historyContainer = document.getElementById('bidHistory');

    // "입찰 내역이 없습니다" 메시지 제거
    const emptyMessage = historyContainer.querySelector('p.text-center.text-muted');
    if (emptyMessage) {
        emptyMessage.remove();
    }

    // 새 입찰 항목 생성
    const bidElement = document.createElement('div');
    bidElement.className = 'border-bottom pb-2 mb-2';
    bidElement.innerHTML = `
        <div class="d-flex justify-content-between">
            <strong>${bidMessage.bidderName} (${bidMessage.bidderUsername})</strong>
            <span class="text-primary">${bidMessage.bidAmount.toLocaleString()}원</span>
        </div>
        <small class="text-muted">${formatBidTime(bidMessage.bidTime)}</small>
    `;

    // 최신 입찰을 맨 위에 추가
    historyContainer.insertBefore(bidElement, historyContainer.firstChild);
}

// 시간 포맷팅
function formatBidTime(bidTime) {
    return new Date(bidTime).toLocaleString('ko-KR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
    });
}
```

---

## 전체 흐름 요약 (실제 예시)

### 시나리오: 3명의 사용자가 동시에 경매 참여

```
초기 상태:
- 물건: 아이폰 13 (itemId=123)
- 시작가: 30,000원
- 현재가: 40,000원
- 참가자: user1(홍길동), user2(김철수), user3(이영희)
```

#### 1. 페이지 진입 및 연결

**사용자 A (홍길동):**
```
1. http://localhost:8080/auction/auction-room/123 접속
2. connect() 자동 실행
3. new SockJS('/auction/ws-auction') → WebSocket 연결
4. stompClient.subscribe('/topic/auction/123') → 경매방 구독
5. loadItemInfo() → 현재가 40,000원 표시
6. loadBidHistory() → 기존 입찰 내역 로드
```

**사용자 B (김철수), 사용자 C (이영희):**
```
동일한 과정으로 경매방 접속 및 구독
```

#### 2. 사용자 A가 입찰 (45,000원)

```
[클라이언트 A]
1. 입찰 폼 입력: 45,000원
2. submit 이벤트 발생
3. stompClient.send('/app/auction/123/bid', {}, '{"bidAmount":45000}')

[서버]
4. @MessageMapping("/auction/123/bid") 메시지 수신
5. principal.getName() = "user1"
6. AuctionService.placeBid(123, user1, 45000) 호출
   - 검증 통과 (45000 > 40000)
   - DB 저장: bid_id=10, bid_amount=45000
   - item 업데이트: current_price=45000, current_winner=user1
7. BidMessage 생성 및 반환
8. @SendTo("/topic/auction/123")에 의해 브로드캐스트

[클라이언트 A, B, C 모두]
9. stompClient.subscribe 콜백 실행
10. handleBidMessage(bidMessage) 호출
11. UI 업데이트:
    - 현재가: 40,000원 → 45,000원
    - 입찰 내역에 추가: "홍길동 (user1) - 45,000원"
```

#### 3. 사용자 B가 입찰 실패 (43,000원)

```
[클라이언트 B]
1. 입찰 폼 입력: 43,000원
2. submit 이벤트 발생
3. stompClient.send('/app/auction/123/bid', {}, '{"bidAmount":43000}')

[서버]
4. @MessageMapping("/auction/123/bid") 메시지 수신
5. principal.getName() = "user2"
6. AuctionService.placeBid(123, user2, 43000) 호출
   - 검증 실패: 43000 <= 45000
   - IllegalArgumentException 발생
7. catch 블록에서 예외 처리
8. messagingTemplate.convertAndSendToUser("user2", "/queue/errors", errorMessage)

[클라이언트 B만]
9. stompClient.subscribe('/user/queue/errors') 콜백 실행
10. alert('입찰 실패: 현재가(45000원)보다 높은 금액을 입력하세요.')

[클라이언트 A, C]
11. 아무 변화 없음 (메시지 받지 않음)
```

#### 4. 사용자 C가 입찰 성공 (50,000원)

```
[클라이언트 C]
1. 입찰 폼 입력: 50,000원
2. stompClient.send('/app/auction/123/bid', {}, '{"bidAmount":50000}')

[서버]
3. AuctionService.placeBid(123, user3, 50000) 호출
4. 검증 통과 → DB 저장
5. 브로드캐스트: /topic/auction/123

[클라이언트 A, B, C 모두]
6. UI 업데이트:
    - 현재가: 45,000원 → 50,000원
    - 입찰 내역에 추가: "이영희 (user3) - 50,000원"
```

---

## 핵심 개념 정리

### 1. Pub/Sub (발행/구독) 패턴

```
┌─────────────┐
│ Publisher   │ (입찰한 사용자)
└──────┬──────┘
       │
       │ publish
       ▼
┌─────────────────────┐
│  Message Broker     │ (/topic/auction/123)
│  (Spring Server)    │
└──────────┬──────────┘
           │
           │ broadcast
    ┌──────┼──────┬──────┐
    │      │      │      │
    ▼      ▼      ▼      ▼
┌─────┐┌─────┐┌─────┐┌─────┐
│Sub A││Sub B││Sub C││Sub D│ (모든 구독자)
└─────┘└─────┘└─────┘└─────┘
```

- **Publisher**: 메시지를 발행하는 사용자
- **Broker**: 메시지를 중계하는 서버
- **Subscriber**: 메시지를 구독하는 사용자들

### 2. 브로드캐스트 vs 개인 메시지

| 구분 | 목적지 | 수신자 | 사용 예 |
|------|--------|--------|---------|
| **브로드캐스트** | `/topic/auction/{id}` | 구독한 모든 사용자 | 입찰 성공 알림 |
| **개인 메시지** | `/user/{username}/queue/errors` | 특정 사용자 1명 | 입찰 실패 에러 |

### 3. 메시지 흐름

```
클라이언트 → 서버: /app/auction/123/bid
서버 → 모든 클라이언트: /topic/auction/123 (브로드캐스트)
서버 → 특정 클라이언트: /user/{username}/queue/errors (개인)
```

---

## 사용한 주요 기술

### 서버 측

✅ **Spring WebSocket**
- `@EnableWebSocketMessageBroker`: WebSocket 메시지 브로커 활성화
- `WebSocketMessageBrokerConfigurer`: 설정 인터페이스

✅ **STOMP 프로토콜**
- `@MessageMapping`: 클라이언트 메시지 수신
- `@SendTo`: 브로드캐스트 목적지 지정
- `@DestinationVariable`: URL 경로 변수 추출
- `@Payload`: 메시지 본문 매핑

✅ **SimpMessagingTemplate**
- `convertAndSendToUser()`: 개인 메시지 전송
- `convertAndSend()`: 브로드캐스트 메시지 전송

### 클라이언트 측

✅ **SockJS**
- WebSocket Fallback 라이브러리
- 브라우저 호환성 보장

✅ **STOMP.js**
- JavaScript STOMP 클라이언트
- `stompClient.connect()`: 연결
- `stompClient.subscribe()`: 구독
- `stompClient.send()`: 메시지 전송

---

## 장점

### 1. 실시간성
- HTTP Polling 불필요
- 서버 → 클라이언트 즉시 전송 (Push)
- 지연 시간 최소화 (1초 이내)

### 2. 효율성
- 단일 TCP 연결 유지
- 불필요한 HTTP 요청 제거
- 서버 부하 감소

### 3. 확장성
- 경매방별 채널 분리 (/topic/auction/{id})
- 사용자별 개인 메시지 채널
- 브로드캐스트와 유니캐스트 동시 지원

### 4. 신뢰성
- SockJS Fallback으로 브라우저 호환성
- 자동 재연결 지원
- 트랜잭션 보장 (@Transactional)

---

## 정리

이 WebSocket 실시간 경매 시스템은 **Spring WebSocket + STOMP + SockJS**를 사용하여 구현했습니다.

**핵심 구성요소:**
1. **WebSocketConfig**: 메시지 브로커 및 엔드포인트 설정
2. **WebSocketAuctionController**: 입찰 메시지 처리 및 브로드캐스트
3. **AuctionService**: 비즈니스 로직 (입찰 검증 및 저장)
4. **클라이언트 (SockJS + STOMP.js)**: 실시간 연결 및 UI 업데이트

**메시지 흐름:**
```
클라이언트 입찰 → WebSocket 전송 → 서버 검증 → DB 저장 →
브로드캐스트 → 모든 클라이언트 수신 → UI 업데이트
```

이 구조로 **여러 사용자가 동시에 입찰해도 실시간으로 모든 참가자에게 즉시 반영**됩니다!
