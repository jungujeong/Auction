# 유효성 검사 (Validation) 구현

## 개요

이 프로젝트는 **다층 유효성 검사(Multi-Layer Validation)** 방식을 사용합니다.

```
클라이언트 측 검사 (JavaScript)
    ↓
서버 측 검사 (Bean Validation)
    ↓
비즈니스 로직 검사 (Service Layer)
```

---

## 1. 사용한 기술

### 1-1. Jakarta Bean Validation (서버)
- **라이브러리**: `jakarta.validation.constraints.*`
- **버전**: Jakarta EE 10 (Spring Boot 3.x)
- **역할**: DTO에 어노테이션으로 검증 규칙 정의

### 1-2. HTML5 Validation (클라이언트)
- **기술**: HTML5 속성 (`required`, `minlength`, `pattern` 등)
- **역할**: 브라우저에서 즉시 입력 검증

### 1-3. JavaScript Custom Validation (클라이언트)
- **기술**: JavaScript 정규표현식, 조건문
- **역할**: 복잡한 검증 로직 (비밀번호 일치 등)

---

## 2. 서버 측 유효성 검사 (Bean Validation)

### 2-1. 회원가입 검증 (SignupRequest.java)

```java
package com.auction.auction.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SignupRequest {

    // ===== 아이디 검증 =====
    @NotBlank(message = "사용자명은 필수입니다")
    @Size(min = 3, max = 20, message = "사용자명은 3~20자여야 합니다")
    @Pattern(regexp = "^[a-zA-Z0-9_]+$",
             message = "사용자명은 영문, 숫자, 언더스코어만 사용 가능합니다")
    private String username;

    // ===== 비밀번호 검증 =====
    @NotBlank(message = "비밀번호는 필수입니다")
    @Size(min = 8, max = 100, message = "비밀번호는 8자 이상이어야 합니다")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d@$!%*#?&]{8,}$",
             message = "비밀번호는 영문과 숫자를 포함해야 합니다")
    private String password;

    // ===== 이메일 검증 =====
    @NotBlank(message = "이메일은 필수입니다")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    // ===== 이름 검증 =====
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2~50자여야 합니다")
    private String name;
}
```

### 2-2. 물건 등록 검증 (ItemRequest.java)

```java
package com.auction.auction.dto;

import java.time.LocalDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ItemRequest {

    // ===== 제목 검증 =====
    @NotBlank(message = "제목은 필수입니다")
    @Size(min = 2, max = 100, message = "제목은 2~100자여야 합니다")
    private String title;

    // ===== 설명 검증 =====
    @NotBlank(message = "설명은 필수입니다")
    @Size(min = 10, max = 1000, message = "설명은 10~1000자여야 합니다")
    private String description;

    // ===== 시작 가격 검증 =====
    @NotNull(message = "시작 가격은 필수입니다")
    @Min(value = 1000, message = "시작 가격은 최소 1000원 이상이어야 합니다")
    private Long startPrice;

    private String imageUrl;  // 선택 사항

    // ===== 종료 시간 검증 =====
    @NotNull(message = "경매 종료 시간은 필수입니다")
    @Future(message = "경매 종료 시간은 현재 시간 이후여야 합니다")
    private LocalDateTime endTime;
}
```

### 2-3. Bean Validation 어노테이션 설명

| 어노테이션 | 용도 | 예시 |
|-----------|------|------|
| `@NotNull` | null 체크 (빈 문자열 허용) | `@NotNull private Long startPrice;` |
| `@NotBlank` | null, 빈 문자열, 공백 모두 불허 | `@NotBlank private String title;` |
| `@Size(min, max)` | 문자열 길이 제한 | `@Size(min = 3, max = 20)` |
| `@Min(value)` | 최소값 제한 | `@Min(value = 1000)` |
| `@Max(value)` | 최대값 제한 | `@Max(value = 1000000)` |
| `@Email` | 이메일 형식 검증 | `@Email private String email;` |
| `@Pattern(regexp)` | 정규표현식 패턴 매칭 | `@Pattern(regexp = "^[a-zA-Z0-9_]+$")` |
| `@Future` | 미래 시간만 허용 | `@Future private LocalDateTime endTime;` |
| `@Past` | 과거 시간만 허용 | `@Past private LocalDate birthDate;` |

