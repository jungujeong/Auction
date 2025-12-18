# 당근마켓 가격 크롤링 및 분석 과정

## 전체 프로세스 개요

```
사용자 입력 (제목 + 설명)
    ↓
Flask API 서버 (Python)
    ↓
1. 웹 크롤링 (crawler.py)
    - 당근마켓 검색
    - JSON-LD 데이터 파싱
    - 가격 리스트 수집
    ↓
2. 가격 분석 (price_analyzer.py)
    - 이상치 제거
    - 평균가 계산
    - 상태 분석 (키워드 검출)
    - 할인율 적용
    ↓
3. 추천가 반환
    - recommended_price (추천 시작가)
    - average_price (평균가)
    - min_price, max_price
    ↓
Spring Boot 프론트엔드에 표시
```

---

## 1단계: 사용자 입력 받기

### 프론트엔드 (register.html)

```javascript
// 사용자가 "AI 가격 추천 받기" 버튼 클릭
async function getPriceRecommendation() {
    const title = document.getElementById('title').value.trim();
    const description = document.getElementById('description').value.trim();

    // 예시:
    // title = "아이폰 13"
    // description = "3년 정도 사용했고 후면에 스크래치가 있습니다"

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
    // result.data에 추천 가격 정보 포함
}
```

---

## 2단계: Flask API 서버 (app.py)

### API 엔드포인트

```python
@app.route('/api/recommend-price', methods=['POST'])
def recommend_price():
    """
    가격 추천 API 엔드포인트
    """
    data = request.get_json()
    title = data.get('title', '').strip()          # "아이폰 13"
    description = data.get('description', '').strip()  # "3년 정도 사용..."

    # 1단계: 크롤링으로 가격 데이터 수집
    prices = crawler.get_price_data(title, description)
    # prices = [320000, 350000, 380000, ...]

    # 2단계: 가격 분석 및 추천
    result = analyzer.analyze(prices, description)
    # result = {'recommended_price': 230216, 'average_price': 354333, ...}

    return jsonify({'success': True, 'data': result})
```

---

## 3단계: 웹 크롤링 (crawler.py)

### 3-1. 키워드 추출

```python
def extract_keywords(self, title: str, description: str) -> str:
    """
    제목에서 검색 키워드 추출
    설명은 상태 분석에만 사용하고 검색어에는 영향을 주지 않음
    """
    keywords = title.strip()
    print(f"[DEBUG] 검색어: '{keywords}'")
    return keywords

# 입력: title = "아이폰 13", description = "3년 정도 사용..."
# 출력: "아이폰 13"
```

**중요 포인트:**
- **제목만 검색어로 사용** (설명은 나중에 할인율 계산에만 사용)
- 이전 버전에서는 "중고", "스크래치" 같은 키워드를 검색어에 추가했지만, 이 방식은 오히려 가격이 더 높은 물건만 검색되는 문제가 있어서 제거함

### 3-2. 당근마켓 검색

```python
def search_items(self, keywords: str, max_results: int = 10) -> List[Dict]:
    """
    당근마켓에서 키워드로 물건 검색
    JSON-LD 구조화된 데이터에서 가격 정보 추출
    """
    # 1. 검색 URL 생성
    search_url = f"{self.base_url}/search/{requests.utils.quote(keywords)}"
    # 예: https://www.daangn.com/search/아이폰%2013

    print(f"[DEBUG] 검색 URL: {search_url}")

    # 2. HTTP 요청
    response = requests.get(search_url, headers=self.headers, timeout=10)
    response.raise_for_status()

    # 3. HTML 파싱
    soup = BeautifulSoup(response.text, 'html.parser')
```

**User-Agent 설정 이유:**
```python
self.headers = {
    'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36'
}
```
- 웹 브라우저인 척 위장하여 크롤링 차단 방지
- User-Agent가 없으면 403 Forbidden 에러 발생 가능

### 3-3. JSON-LD 데이터 파싱

**JSON-LD란?**
- JSON for Linking Data
- 웹 페이지에 구조화된 데이터를 삽입하는 형식
- 검색 엔진 최적화(SEO)를 위해 사이트에서 제공
- `<script type="application/ld+json">` 태그 안에 위치

