# 경매 가격 추천 시스템

AI 기반 중고 물품 가격 추천 시스템입니다.

## 설치

```bash
cd price-recommender
pip install -r requirements.txt
```

## 실행

```bash
python app.py
```

서버가 http://localhost:5000 에서 실행됩니다.

## API 사용법

### POST /api/recommend-price

**Request:**
```json
{
  "title": "아이폰 13 128GB",
  "description": "사용감 적고 배터리 100% 상태 좋습니다"
}
```

**Response:**
```json
{
  "success": true,
  "data": {
    "recommended_price": 450000,
    "average_price": 500000,
    "min_price": 350000,
    "max_price": 650000,
    "count": 12,
    "message": "12개의 유사 물품을 분석했습니다."
  }
}
```

## Spring Boot 연동

물건 등록 페이지에서 제목과 설명을 입력하면 자동으로 추천 가격을 표시합니다.
