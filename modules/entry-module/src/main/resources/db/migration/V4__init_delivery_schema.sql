-- 送水员表
CREATE TABLE IF NOT EXISTS delivery_workers (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    wechat_openid VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    phone VARCHAR(20) UNIQUE NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'OFFLINE',
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_delivery_workers_status ON delivery_workers(status);
CREATE INDEX idx_delivery_workers_phone ON delivery_workers(phone);

-- 配送区域表
CREATE TABLE IF NOT EXISTS delivery_areas (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    province VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    district VARCHAR(100) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_delivery_areas_location ON delivery_areas(province, city, district);
CREATE INDEX idx_delivery_areas_enabled ON delivery_areas(enabled);
