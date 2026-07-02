-- 对账模块数据库表创建
-- 创建时间：2024-01-XX
-- 描述：添加订单对账相关表

-- 创建对账任务表
CREATE TABLE IF NOT EXISTS reconciliation_tasks (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL UNIQUE,
    task_type VARCHAR(20) NOT NULL CHECK (task_type IN ('PAYMENT', 'REFUND', 'SETTLEMENT')),
    status VARCHAR(20) NOT NULL CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED')),
    task_date TIMESTAMP NOT NULL,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    total_records INTEGER DEFAULT 0 CHECK (total_records >= 0),
    matched_records INTEGER DEFAULT 0 CHECK (matched_records >= 0),
    unmatched_records INTEGER DEFAULT 0 CHECK (unmatched_records >= 0),
    error_message TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建对账差异表
CREATE TABLE IF NOT EXISTS reconciliation_discrepancies (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    discrepancy_type VARCHAR(20) NOT NULL CHECK (discrepancy_type IN ('MISSING', 'MISMATCH', 'EXTRA')),
    source_system VARCHAR(20) NOT NULL CHECK (source_system IN ('INTERNAL', 'WECHAT')),
    record_id VARCHAR(100) NOT NULL,
    record_details JSONB,
    status VARCHAR(20) NOT NULL DEFAULT 'UNRESOLVED' CHECK (status IN ('UNRESOLVED', 'RESOLVED')),
    resolution_notes TEXT,
    resolved_by VARCHAR(100),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建对账报表表
CREATE TABLE IF NOT EXISTS reconciliation_reports (
    id BIGSERIAL PRIMARY KEY,
    task_id VARCHAR(100) NOT NULL,
    report_type VARCHAR(20) NOT NULL,
    report_data JSONB NOT NULL,
    file_path VARCHAR(500),
    file_name VARCHAR(255),
    file_size BIGINT,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引以优化查询性能
-- 对账任务表索引
CREATE INDEX IF NOT EXISTS idx_reconciliation_tasks_task_id ON reconciliation_tasks(task_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_tasks_task_type ON reconciliation_tasks(task_type);
CREATE INDEX IF NOT EXISTS idx_reconciliation_tasks_status ON reconciliation_tasks(status);
CREATE INDEX IF NOT EXISTS idx_reconciliation_tasks_task_date ON reconciliation_tasks(task_date);
CREATE INDEX IF NOT EXISTS idx_reconciliation_tasks_composite ON reconciliation_tasks(task_date, status, created_at);
CREATE INDEX IF NOT EXISTS idx_reconciliation_tasks_type_status ON reconciliation_tasks(task_type, status);

-- 对账差异表索引
CREATE INDEX IF NOT EXISTS idx_reconciliation_discrepancies_task_id ON reconciliation_discrepancies(task_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_discrepancies_discrepancy_type ON reconciliation_discrepancies(discrepancy_type);
CREATE INDEX IF NOT EXISTS idx_reconciliation_discrepancies_source_system ON reconciliation_discrepancies(source_system);
CREATE INDEX IF NOT EXISTS idx_reconciliation_discrepancies_status ON reconciliation_discrepancies(status);
CREATE INDEX IF NOT EXISTS idx_reconciliation_discrepancies_record_id ON reconciliation_discrepancies(record_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_discrepancies_task_created ON reconciliation_discrepancies(task_id, created_at);
CREATE INDEX IF NOT EXISTS idx_reconciliation_discrepancies_resolution ON reconciliation_discrepancies(status, discrepancy_type);

-- 对账报表表索引
CREATE INDEX IF NOT EXISTS idx_reconciliation_reports_task_id ON reconciliation_reports(task_id);
CREATE INDEX IF NOT EXISTS idx_reconciliation_reports_report_type ON reconciliation_reports(report_type);
CREATE INDEX IF NOT EXISTS idx_reconciliation_reports_generated_at ON reconciliation_reports(generated_at);
CREATE INDEX IF NOT EXISTS idx_reconciliation_reports_task_type ON reconciliation_reports(task_id, report_type);

-- 添加外键约束
ALTER TABLE reconciliation_discrepancies
ADD CONSTRAINT IF NOT EXISTS fk_reconciliation_discrepancies_task_id
FOREIGN KEY (task_id) REFERENCES reconciliation_tasks(task_id) ON DELETE CASCADE;

ALTER TABLE reconciliation_reports
ADD CONSTRAINT IF NOT EXISTS fk_reconciliation_reports_task_id
FOREIGN KEY (task_id) REFERENCES reconciliation_tasks(task_id) ON DELETE CASCADE;

-- 添加表注释
COMMENT ON TABLE reconciliation_tasks IS '对账任务表，记录对账任务的执行状态和结果统计';
COMMENT ON TABLE reconciliation_discrepancies IS '对账差异表，记录对账过程中发现的差异';
COMMENT ON TABLE reconciliation_reports IS '对账报表表，存储对账结果报表';

-- 添加列注释
COMMENT ON COLUMN reconciliation_tasks.task_id IS '任务唯一标识符';
COMMENT ON COLUMN reconciliation_tasks.task_type IS '任务类型：PAYMENT-支付对账，REFUND-退款对账，SETTLEMENT-结算对账';
COMMENT ON COLUMN reconciliation_tasks.status IS '任务状态：PENDING-等待执行，RUNNING-执行中，SUCCESS-成功，FAILED-失败';
COMMENT ON COLUMN reconciliation_tasks.task_date IS '对账任务日期';
COMMENT ON COLUMN reconciliation_tasks.total_records IS '总记录数';
COMMENT ON COLUMN reconciliation_tasks.matched_records IS '匹配记录数';
COMMENT ON COLUMN reconciliation_tasks.unmatched_records IS '未匹配记录数';

COMMENT ON COLUMN reconciliation_discrepancies.task_id IS '关联的对账任务ID';
COMMENT ON COLUMN reconciliation_discrepancies.discrepancy_type IS '差异类型：MISSING-缺失，MISMATCH-不匹配，EXTRA-多余';
COMMENT ON COLUMN reconciliation_discrepancies.source_system IS '源系统：INTERNAL-内部系统，WECHAT-微信支付';
COMMENT ON COLUMN reconciliation_discrepancies.record_id IS '差异记录的标识符';
COMMENT ON COLUMN reconciliation_discrepancies.record_details IS '差异记录详细信息（JSON格式）';
COMMENT ON COLUMN reconciliation_discrepancies.status IS '处理状态：UNRESOLVED-未解决，RESOLVED-已解决';

COMMENT ON COLUMN reconciliation_reports.task_id IS '关联的对账任务ID';
COMMENT ON COLUMN reconciliation_reports.report_type IS '报表类型：SUMMARY-汇总，DETAIL-明细，EXCEL_EXPORT-Excel导出';
COMMENT ON COLUMN reconciliation_reports.report_data IS '报表数据（JSON格式）';
COMMENT ON COLUMN reconciliation_reports.file_path IS '报表文件路径';
COMMENT ON COLUMN reconciliation_reports.file_name IS '报表文件名';
COMMENT ON COLUMN reconciliation_reports.file_size IS '报表文件大小（字节）';
COMMENT ON COLUMN reconciliation_reports.generated_at IS '报表生成时间';

-- 创建触发器以自动更新 updated_at 字段
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 为 reconciliation_tasks 表创建触发器
DROP TRIGGER IF EXISTS update_reconciliation_tasks_updated_at ON reconciliation_tasks;
CREATE TRIGGER update_reconciliation_tasks_updated_at
    BEFORE UPDATE ON reconciliation_tasks
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

-- 为 reconciliation_discrepancies 表创建触发器
DROP TRIGGER IF EXISTS update_reconciliation_discrepancies_updated_at ON reconciliation_discrepancies;
CREATE TRIGGER update_reconciliation_discrepancies_updated_at
    BEFORE UPDATE ON reconciliation_discrepancies
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();