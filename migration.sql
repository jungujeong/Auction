-- 경매 시스템 데이터베이스 마이그레이션 스크립트
-- 기존 items 테이블에 새로운 시간 필드 추가 및 status 업데이트

USE auction_db;

-- 1. 새 컬럼 추가 (nullable로 먼저 추가)
ALTER TABLE items
ADD COLUMN IF NOT EXISTS recruitment_end_time DATETIME(6) NULL,
ADD COLUMN IF NOT EXISTS auction_start_time DATETIME(6) NULL;

-- 2. 기존 데이터의 새 필드에 값 설정
UPDATE items
SET
    recruitment_end_time = DATE_ADD(created_at, INTERVAL 10 MINUTE),
    auction_start_time = DATE_ADD(created_at, INTERVAL 10 MINUTE)
WHERE recruitment_end_time IS NULL;

-- 3. status 값 업데이트 (기존 ACTIVE -> RECRUITING으로 변경)
UPDATE items
SET status = 'RECRUITING'
WHERE status = 'ACTIVE';

-- 4. 새 테이블 생성
CREATE TABLE IF NOT EXISTS auction_participants (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    joined_at DATETIME(6) NOT NULL,
    UNIQUE KEY unique_participant (item_id, user_id),
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS bids (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    bidder_id BIGINT NOT NULL,
    bid_amount BIGINT NOT NULL,
    bid_time DATETIME(6) NOT NULL,
    FOREIGN KEY (item_id) REFERENCES items(id),
    FOREIGN KEY (bidder_id) REFERENCES users(id),
    INDEX idx_item_time (item_id, bid_time DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 이미지 경로 수정 (중복된 /auction/ 제거)
UPDATE items
SET image_url = REPLACE(image_url, '/auction/uploads/', '/uploads/')
WHERE image_url LIKE '/auction/uploads/%';

-- 마이그레이션 완료
SELECT 'Migration completed successfully!' AS status;

-- 수정된 이미지 경로 확인
SELECT id, title, image_url FROM items WHERE image_url IS NOT NULL;
