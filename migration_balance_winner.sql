-- User 테이블에 balance 컬럼 추가
ALTER TABLE users ADD COLUMN balance BIGINT NOT NULL DEFAULT 0;

-- Item 테이블에 winner_id 컬럼 추가
ALTER TABLE items ADD COLUMN winner_id BIGINT;
