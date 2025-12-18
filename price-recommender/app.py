"""
Flask REST API 서버
Spring Boot에서 호출할 가격 추천 API를 제공합니다.
"""
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

    Request:
        {
            "title": "상품 제목",
            "description": "상품 설명"
        }

    Response:
        {
            "success": true,
            "data": {
                "recommended_price": 90000,
                "average_price": 100000,
                "min_price": 50000,
                "max_price": 150000,
                "count": 10,
                "message": "10개의 유사 물품을 분석했습니다."
            }
        }
    """
    try:
        data = request.get_json()

        if not data:
            return jsonify({
                'success': False,
                'error': 'Invalid request body'
            }), 400

        title = data.get('title', '').strip()
        description = data.get('description', '').strip()

        if not title:
            return jsonify({
                'success': False,
                'error': 'Title is required'
            }), 400

        # 1. 크롤링으로 가격 데이터 수집
        prices = crawler.get_price_data(title, description)

        # 2. 가격 분석 및 추천 (설명 전달하여 상태 분석)
        result = analyzer.analyze(prices, description)

        return jsonify({
            'success': True,
            'data': result
        })

    except Exception as e:
        return jsonify({
            'success': False,
            'error': str(e)
        }), 500

@app.route('/health', methods=['GET'])
def health_check():
    """서버 상태 확인"""
    return jsonify({'status': 'ok'})

if __name__ == '__main__':
    print("=" * 50)
    print("가격 추천 서버 시작")
    print("API Endpoint: http://localhost:5000/api/recommend-price")
    print("=" * 50)
    app.run(host='0.0.0.0', port=5000, debug=True)
