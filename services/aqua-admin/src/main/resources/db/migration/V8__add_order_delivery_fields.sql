-- 为 orders 表添加配送地址和总金额字段
ALTER TABLE orders
ADD COLUMN delivery_address_id UUID NOT NULL DEFAULT gen_random_uuid(),
ADD COLUMN total_amount DECIMAL(10,2) NOT NULL DEFAULT 0.00;

-- 添加索引
CREATE INDEX idx_orders_delivery_address_id ON orders(delivery_address_id);

-- 如果需要，可以将现有数据的 address_id 复制到 delivery_address_id，amount 复制到 total_amount
UPDATE orders SET delivery_address_id = address_id, total_amount = COALESCE(amount, total_amount) WHERE delivery_address_id IS NULL;