### 2-4. 컨트롤러에서 검증 활성화

**`@Valid` 어노테이션 사용:**

```java
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // @Valid 어노테이션으로 자동 검증
    @PostMapping("/signup")
    public ResponseEntity<?> signup(@Valid @RequestBody SignupRequest request) {
        try {
            User user = new User();
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());
            user.setEmail(request.getEmail());
            user.setName(request.getName());

            User savedUser = userService.registerUser(user);

            UserResponse response = new UserResponse(
                savedUser.getId(),
                savedUser.getUsername(),
                savedUser.getEmail(),
                savedUser.getName(),
                savedUser.getCreatedAt()
            );

            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        // 로그인 처리
        // ...
    }
}
```

**동작 방식:**
1. 클라이언트가 JSON 데이터 전송
2. `@Valid`가 `SignupRequest`의 모든 검증 어노테이션 실행
3. 검증 실패 시 `MethodArgumentNotValidException` 발생
4. `GlobalExceptionHandler`에서 예외 처리

### 2-5. 검증 예외 처리

```java
@ControllerAdvice
public class GlobalExceptionHandler {

    // Bean Validation 실패 시 처리
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationExceptions(
            MethodArgumentNotValidException ex) {

        Map<String, String> errors = new HashMap<>();

        // 각 필드별 오류 메시지 수집
        ex.getBindingResult().getFieldErrors().forEach(error ->
            errors.put(error.getField(), error.getDefaultMessage())
        );

        return ResponseEntity.badRequest().body(errors);
    }

    // 일반적인 IllegalArgumentException 처리
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<String> handleIllegalArgument(IllegalArgumentException e) {
        return ResponseEntity.badRequest().body(e.getMessage());
    }
}
```

**오류 응답 예시:**
```json
{
  "username": "사용자명은 3~20자여야 합니다",
  "password": "비밀번호는 영문과 숫자를 포함해야 합니다",
  "email": "올바른 이메일 형식이 아닙니다"
}
```

---

## 3. 클라이언트 측 유효성 검사

### 3-1. HTML5 속성 (signup.html)

```html
<form id="signupForm" th:action="@{/api/users/signup}" method="post">
    <!-- ===== 아이디 검증 ===== -->
    <div class="form-group">
        <label for="username">아이디</label>
        <input type="text" id="username" name="username"
               required
               minlength="3"
               maxlength="20"
               pattern="[a-zA-Z0-9_]+">
        <small class="form-text text-muted">
            3~20자, 영문/숫자/언더스코어만 사용 가능
        </small>
    </div>

    <!-- ===== 비밀번호 검증 ===== -->
    <div class="form-group">
        <label for="password">비밀번호</label>
        <input type="password" id="password" name="password"
               required
               minlength="8"
               pattern="^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$">
        <small class="form-text text-muted">
            8자 이상, 영문과 숫자 포함
        </small>
    </div>

    <!-- ===== 비밀번호 확인 ===== -->
    <div class="form-group">
        <label for="confirmPassword">비밀번호 확인</label>
        <input type="password" id="confirmPassword" name="confirmPassword" required>
        <small class="form-text text-muted">
            비밀번호를 다시 입력하세요
        </small>
    </div>

    <!-- ===== 이름 검증 ===== -->
    <div class="form-group">
        <label for="name">이름</label>
        <input type="text" id="name" name="name"
               required
               minlength="2"
               maxlength="50">
        <small class="form-text text-muted">2~50자</small>
    </div>

    <!-- ===== 이메일 검증 ===== -->
    <div class="form-group">
        <label for="email">이메일</label>
        <input type="email" id="email" name="email" required>
        <small class="form-text text-muted">
            유효한 이메일 주소를 입력하세요
        </small>
    </div>

    <button type="submit" class="btn btn-primary">회원가입</button>
</form>
```

