"""
가격 분석 및 추천 모듈
수집된 가격 데이터를 분석하여 경매 시작가를 추천합니다.
"""
from typing import List, Dict
import statistics

class PriceAnalyzer:
    def __init__(self):
        self.base_discount_rate = 0.1  # 기본 할인율 10%

    def detect_condition(self, description: str) -> float:
        """
        설명에서 물건 상태를 분석하여 추가 할인율 계산

        Returns:
            추가 할인율 (0.0 ~ 0.3)
        """
        description_lower = description.lower()
        additional_discount = 0.0

        print(f"[DEBUG] 설명 분석: '{description}'")
        print(f"[DEBUG] 소문자 변환: '{description_lower}'")

        # 손상/결함 관련 키워드 (-15% 추가)
        damage_keywords = ['스크래치', '긁힘', '파손', '고장', '오류', '문제', '불량', '흠']
        damage_found = [k for k in damage_keywords if k in description_lower]
        if damage_found:
            additional_discount += 0.15
            print(f"[DEBUG] 손상 키워드 발견: {damage_found} → +15% 할인")

        # 오래 사용 (-10% 추가)
        # "2년", "3년" 등 숫자 확인
        import re
        year_match = re.search(r'(\d+)\s*년', description_lower)
        if year_match:
            years = int(year_match.group(1))
            print(f"[DEBUG] 사용 기간 발견: {years}년")
            if years >= 2:
                additional_discount += 0.10
                print(f"[DEBUG] 2년 이상 사용 → +10% 할인")

        # 배터리 문제 (-10% 추가)
        battery_keywords = ['배터리', '베터리']
        bad_battery_keywords = ['약함', '부족', '교체', '낮음', '소모']
        if any(b in description_lower for b in battery_keywords):
            if any(bad in description_lower for bad in bad_battery_keywords):
                additional_discount += 0.10
                print(f"[DEBUG] 배터리 문제 발견 → +10% 할인")

        print(f"[DEBUG] 총 추가 할인율: {additional_discount * 100}%")

        # 최대 30% 추가 할인
        return min(additional_discount, 0.3)

    def analyze(self, prices: List[int], description: str = "") -> Dict:
        """
        가격 리스트를 분석하여 추천가 계산

        Args:
            prices: 가격 리스트
            description: 물건 설명 (상태 분석용)

        Returns:
            {
                'recommended_price': int,  # 추천 시작가
                'average_price': int,      # 평균가
                'min_price': int,          # 최저가
                'max_price': int,          # 최고가
                'count': int,              # 샘플 수
                'condition_discount': float # 상태별 추가 할인율
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

        print(f"[DEBUG] 평균가: {avg_price:,}원")

        # 상태별 추가 할인율 계산
        condition_discount = self.detect_condition(description)
        total_discount = self.base_discount_rate + condition_discount

        print(f"[DEBUG] 기본 할인: {self.base_discount_rate * 100}%")
        print(f"[DEBUG] 총 할인: {total_discount * 100}%")

        # 추천가: 평균가 * (1 - 총 할인율)
        recommended = int(avg_price * (1 - total_discount))

        print(f"[DEBUG] 추천가: {recommended:,}원")

        # 메시지 생성
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

    def format_price(self, price: int) -> str:
        """
        가격을 한국 원화 형식으로 포맷팅
        """
        return f"{price:,}원"
