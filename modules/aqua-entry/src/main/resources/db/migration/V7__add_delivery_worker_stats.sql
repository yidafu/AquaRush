-- 为 delivery_workers 表添加统计字段
ALTER TABLE delivery_workers
ADD COLUMN current_location JSONB,
ADD COLUMN rating DECIMAL(2,1),
ADD COLUMN total_orders INTEGER NOT NULL DEFAULT 0,
ADD COLUMN completed_orders INTEGER NOT NULL DEFAULT 0,
ADD COLUMN average_rating DECIMAL(2,1),
ADD COLUMN earning DECIMAL(10,2) DEFAULT 0.00,
ADD COLUMN is_available BOOLEAN NOT NULL DEFAULT true;

-- 修复 updated_at 字段为可变
ALTER TABLE delivery_workers
ALTER COLUMN updated_at DROP NOT NULL;

-- 为新字段添加索引
CREATE INDEX idx_delivery_workers_total_orders ON delivery_workers(total_orders);
CREATE INDEX idx_delivery_workers_is_available ON delivery_workers(is_available);
CREATE INDEX idx_delivery_workers_rating ON delivery_workers(rating);