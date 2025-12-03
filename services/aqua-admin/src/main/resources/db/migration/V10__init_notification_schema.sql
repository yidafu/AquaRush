-- Create user notification settings table
CREATE TABLE IF NOT EXISTS user_notification_settings (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL UNIQUE,
    order_updates BOOLEAN NOT NULL DEFAULT true,
    payment_notifications BOOLEAN NOT NULL DEFAULT true,
    delivery_notifications BOOLEAN NOT NULL DEFAULT true,
    promotional_notifications BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create message history table
CREATE TABLE IF NOT EXISTS message_history (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    template_id VARCHAR(100) NOT NULL,
    content TEXT,
    status VARCHAR(20) NOT NULL,
    sent_at TIMESTAMP,
    wx_message_id VARCHAR(100),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0
);

-- Create indexes for better performance
CREATE INDEX IF NOT EXISTS idx_user_notification_settings_user_id ON user_notification_settings(user_id);
CREATE INDEX IF NOT EXISTS idx_message_history_user_id ON message_history(user_id);
CREATE INDEX IF NOT EXISTS idx_message_history_status ON message_history(status);
CREATE INDEX IF NOT EXISTS idx_message_history_message_type ON message_history(message_type);
CREATE INDEX IF NOT EXISTS idx_message_history_created_at ON message_history(created_at);
CREATE INDEX IF NOT EXISTS idx_message_history_wx_message_id ON message_history(wx_message_id);

-- Add constraints for message status
ALTER TABLE message_history
ADD CONSTRAINT IF NOT EXISTS chk_message_status
CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'RETRYING'));

-- Create trigger to update updated_at column for user_notification_settings
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_user_notification_settings_updated_at
BEFORE UPDATE ON user_notification_settings
FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Add comments for documentation
COMMENT ON TABLE user_notification_settings IS '用户通知设置表，管理用户的订阅偏好';
COMMENT ON TABLE message_history IS '消息历史记录表，记录所有发送给用户的消息';

COMMENT ON COLUMN user_notification_settings.user_id IS '用户ID';
COMMENT ON COLUMN user_notification_settings.order_updates IS '订单更新通知开关';
COMMENT ON COLUMN user_notification_settings.payment_notifications IS '支付通知开关';
COMMENT ON COLUMN user_notification_settings.delivery_notifications IS '配送通知开关';
COMMENT ON COLUMN user_notification_settings.promotional_notifications IS '推广通知开关';

COMMENT ON COLUMN message_history.user_id IS '接收消息的用户ID';
COMMENT ON COLUMN message_history.message_type IS '消息类型';
COMMENT ON COLUMN message_history.template_id IS '微信模板消息ID';
COMMENT ON COLUMN message_history.content IS '消息内容（JSON格式）';
COMMENT ON COLUMN message_history.status IS '发送状态：PENDING-待发送，SENT-已发送，FAILED-发送失败，RETRYING-重试中';
COMMENT ON COLUMN message_history.sent_at IS '实际发送时间';
COMMENT ON COLUMN message_history.wx_message_id IS '微信返回的消息ID';
COMMENT ON COLUMN message_history.error_message IS '错误信息';
COMMENT ON COLUMN message_history.retry_count IS '重试次数';