**당근마켓 JSON-LD 구조 예시:**
```json
{
  "@context": "https://schema.org",
  "@type": "ItemList",
  "itemListElement": [
    {
      "@type": "ListItem",
      "position": 1,
      "item": {
        "@type": "Product",
        "name": "아이폰13 128기가",
        "offers": {
          "@type": "Offer",
          "price": "350000",
          "priceCurrency": "KRW"
        }
      }
    },
    {
      "@type": "ListItem",
      "position": 2,
      "item": {
        "@type": "Product",
        "name": "아이폰13 미니",
        "offers": {
          "@type": "Offer",
          "price": "320000",
          "priceCurrency": "KRW"
        }
      }
    }
    // ... 더 많은 아이템
  ]
}
```

**JSON-LD 파싱 코드:**
```python
items = []

# JSON-LD script 태그 찾기
json_ld_scripts = soup.find_all('script', type='application/ld+json')
print(f"[DEBUG] JSON-LD script 개수: {len(json_ld_scripts)}")

for script in json_ld_scripts:
    try:
        # JSON 파싱
        data = json.loads(script.string)

        # ItemList 타입 찾기
        if isinstance(data, dict) and data.get('@type') == 'ItemList':
            item_list = data.get('itemListElement', [])
            print(f"[DEBUG] ItemList 발견, 아이템 개수: {len(item_list)}")

            for item_data in item_list[:max_results]:
                try:
                    # 상품 정보 추출
                    product = item_data.get('item', {})
                    title = product.get('name', '')
                    offers = product.get('offers', {})
                    price_str = offers.get('price', '0')

                    print(f"[DEBUG] 상품명: {title}")
                    print(f"[DEBUG] offers 데이터: {offers}")
                    print(f"[DEBUG] 가격 문자열: '{price_str}'")

                    # 가격 파싱 (문자열 → 정수)
                    price = int(float(price_str))

                    print(f"[DEBUG] 파싱된 가격: {price}")

                    # 유효한 가격만 추가 (0원 제외)
                    if price > 0 and title:
                        items.append({
                            'title': title,
                            'price': price
                        })
                        print(f"[DEBUG] 아이템 추가: {title} - {price:,}원")
                    else:
                        print(f"[DEBUG] 아이템 제외됨 (가격: {price}, 제목: '{title}')")

                except (ValueError, TypeError) as e:
                    print(f"[DEBUG] 가격 파싱 실패: {e}")
                    continue

    except json.JSONDecodeError as e:
        print(f"[DEBUG] JSON 파싱 실패: {e}")
        continue

print(f"[DEBUG] 총 {len(items)}개 아이템 수집됨")
return items[:max_results]
```

**실제 크롤링 결과 예시 (콘솔 출력):**
```
[DEBUG] 검색어: '아이폰 13'
[DEBUG] 검색 URL: https://www.daangn.com/search/아이폰%2013
[DEBUG] JSON-LD script 개수: 3
[DEBUG] ItemList 발견, 아이템 개수: 15
[DEBUG] 상품명: 아이폰13 128기가
[DEBUG] offers 데이터: {'@type': 'Offer', 'price': '350000', 'priceCurrency': 'KRW'}
[DEBUG] 가격 문자열: '350000'
[DEBUG] 파싱된 가격: 350000
[DEBUG] 아이템 추가: 아이폰13 128기가 - 350,000원

[DEBUG] 상품명: 아이폰13 미니
[DEBUG] offers 데이터: {'@type': 'Offer', 'price': '320000', 'priceCurrency': 'KRW'}
[DEBUG] 가격 문자열: '320000'
[DEBUG] 파싱된 가격: 320000
[DEBUG] 아이템 추가: 아이폰13 미니 - 320,000원

[DEBUG] 상품명: 아이폰13 프로 맥스
[DEBUG] offers 데이터: {'@type': 'Offer', 'price': '550000', 'priceCurrency': 'KRW'}
[DEBUG] 가격 문자열: '550000'
[DEBUG] 파싱된 가격: 550000
[DEBUG] 아이템 추가: 아이폰13 프로 맥스 - 550,000원

... (총 15개)

[DEBUG] 총 15개 아이템 수집됨
```

### 3-4. 가격 리스트 반환

