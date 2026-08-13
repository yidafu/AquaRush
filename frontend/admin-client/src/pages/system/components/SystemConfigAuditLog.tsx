import React, { useState, useEffect, useCallback } from 'react';
import { Table, Tag, Typography } from 'antd';
import { queryUserActionLogs } from '../../../services/logs';
import type { UserActionLogItem } from '../../../services/logs';

// 系统参数修改记录(R-SYS-01):读取 user_action_logs 中 target=system_config 的审计日志(SYS_CONFIG_SAVE/SYS_CONFIG_RESTORE)
const ACTION_TYPE_MAP: Record<string, { label: string; color: string }> = {
  SYS_CONFIG_SAVE: { label: '保存', color: 'blue' },
  SYS_CONFIG_RESTORE: { label: '恢复默认', color: 'orange' },
};

interface BeforeAfterChange {
  key: string;
  before: string | null;
  after: string | null;
}

// 解析审计 properties JSON: {"before": {...}, "after": {...}, "description": ...}
const parseChanges = (properties?: string): BeforeAfterChange[] => {
  if (!properties) return [];
  try {
    const parsed = JSON.parse(properties);
    const before: Record<string, unknown> = parsed.before || {};
    const after: Record<string, unknown> = parsed.after || {};
    const keys = new Set([...Object.keys(before), ...Object.keys(after)]);
    return [...keys].map((key) => ({
      key,
      before: before[key] == null ? null : String(before[key]),
      after: after[key] == null ? null : String(after[key]),
    }));
  } catch {
    return [];
  }
};

const SystemConfigAuditLog: React.FC = () => {
  const [data, setData] = useState<UserActionLogItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });

  const fetchLogs = useCallback(async () => {
    setLoading(true);
    try {
      const result = await queryUserActionLogs({
        target: 'system_config',
        page: pagination.current - 1,
        size: pagination.pageSize,
      });
      setData(result.content);
      setPagination((prev) => ({ ...prev, total: result.totalElements }));
    } catch (error) {
      console.error('Failed to fetch system config audit logs:', error);
    } finally {
      setLoading(false);
    }
  }, [pagination.current, pagination.pageSize]);

  useEffect(() => {
    fetchLogs();
  }, [fetchLogs]);

  const handleTableChange = (newPagination: any) => {
    setPagination({
      current: newPagination.current,
      pageSize: newPagination.pageSize,
      total: pagination.total,
    });
  };

  const columns = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => (date ? new Date(date).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作人',
      dataIndex: 'username',
      key: 'username',
      width: 140,
      render: (name: string, record: UserActionLogItem) => name || record.userId || '-',
    },
    {
      title: '操作类型',
      dataIndex: 'actionType',
      key: 'actionType',
      width: 120,
      render: (type: string) => {
        const info = ACTION_TYPE_MAP[type] || { label: type, color: 'default' };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '变更详情',
      key: 'changes',
      render: (_: unknown, record: UserActionLogItem) => {
        const changes = parseChanges(record.properties);
        if (changes.length === 0) return <Typography.Text type="secondary">-</Typography.Text>;
        return (
          <div>
            {changes.map((change) => (
              <div key={change.key}>
                <Typography.Text code>{change.key}</Typography.Text>
                {'  '}
                <Typography.Text delete type="secondary">{change.before ?? '空'}</Typography.Text>
                {' → '}
                <Typography.Text strong>{change.after ?? '空'}</Typography.Text>
              </div>
            ))}
          </div>
        );
      },
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={data}
      loading={loading}
      rowKey="id"
      size="small"
      pagination={{
        ...pagination,
        showSizeChanger: true,
        showTotal: (total) => `共 ${total} 条记录`,
      }}
      onChange={handleTableChange}
    />
  );
};

export default SystemConfigAuditLog;
