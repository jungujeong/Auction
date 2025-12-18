# 중고 경매 시스템 (Auction System)

실시간 WebSocket 기반 중고 물품 경매 플랫폼 + AI 가격 추천 시스템

---

## 📋 목차

- [프로젝트 개요](#프로젝트-개요)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [시스템 요구사항](#시스템-요구사항)
- [설치 및 실행](#설치-및-실행)
- [프로젝트 구조](#프로젝트-구조)
- [기술 문서](#기술-문서)
- [API 명세](#api-명세)
- [라이센스](#라이센스)

---

## 🎯 프로젝트 개요

Spring Boot 기반의 실시간 중고 물품 경매 시스템입니다. WebSocket을 활용한 실시간 입찰 기능과 Python Flask 기반 AI 가격 추천 시스템을 제공합니다.

### 핵심 특징

- ⚡ **실시간 경매**: WebSocket + STOMP 프로토콜로 실시간 입찰
- 🤖 **AI 가격 추천**: 당근마켓 크롤링 + 상태 분석 알고리즘
- 🌐 **다국어 지원**: 한국어/영어 (i18n)
- 🔒 **보안**: Spring Security + BCrypt 암호화
- ✅ **유효성 검사**: 다층 검증 (클라이언트 + 서버)

---

## ✨ 주요 기능

### 1. 사용자 관리
- 회원가입/로그인 (Spring Security)
- 프로필 관리 (이메일, 잔액, 비밀번호 변경)
- 세션 기반 인증

### 2. 물건 관리
- 물건 등록/수정/삭제
- 이미지 업로드
- AI 가격 추천 (당근마켓 크롤링)
- 카테고리별 분류

### 3. 실시간 경매
- WebSocket 기반 실시간 입찰
- 입찰 내역 실시간 업데이트
- 경매 자동 종료 (스케줄러)
- 최고가 낙찰자 자동 결정

### 4. AI 가격 추천
- 제목 기반 당근마켓 검색
- JSON-LD 구조화 데이터 파싱
- 이상치 제거 통계 분석
- 상태별 할인율 적용 (손상, 사용 기간, 배터리)

---

## 🛠 기술 스택

### Backend
- **Framework**: Spring Boot 3.2.x
- **Language**: Java 17
- **Security**: Spring Security 6.x
- **Database**: MySQL 8.0
- **ORM**: Spring Data JPA (Hibernate)
- **WebSocket**: Spring WebSocket + STOMP
- **Validation**: Jakarta Bean Validation
- **Template Engine**: Thymeleaf

### AI/Python
- **Framework**: Flask 3.x
- **Language**: Python 3.11
- **Web Crawling**: BeautifulSoup4, Requests
- **Data Analysis**: Statistics (Python 내장)

### Frontend
- **HTML5 + CSS3**
- **JavaScript (ES6)**
- **SockJS + STOMP.js** (WebSocket 클라이언트)
- **Bootstrap 5** (UI 프레임워크)

### Tools
- **Build**: Gradle 8.x
- **IDE**: Eclipse / IntelliJ IDEA
- **Version Control**: Git

---

## 💻 시스템 요구사항

### 필수 소프트웨어

1. **Java 17 이상**
   ```bash
   java -version
   # Java version "17.0.x" 이상 확인
   ```

2. **MySQL 8.0 이상**
   ```bash
   mysql --version
   # MySQL 8.0.x 이상 확인
   ```

3. **Python 3.11 이상** (AI 가격 추천 사용 시)
   ```bash
   python --version
   # Python 3.11.x 이상 확인
   ```

4. **Gradle 8.x** (또는 Gradle Wrapper 사용)

---

## 🚀 설치 및 실행

### 1. 프로젝트 클론

```bash
git clone <repository-url>
cd auction
```

### 2. 데이터베이스 설정

#### 2-1. MySQL 데이터베이스 생성

```sql
CREATE DATABASE auction_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'auction_user'@'localhost' IDENTIFIED BY 'your_password';
GRANT ALL PRIVILEGES ON auction_db.* TO 'auction_user'@'localhost';
FLUSH PRIVILEGES;
```

#### 2-2. 데이터베이스 스키마 Import

```bash
# 방법 1: MySQL 명령줄
mysql -u auction_user -p auction_db < auction_db.sql

# 방법 2: MySQL Workbench
# Server > Data Import > Import from Self-Contained File
# auction_db.sql 선택 후 Start Import
```

#### 2-3. application.properties 설정

`src/main/resources/application.properties` 파일을 확인하고 DB 정보를 수정하세요:

```properties
spring.datasource.url=jdbc:mysql://localhost:3306/auction_db
spring.datasource.username=auction_user
spring.datasource.password=your_password
```

### 3. Spring Boot 서버 실행

```bash
# Gradle Wrapper 사용 (권장)
./gradlew bootRun

# 또는 IDE에서 실행
# AuctionApplication.java 우클릭 > Run As > Java Application
```

서버가 시작되면 http://localhost:8080/auction/ 에서 접속 가능합니다.

### 4. AI 가격 추천 서버 실행 (선택 사항)

#### 4-1. Python 가상환경 생성 및 활성화

```bash
cd price-recommender

# Windows
python -m venv venv
venv\Scripts\activate

# macOS/Linux
python3 -m venv venv
source venv/bin/activate
```

#### 4-2. 의존성 설치

```bash
pip install -r requirements.txt
```

#### 4-3. Flask 서버 실행

```bash
python app.py
```

Flask 서버가 http://localhost:5000 에서 실행됩니다.

### 5. 접속 및 테스트

1. 브라우저에서 http://localhost:8080/auction/ 접속
2. 회원가입 후 로그인
3. 물건 등록 시 "AI 가격 추천 받기" 버튼 클릭 (Flask 서버 실행 필요)
4. 경매 입찰 테스트

---

## 📁 프로젝트 구조

```
auction/
├── src/
│   ├── main/
│   │   ├── java/com/auction/auction/
│   │   │   ├── config/              # 설정 클래스
│   │   │   │   ├── SecurityConfig.java
│   │   │   │   ├── WebConfig.java
│   │   │   │   └── WebSocketConfig.java
│   │   │   ├── controller/          # 컨트롤러
│   │   │   │   ├── ItemController.java
│   │   │   │   ├── UserController.java
│   │   │   │   ├── AuctionController.java
│   │   │   │   └── WebSocketAuctionController.java
│   │   │   ├── dto/                 # 데이터 전송 객체
│   │   │   ├── exception/           # 예외 처리
│   │   │   ├── filter/              # 필터
│   │   │   ├── model/               # 엔티티
│   │   │   │   ├── User.java
│   │   │   │   ├── Item.java
│   │   │   │   └── Bid.java
│   │   │   ├── repository/          # JPA Repository
│   │   │   ├── scheduler/           # 스케줄러
│   │   │   ├── service/             # 비즈니스 로직
│   │   │   └── util/                # 유틸리티
│   │   └── resources/
│   │       ├── application.properties
│   │       ├── messages/            # 다국어 파일
│   │       │   ├── i18n.properties
│   │       │   ├── i18n_ko.properties
│   │       │   └── i18n_en.properties
│   │       ├── static/              # 정적 파일
│   │       │   ├── css/
│   │       │   ├── js/
│   │       │   └── uploads/         # 업로드된 이미지
│   │       └── templates/           # Thymeleaf 템플릿
│   │           ├── index.html
│   │           ├── login.html
│   │           ├── signup.html
│   │           ├── auction-room.html
│   │           └── items/
│   └── test/                        # 테스트 코드
├── price-recommender/               # Python AI 가격 추천 시스템
│   ├── app.py                       # Flask REST API 서버
│   ├── crawler.py                   # 당근마켓 크롤러
│   ├── price_analyzer.py            # 가격 분석 모듈
│   ├── requirements.txt             # Python 의존성
│   └── README.md
├── auction_db.sql                   # 데이터베이스 스키마
├── build.gradle                     # Gradle 빌드 스크립트
└── README.md                        # 이 파일
```

---

## 📚 기술 문서

프로젝트의 상세한 기술 문서는 다음 파일들을 참고하세요:

### 주요 문서

1. **[TECHNICAL_DOCUMENTATION.md](TECHNICAL_DOCUMENTATION.md)**
   - 전체 기술 스택 및 주요 기능 설명
   - RESTful API, 예외 처리, 다국어 지원, 보안, 3계층 구조
   - WebSocket 실시간 경매, AI 가격 추천

2. **[PACKAGE_STRUCTURE.md](PACKAGE_STRUCTURE.md)**
   - Java 패키지 구조 상세 설명
   - 각 패키지별 역할 및 클래스 설명
   - 계층 간 호출 흐름

3. **[VALIDATION_DOCUMENTATION.md](VALIDATION_DOCUMENTATION.md)**
   - 유효성 검사 구현 방법
   - Jakarta Bean Validation, HTML5 Validation
   - 다층 검증 구조 (클라이언트 + 서버)

4. **[WEBSOCKET_PROCESS.md](WEBSOCKET_PROCESS.md)**
   - WebSocket 실시간 경매 시스템 실행 과정
   - STOMP 프로토콜, SockJS, Pub/Sub 패턴
   - 7단계 실행 흐름 상세 설명

5. **[AI_PRICE_CRAWLING_PROCESS.md](AI_PRICE_CRAWLING_PROCESS.md)**
   - AI 가격 추천 시스템 작동 원리
   - 당근마켓 크롤링 과정
   - 가격 분석 알고리즘 (이상치 제거, 상태별 할인)

---

## 🔌 API 명세

### RESTful API Endpoints

#### 사용자 API
```
POST   /api/users/signup          # 회원가입
POST   /api/users/login           # 로그인
POST   /api/users/logout          # 로그아웃
GET    /api/users/profile         # 프로필 조회
PUT    /api/users/profile         # 프로필 수정
```

#### 물건 API
```
GET    /api/items                 # 물건 목록 조회
GET    /api/items/{id}            # 물건 상세 조회
POST   /api/items                 # 물건 등록
PUT    /api/items/{id}            # 물건 수정
DELETE /api/items/{id}            # 물건 삭제
```

#### 경매 API
```
GET    /api/auctions/{itemId}/bids    # 입찰 내역 조회
```

#### 파일 업로드 API
```
POST   /api/upload                # 이미지 업로드
```

### WebSocket Endpoints

```
# 연결
CONNECT  /ws-auction

# 구독
SUBSCRIBE  /topic/auction/{itemId}     # 경매방 입찰 정보 (브로드캐스트)
SUBSCRIBE  /user/queue/errors          # 개인 에러 메시지

# 메시지 전송
SEND  /app/auction/{itemId}/bid       # 입찰
```

### Python Flask API

```
POST   http://localhost:5000/api/recommend-price
GET    http://localhost:5000/health
```

**요청 예시:**
```json
{
  "title": "아이폰 13",
  "description": "3년 사용, 스크래치 있음"
}
```

**응답 예시:**
```json
{
  "success": true,
  "data": {
    "recommended_price": 230216,
    "average_price": 354333,
    "min_price": 320000,
    "max_price": 550000,
    "count": 12,
    "message": "12개의 유사 물품을 분석했습니다. (상태 분석: 추가 25% 할인 적용)"
  }
}
```

---

## 🎓 사용 예시

### 1. 회원가입 및 로그인

1. http://localhost:8080/auction/signup 접속
2. 아이디, 비밀번호, 이메일, 이름 입력
3. 회원가입 완료 후 로그인

### 2. 물건 등록 (AI 가격 추천 사용)

1. 상단 메뉴에서 "물건 등록" 클릭
2. 제목과 설명 입력 (예: "아이폰 13", "3년 사용, 스크래치 있음")
3. "💡 AI 가격 추천 받기" 버튼 클릭
4. 추천 시작가, 평균가, 최저가, 최고가 확인
5. "추천가 적용하기" 버튼으로 자동 입력
6. 종료 일시 선택 후 등록

### 3. 실시간 경매 참여

1. 경매 목록에서 원하는 물건 클릭
2. 경매방 입장 (WebSocket 자동 연결)
3. 현재가보다 높은 금액 입력 후 "입찰하기"
4. 다른 사용자의 입찰이 실시간으로 반영됨
5. 경매 종료 시 최고가 입찰자가 낙찰

---

## 🧪 테스트

### 단위 테스트 실행

```bash
./gradlew test
```

### 통합 테스트 실행

```bash
./gradlew integrationTest
```

---

## 🔧 환경 설정

### application.properties

```properties
# 서버 설정
server.port=8080
server.servlet.context-path=/auction

# 데이터베이스
spring.datasource.url=jdbc:mysql://localhost:3306/auction_db
spring.datasource.username=auction_user
spring.datasource.password=your_password

# JPA
spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true

# 파일 업로드
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB
upload.path=src/main/resources/static/uploads

# 다국어
spring.messages.basename=messages/i18n
spring.messages.encoding=UTF-8
```

---

## 🐛 트러블슈팅

### 1. MySQL 연결 실패

**문제**: `Communications link failure`

**해결**:
```bash
# MySQL 서비스 시작
# Windows
net start MySQL80

# macOS/Linux
sudo systemctl start mysql
```

### 2. Flask 서버 연결 안 됨

**문제**: "가격 추천 서버에 연결할 수 없습니다"

**해결**:
```bash
# Python 가상환경 활성화 후 Flask 서버 실행
cd price-recommender
venv\Scripts\activate  # Windows
python app.py
```

### 3. WebSocket 연결 끊김

**문제**: "서버와 연결이 끊어졌습니다"

**해결**:
- 페이지 새로고침 (자동 재연결)
- 브라우저 콘솔에서 에러 메시지 확인
- Spring Boot 서버 로그 확인

### 4. 이미지 업로드 실패

**문제**: 이미지 업로드 시 에러

**해결**:
```bash
# uploads 폴더 권한 확인
# Windows에서는 보통 문제 없음
# Linux/macOS:
chmod 755 src/main/resources/static/uploads
```

---

## 📝 개발 가이드

### 새 기능 추가 시 순서

1. **Model (Entity)** 작성
2. **Repository** 인터페이스 작성
3. **Service** 비즈니스 로직 구현
4. **Controller** REST API/View 작성
5. **DTO** 요청/응답 객체 작성
6. **Validation** 유효성 검사 추가
7. **Template** Thymeleaf 뷰 작성
8. **Test** 단위 테스트 작성

### 코딩 컨벤션

- **Java**: Google Java Style Guide
- **Python**: PEP 8
- **커밋 메시지**: Conventional Commits

---

## 🤝 기여

이 프로젝트는 학습 목적의 개인 프로젝트입니다.

---

## 📄 라이센스

이 프로젝트는 교육 목적으로 제작되었습니다.

---

## 👨‍💻 개발자

**정준구**
- Email: your-email@example.com
- GitHub: [@your-github](https://github.com/your-github)

---

## 🙏 감사의 말

- Spring Boot Documentation
- Thymeleaf Documentation
- Flask Documentation
- 당근마켓 (가격 데이터 제공)

---

## 📌 Quick Start (빠른 시작)

```bash
# 1. 클론
git clone <repository-url>
cd auction

# 2. 데이터베이스 생성 및 Import
mysql -u root -p
CREATE DATABASE auction_db;
USE auction_db;
SOURCE auction_db.sql;

# 3. Spring Boot 실행
./gradlew bootRun

# 4. (선택) Python Flask 실행
cd price-recommender
pip install -r requirements.txt
python app.py

# 5. 브라우저 접속
http://localhost:8080/auction/
```

---

**프로젝트 완성도**: 95% ✅
- ✅ 실시간 경매 시스템
- ✅ AI 가격 추천
- ✅ 다국어 지원
- ✅ 보안 (Spring Security)
- ✅ 유효성 검사
- ✅ 기술 문서 완비

**주요 기능 데모**: [스크린샷 추가 예정]