```python
def get_price_data(self, title: str, description: str) -> List[int]:
    """
    제목과 설명으로 유사 물건들의 가격 리스트 반환
    """
    keywords = self.extract_keywords(title, description)
    items = self.search_items(keywords, max_results=15)

    # 가격만 추출
    prices = [item['price'] for item in items if item['price'] > 0]

    return prices

# 반환 예시: [350000, 320000, 550000, 380000, 340000, ...]
```

---

## 4단계: 가격 분석 (price_analyzer.py)

### 4-1. 이상치 제거

```python
def analyze(self, prices: List[int], description: str = "") -> Dict:
    """
    가격 리스트를 분석하여 추천가 계산
    """
    # 입력: prices = [350000, 320000, 550000, 380000, 340000, ...]

    if not prices:
        return {
            'recommended_price': 0,
            'average_price': 0,
            'message': '검색 결과가 없습니다.'
        }

    # 이상치 제거 (평균의 3배 이상인 값 제거)
    mean = statistics.mean(prices)
    # mean = 354,333원

    filtered_prices = [p for p in prices if p <= mean * 3]
    # 1,062,999원 이상인 가격 제거 (너무 비싼 프로 맥스 등)

    if not filtered_prices:
        filtered_prices = prices

    # 통계 계산
    avg_price = int(statistics.mean(filtered_prices))  # 354,333원
    min_price = min(filtered_prices)                    # 320,000원
    max_price = max(filtered_prices)                    # 550,000원

    print(f"[DEBUG] 평균가: {avg_price:,}원")
```

**이상치 제거 이유:**
- "아이폰 13" 검색 시 "아이폰 13 프로 맥스"도 포함될 수 있음
- 프로 맥스는 일반 모델보다 2배 이상 비쌈
- 평균의 3배를 초과하는 가격은 다른 모델로 간주하여 제외

### 4-2. 상태 분석 (키워드 검출)

```python
def detect_condition(self, description: str) -> float:
    """
    설명에서 물건 상태를 분석하여 추가 할인율 계산

    Returns: 추가 할인율 (0.0 ~ 0.3)
    """
    description_lower = description.lower()
    # "3년 정도 사용했고 후면에 스크래치가 있습니다"
    # → "3년 정도 사용했고 후면에 스크래치가 있습니다"

    additional_discount = 0.0

    print(f"[DEBUG] 설명 분석: '{description}'")
    print(f"[DEBUG] 소문자 변환: '{description_lower}'")

    # ===== 1. 손상/결함 관련 키워드 검출 (-15% 추가) =====
    damage_keywords = ['스크래치', '긁힘', '파손', '고장', '오류', '문제', '불량', '흠']
    damage_found = [k for k in damage_keywords if k in description_lower]

    if damage_found:
        additional_discount += 0.15
        print(f"[DEBUG] 손상 키워드 발견: {damage_found} → +15% 할인")

    # 예: "스크래치" 발견 → +15% 할인

    # ===== 2. 오래 사용 검출 (-10% 추가) =====
    # 정규표현식으로 "2년", "3년" 등 숫자 찾기
    import re
    year_match = re.search(r'(\d+)\s*년', description_lower)

    if year_match:
        years = int(year_match.group(1))
        print(f"[DEBUG] 사용 기간 발견: {years}년")

        if years >= 2:
            additional_discount += 0.10
            print(f"[DEBUG] 2년 이상 사용 → +10% 할인")

    # 예: "3년" 발견 → +10% 할인

    # ===== 3. 배터리 문제 검출 (-10% 추가) =====
    battery_keywords = ['배터리', '베터리']
    bad_battery_keywords = ['약함', '부족', '교체', '낮음', '소모']

    if any(b in description_lower for b in battery_keywords):
        if any(bad in description_lower for bad in bad_battery_keywords):
            additional_discount += 0.10
            print(f"[DEBUG] 배터리 문제 발견 → +10% 할인")

    # 예: "배터리 교체" 발견 → +10% 할인

    print(f"[DEBUG] 총 추가 할인율: {additional_discount * 100}%")

    # 최대 30% 추가 할인
    return min(additional_discount, 0.3)

# 예시 결과:
# - "스크래치" (+15%) + "3년" (+10%) = 총 25% 추가 할인
```

