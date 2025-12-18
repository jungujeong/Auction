"""
당근마켓 크롤러
제목과 설명을 받아서 비슷한 물건의 가격 정보를 수집합니다.
"""
import requests
from bs4 import BeautifulSoup
import re
import json
from typing import List, Dict
import time

class DaangnCrawler:
    def __init__(self):
        self.base_url = "https://www.daangn.com"
        self.headers = {
            'User-Agent': 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36'
        }

    def extract_keywords(self, title: str, description: str) -> str:
        """
        제목에서 검색 키워드 추출
        설명은 상태 분석에만 사용하고 검색어에는 영향을 주지 않음
        """
        keywords = title.strip()
        print(f"[DEBUG] 검색어: '{keywords}'")
        return keywords

    def search_items(self, keywords: str, max_results: int = 10) -> List[Dict]:
        """
        당근마켓에서 키워드로 물건 검색
        JSON-LD 구조화된 데이터에서 가격 정보 추출
        """
        search_url = f"{self.base_url}/search/{requests.utils.quote(keywords)}"
        print(f"[DEBUG] 검색 URL: {search_url}")

        try:
            response = requests.get(search_url, headers=self.headers, timeout=10)
            response.raise_for_status()

            soup = BeautifulSoup(response.text, 'html.parser')
            items = []

            # JSON-LD script 태그 찾기
            json_ld_scripts = soup.find_all('script', type='application/ld+json')
            print(f"[DEBUG] JSON-LD script 개수: {len(json_ld_scripts)}")

            for script in json_ld_scripts:
                try:
                    data = json.loads(script.string)

                    # ItemList 타입 찾기
                    if isinstance(data, dict) and data.get('@type') == 'ItemList':
                        item_list = data.get('itemListElement', [])
                        print(f"[DEBUG] ItemList 발견, 아이템 개수: {len(item_list)}")

                        for item_data in item_list[:max_results]:
                            try:
                                product = item_data.get('item', {})
                                title = product.get('name', '')
                                offers = product.get('offers', {})
                                price_str = offers.get('price', '0')

                                print(f"[DEBUG] 상품명: {title}")
                                print(f"[DEBUG] offers 데이터: {offers}")
                                print(f"[DEBUG] 가격 문자열: '{price_str}'")

                                # 가격 파싱
                                price = int(float(price_str))

                                print(f"[DEBUG] 파싱된 가격: {price}")

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

        except Exception as e:
            print(f"[ERROR] 검색 실패: {e}")
            return []

    def get_price_data(self, title: str, description: str) -> List[int]:
        """
        제목과 설명으로 유사 물건들의 가격 리스트 반환
        """
        keywords = self.extract_keywords(title, description)
        items = self.search_items(keywords, max_results=15)

        # 가격만 추출
        prices = [item['price'] for item in items if item['price'] > 0]

        return prices