### 3-2. HTML5 속성 설명

| 속성 | 용도 | 예시 |
|------|------|------|
| `required` | 필수 입력 필드 | `<input required>` |
| `minlength` | 최소 문자 길이 | `<input minlength="3">` |
| `maxlength` | 최대 문자 길이 | `<input maxlength="20">` |
| `pattern` | 정규표현식 패턴 | `<input pattern="[a-zA-Z0-9_]+">` |
| `type="email"` | 이메일 형식 자동 검증 | `<input type="email">` |
| `type="number"` | 숫자만 입력 | `<input type="number">` |
| `min` | 최소값 (숫자) | `<input type="number" min="1000">` |
| `max` | 최대값 (숫자) | `<input type="number" max="1000000">` |

### 3-3. JavaScript 커스텀 검증 (signup.html)

```javascript
document.getElementById('signupForm').addEventListener('submit', async function(e) {
    e.preventDefault();

    const username = document.getElementById('username').value;
    const password = document.getElementById('password').value;
    const confirmPassword = document.getElementById('confirmPassword').value;
    const name = document.getElementById('name').value;
    const email = document.getElementById('email').value;

    // ===== 클라이언트 측 유효성 검사 =====

    // 1. 아이디 검사
    if (username.length < 3 || username.length > 20) {
        alert('아이디는 3~20자여야 합니다.');
        return;
    }
    if (!/^[a-zA-Z0-9_]+$/.test(username)) {
        alert('아이디는 영문, 숫자, 언더스코어만 사용 가능합니다.');
        return;
    }

    // 2. 비밀번호 검사
    if (password.length < 8) {
        alert('비밀번호는 8자 이상이어야 합니다.');
        return;
    }
    if (!/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/.test(password)) {
        alert('비밀번호는 영문과 숫자를 포함해야 합니다.');
        return;
    }

    // 3. 비밀번호 일치 확인
    if (password !== confirmPassword) {
        alert('비밀번호가 일치하지 않습니다.');
        return;
    }

    // 4. 이름 검사
    if (name.length < 2 || name.length > 50) {
        alert('이름은 2~50자여야 합니다.');
        return;
    }

    // 5. 이메일 검사
    const emailPattern = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    if (!emailPattern.test(email)) {
        alert('올바른 이메일 형식이 아닙니다.');
        return;
    }

    // ===== 서버로 전송 =====
    try {
        const response = await fetch(basePath + '/api/users/signup', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                username: username,
                password: password,
                email: email,
                name: name
            })
        });

        if (response.ok) {
            alert('회원가입이 완료되었습니다!');
            window.location.href = basePath + '/login';
        } else {
            const error = await response.text();
            alert('회원가입 실패: ' + error);
        }
    } catch (error) {
        console.error('Error:', error);
        alert('서버 오류가 발생했습니다.');
    }
});
```

### 3-4. 정규표현식 설명

**아이디 패턴:**
```javascript
/^[a-zA-Z0-9_]+$/

^ : 문자열 시작
[a-zA-Z0-9_] : 영문 대소문자, 숫자, 언더스코어
+ : 1개 이상
$ : 문자열 끝
```

**비밀번호 패턴:**
```javascript
/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/

(?=.*[A-Za-z]) : 최소 1개 영문자 포함 (positive lookahead)
(?=.*\d) : 최소 1개 숫자 포함
[A-Za-z\d@$!%*#?&]{8,} : 영문, 숫자, 특수문자로 8자 이상
```