**정규표현식 설명:**
```python
year_match = re.search(r'(\d+)\s*년', description_lower)
```
- `\d+`: 1개 이상의 숫자 (예: 2, 3, 10)
- `\s*`: 0개 이상의 공백 (있어도 되고 없어도 됨)
- `년`: 한글 "년" 문자
- 매칭 예시: "2년", "3 년", "10년"

### 4-3. 추천가 계산

```python
# 상태별 추가 할인율 계산
condition_discount = self.detect_condition(description)
# condition_discount = 0.25 (25%)

total_discount = self.base_discount_rate + condition_discount
# total_discount = 0.10 + 0.25 = 0.35 (35%)

print(f"[DEBUG] 기본 할인: {self.base_discount_rate * 100}%")  # 10%
print(f"[DEBUG] 총 할인: {total_discount * 100}%")              # 35%

# 추천가 = 평균가 × (1 - 총 할인율)
recommended = int(avg_price * (1 - total_discount))
# recommended = 354,333 × (1 - 0.35)
# recommended = 354,333 × 0.65
# recommended = 230,216원

print(f"[DEBUG] 추천가: {recommended:,}원")
```

**할인율 계산 로직:**
```
기본 할인: 10% (경매는 중고 시장가보다 낮게 시작)
상태별 추가 할인:
  - 손상 키워드 (스크래치, 긁힘 등): +15%
  - 2년 이상 사용: +10%
  - 배터리 문제: +10%
  - 최대 추가 할인: 30%

총 할인율 = 기본 10% + 상태별 추가 할인 (최대 40%)
추천가 = 평균가 × (1 - 총 할인율)
```

### 4-4. 메시지 생성 및 결과 반환

```python
# 메시지 생성
message = f'{len(filtered_prices)}개의 유사 물품을 분석했습니다.'
if condition_discount > 0:
    message += f' (상태 분석: 추가 {int(condition_discount * 100)}% 할인 적용)'

# 예: "12개의 유사 물품을 분석했습니다. (상태 분석: 추가 25% 할인 적용)"

return {
    'recommended_price': recommended,    # 230,216원
    'average_price': avg_price,          # 354,333원
    'min_price': min_price,              # 320,000원
    'max_price': max_price,              # 550,000원
    'count': len(filtered_prices),       # 12개
    'condition_discount': int(condition_discount * 100),  # 25%
    'message': message
}
```

---

## 5단계: 프론트엔드에 결과 표시

### JavaScript로 UI 업데이트

```javascript
const result = await response.json();

if (result.success && result.data) {
    // 결과 표시
    document.getElementById('recommendationMessage').textContent = result.data.message;
    // "12개의 유사 물품을 분석했습니다. (상태 분석: 추가 25% 할인 적용)"

    document.getElementById('recommendedPrice').textContent =
        formatPrice(result.data.recommended_price);
    // "230,216원"

    document.getElementById('averagePrice').textContent =
        formatPrice(result.data.average_price);
    // "354,333원"

    document.getElementById('minPrice').textContent =
        formatPrice(result.data.min_price);
    // "320,000원"

    document.getElementById('maxPrice').textContent =
        formatPrice(result.data.max_price);
    // "550,000원"

    document.getElementById('priceRecommendation').style.display = 'block';
}

// 가격 포맷팅 함수
function formatPrice(price) {
    return price.toLocaleString('ko-KR') + '원';
}
```

### 사용자가 보는 화면

```
┌─────────────────────────────────────────┐
│ 💡 AI 가격 추천 받기                     │
└─────────────────────────────────────────┘

┌─────────────────────────────────────────┐
│ 추천 가격 정보                            │
│                                          │
│ 12개의 유사 물품을 분석했습니다.          │
│ (상태 분석: 추가 25% 할인 적용)           │
│                                          │
│ 추천 시작가: 230,216원  │  평균가: 354,333원  │
│ 최저가: 320,000원      │  최고가: 550,000원  │
│                                          │
│ [ 추천가 적용하기 ]                       │
└─────────────────────────────────────────┘
```

---

## 전체 흐름 요약 (실제 예시)

### 입력
```
제목: "아이폰 13"
설명: "3년 정도 사용했고 후면에 스크래치가 있습니다"
```

### 처리 과정

