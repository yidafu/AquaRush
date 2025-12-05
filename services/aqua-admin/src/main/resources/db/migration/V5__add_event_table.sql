-- 事件表(Outbox模式)
CREATE TABLE IF NOT EXISTS events (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    event_type VARCHAR(100) NOT NULL,
    event_payload JSONB NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    next_run_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_events_status_next_run ON events(status, next_run_at);
CREATE INDEX idx_events_type ON events(event_type);
CREATE INDEX idx_events_created_at ON events(created_at);

COMMENT ON TABLE events IS '事件表,用于实现Outbox模式的异步事件处理';
COMMENT ON COLUMN events.event_type IS '事件类型: ORDER_PAID, ORDER_ASSIGNED, ORDER_DELIVERED等';
COMMENT ON COLUMN events.status IS '事件状态: PENDING, PROCESSING, COMPLETED, FAILED';