**이메일 패턴:**
```javascript
/^[^\s@]+@[^\s@]+\.[^\s@]+$/

[^\s@]+ : 공백과 @를 제외한 문자 1개 이상
@ : @ 기호
[^\s@]+ : 도메인명
\. : . (점)
[^\s@]+ : 최상위 도메인
```

---

## 4. 비즈니스 로직 검증 (Service Layer)

### 4-1. UserService.java

```java
@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public User registerUser(User user) {
        // 1. 중복 아이디 검증
        if (userRepository.existsByUsername(user.getUsername())) {
            throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
        }

        // 2. 중복 이메일 검증
        if (userRepository.existsByEmail(user.getEmail())) {
            throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
        }

        // 3. 비밀번호 암호화
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        // 4. 기본값 설정
        user.setBalance(0L);
        user.setCreatedAt(LocalDateTime.now());

        // 5. 저장
        return userRepository.save(user);
    }

    @Transactional
    public User updateUser(String username, String email, Long balance,
                          String currentPassword, String newPassword) {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        // 1. 이메일 변경 (중복 검증)
        if (email != null && !email.equals(user.getEmail())) {
            if (userRepository.existsByEmail(email)) {
                throw new IllegalArgumentException("이미 사용 중인 이메일입니다.");
            }
            user.setEmail(email);
        }

        // 2. 잔액 변경
        if (balance != null) {
            user.setBalance(balance);
        }

        // 3. 비밀번호 변경 (현재 비밀번호 확인)
        if (currentPassword != null && newPassword != null) {
            if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
                throw new IllegalArgumentException("현재 비밀번호가 일치하지 않습니다.");
            }
            user.setPassword(passwordEncoder.encode(newPassword));
        }

        return userRepository.save(user);
    }
}
```

### 4-2. AuctionService.java (입찰 검증)