#### 1. 크롤링
```
검색어: "아이폰 13"
→ 당근마켓 검색: https://www.daangn.com/search/아이폰%2013
→ 15개 상품 가격 수집: [350000, 320000, 550000, 380000, 340000, ...]
```

#### 2. 이상치 제거
```
평균: 354,333원
평균의 3배: 1,062,999원
→ 1,062,999원 이상 제거
→ 최종 12개 가격: [350000, 320000, 550000, 380000, 340000, ...]
```

#### 3. 통계 계산
```
평균가: 354,333원
최저가: 320,000원
최고가: 550,000원
```

#### 4. 상태 분석
```
설명: "3년 정도 사용했고 후면에 스크래치가 있습니다"
→ "스크래치" 발견: +15% 할인
→ "3년" 발견: +10% 할인
→ 총 추가 할인: 25%
```

#### 5. 추천가 계산
```
기본 할인: 10%
추가 할인: 25%
총 할인: 35%

추천가 = 354,333 × (1 - 0.35)
추천가 = 354,333 × 0.65
추천가 = 230,216원
```

### 출력
```json
{
  "success": true,
  "data": {
    "recommended_price": 230216,
    "average_price": 354333,
    "min_price": 320000,
    "max_price": 550000,
    "count": 12,
    "condition_discount": 25,
    "message": "12개의 유사 물품을 분석했습니다. (상태 분석: 추가 25% 할인 적용)"
  }
}
```

---

## 핵심 알고리즘 정리

### 1. 크롤링 방식: JSON-LD 파싱
- **장점**: HTML 구조 변경에 강함, 정확한 데이터 추출
- **단점**: JSON-LD가 없는 사이트에서는 사용 불가

### 2. 가격 분석: 통계 기반
- **이상치 제거**: 평균의 3배 초과 가격 제외
- **평균가 사용**: 중앙값보다 더 직관적이고 이해하기 쉬움

### 3. 상태 분석: 규칙 기반 NLP
- **키워드 매칭**: 손상, 사용 기간, 배터리 상태 등
- **정규표현식**: 숫자 추출 ("2년", "3년" 등)
- **할인율 누적**: 여러 문제가 있으면 할인율 합산

### 4. 할인율 설계
```
기본 할인 10% (경매 특성)
+ 손상 15% (외관/기능 문제)
+ 오래 사용 10% (2년 이상)
+ 배터리 문제 10%
─────────────────
최대 총 할인 40%
```

---

## 기술적 특징

### 1. BeautifulSoup4
- Python HTML 파싱 라이브러리
- CSS Selector, 태그 검색 지원

### 2. JSON-LD (Linked Data)
- schema.org 표준 데이터 형식
- 검색 엔진 최적화(SEO) 목적으로 사이트에서 제공

### 3. 정규표현식 (Regex)
- 패턴 매칭으로 텍스트에서 정보 추출
- "2년", "3년" 등 사용 기간 파싱

### 4. 통계 분석
- `statistics.mean()`: 평균 계산
- 이상치 제거: Outlier detection

---

## 개선 가능성

### 머신러닝 적용 시
```python
# 현재: 규칙 기반 (Rule-based)
if "스크래치" in description:
    discount += 0.15

# 머신러닝: 학습 기반 (Learning-based)
# 수천 개의 거래 데이터로 학습
# "상태 설명" → "실제 거래가" 패턴 학습
# 더 정확한 가격 예측 가능
```

### 현재 방식의 장점
- **간단하고 투명함**: 할인율 계산 과정이 명확
- **즉시 작동**: 학습 데이터 불필요
- **쉬운 유지보수**: 키워드만 추가하면 확장 가능
- **교육용**: 알고리즘 이해가 쉬움

---

## 결론

이 AI 가격 추천 시스템은 **웹 크롤링 + 통계 분석 + 규칙 기반 NLP**를 결합한 실용적인 솔루션입니다.

**핵심 기능:**
1. 당근마켓에서 유사 물품 가격 자동 수집
2. 이상치 제거 및 통계 계산
3. 물건 상태 자동 분석 (키워드 검출)
4. 경매 시작가 추천 (평균가 대비 할인)

**실제 활용:**
- 판매자가 적정 시작가를 쉽게 설정
- 시장 가격을 모르는 초보 판매자에게 유용
- 데이터 기반 의사결정 지원