```java
@Service
public class AuctionService {

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private BidRepository bidRepository;

    @Transactional
    public Bid placeBid(Long itemId, Long userId, Integer bidAmount) {
        // 1. 경매 물건 존재 확인
        Item item = itemRepository.findById(itemId)
            .orElseThrow(() -> new IllegalArgumentException("물건을 찾을 수 없습니다."));

        // 2. 경매 진행 중 확인
        if (item.getStatus() != ItemStatus.ACTIVE) {
            throw new IllegalStateException("경매가 진행 중이 아닙니다.");
        }

        // 3. 종료 시간 확인
        if (LocalDateTime.now().isAfter(item.getEndTime())) {
            throw new IllegalStateException("경매가 종료되었습니다.");
        }

        // 4. 판매자 본인 입찰 방지
        if (item.getSeller().getId().equals(userId)) {
            throw new IllegalArgumentException("본인의 물건에는 입찰할 수 없습니다.");
        }

        // 5. 입찰가 검증 (현재가보다 높아야 함)
        if (bidAmount <= item.getCurrentPrice()) {
            throw new IllegalArgumentException(
                "현재가(" + item.getCurrentPrice() + "원)보다 높은 금액을 입력하세요."
            );
        }

        // 6. 사용자 잔액 확인
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        if (user.getBalance() < bidAmount) {
            throw new IllegalArgumentException("잔액이 부족합니다.");
        }

        // 7. 입찰 처리
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

---

## 5. 다층 검증 흐름

### 실제 회원가입 예시

**입력 데이터:**
```json
{
  "username": "ab",           // 너무 짧음 (3자 미만)
  "password": "12345678",     // 영문 없음
  "email": "invalid-email",   // 이메일 형식 아님
  "name": "홍"                // 너무 짧음 (2자 미만)
}
```

#### 1단계: HTML5 검증 (브라우저)
```html
<input type="text" id="username" minlength="3" maxlength="20" required>
```
→ **결과**: "3자 이상 입력해주세요" (브라우저 기본 메시지)

#### 2단계: JavaScript 검증 (클라이언트)
```javascript
if (username.length < 3 || username.length > 20) {
    alert('아이디는 3~20자여야 합니다.');
    return;
}
```
→ **결과**: alert 창으로 "아이디는 3~20자여야 합니다."

#### 3단계: Bean Validation 검증 (서버)
```java
@NotBlank(message = "사용자명은 필수입니다")
@Size(min = 3, max = 20, message = "사용자명은 3~20자여야 합니다")
private String username;
```
→ **결과**: HTTP 400 Bad Request + JSON 응답
```json
{
  "username": "사용자명은 3~20자여야 합니다",
  "password": "비밀번호는 영문과 숫자를 포함해야 합니다",
  "email": "올바른 이메일 형식이 아닙니다"
}
```

#### 4단계: 비즈니스 로직 검증 (Service)
```java
if (userRepository.existsByUsername(user.getUsername())) {
    throw new IllegalArgumentException("이미 존재하는 사용자명입니다.");
}
```
→ **결과**: HTTP 400 Bad Request + "이미 존재하는 사용자명입니다."

---

## 6. 유효성 검사 장점

### 6-1. 보안
- **SQL Injection 방지**: 입력 데이터 검증으로 악의적 쿼리 차단
- **XSS 방지**: 스크립트 태그 입력 제한
- **데이터 무결성**: 잘못된 데이터 저장 방지

### 6-2. 사용자 경험
- **즉각적인 피드백**: 클라이언트 측 검증으로 빠른 응답
- **명확한 오류 메시지**: 어떤 필드가 잘못되었는지 정확히 알림
- **중복 제출 방지**: 서버 부하 감소

### 6-3. 유지보수
- **일관된 검증 로직**: DTO에 한 번 정의하면 모든 컨트롤러에서 재사용
- **코드 간결성**: 어노테이션으로 검증 규칙 명시
- **테스트 용이**: 각 계층별 독립적 테스트 가능

---

## 7. 정리

| 계층 | 기술 | 역할 | 예시 |
|------|------|------|------|
| **클라이언트 (HTML5)** | `required`, `minlength`, `pattern` | 브라우저 기본 검증 | `<input minlength="3">` |
| **클라이언트 (JavaScript)** | 정규표현식, 조건문 | 복잡한 검증 (비밀번호 일치 등) | `if (password !== confirmPassword)` |
| **서버 (Bean Validation)** | `@NotBlank`, `@Size`, `@Email` | DTO 자동 검증 | `@Size(min = 3, max = 20)` |
| **서버 (Service)** | 비즈니스 로직 | 중복 검증, 권한 확인 등 | `if (existsByUsername())` |

**핵심 원칙:**
1. **클라이언트 검증**: 사용자 경험 향상 (빠른 피드백)
2. **서버 검증**: 보안 (클라이언트 우회 가능하므로 필수)
3. **다층 검증**: 각 계층이 서로 보완하며 데이터 무결성 보장

---

## 사용한 주요 기술 요약

✅ **Jakarta Bean Validation** (`jakarta.validation.constraints.*`)
- `@NotBlank`, `@NotNull`, `@Size`, `@Min`, `@Max`
- `@Email`, `@Pattern`, `@Future`, `@Past`

✅ **HTML5 Validation 속성**
- `required`, `minlength`, `maxlength`, `pattern`
- `type="email"`, `type="number"`, `min`, `max`

✅ **JavaScript 정규표현식**
- 아이디 검증: `/^[a-zA-Z0-9_]+$/`
- 비밀번호 검증: `/^(?=.*[A-Za-z])(?=.*\d)[A-Za-z\d@$!%*#?&]{8,}$/`
- 이메일 검증: `/^[^\s@]+@[^\s@]+\.[^\s@]+$/`

✅ **Spring Boot `@Valid`**
- 컨트롤러에서 자동 검증 활성화
- `MethodArgumentNotValidException` 예외 발생

✅ **GlobalExceptionHandler**
- `@ControllerAdvice`로 전역 예외 처리
- 검증 실패 시 JSON 형식 오류 메시지 반환
