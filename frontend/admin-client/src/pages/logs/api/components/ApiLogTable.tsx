import React from 'react';
import { Table, Tag, Button } from '@aquarush/admin-ui';
import type { ColumnsType, TablePaginationConfig } from '@aquarush/admin-ui';
import type { ApiLogItem } from '../../../../services/logs';

const getStatusColor = (status?: number) => {
  if (!status) return 'default';
  if (status >= 200 && status < 300) return 'green';
  if (status >= 400 && status < 500) return 'orange';
  if (status >= 500) return 'red';
  return 'default';
};

export interface ApiLogTableProps {
  data: readonly ApiLogItem[];
  loading: boolean;
  pagination: TablePaginationConfig;
  onChange: (pagination: { current: number; pageSize: number }) => void;
  onViewDetail: (log: ApiLogItem) => void;
}

/** API 日志表格(logs/api/index.tsx 拆分) */
const ApiLogTable: React.FC<ApiLogTableProps> = ({
  data,
  loading,
  pagination,
  onChange,
  onViewDetail,
}) => {
  const columns: ColumnsType<ApiLogItem> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => date ? new Date(date).toLocaleString('zh-CN') : '-',
    },
    {
      title: '类型',
      dataIndex: 'requestType',
      key: 'requestType',
      width: 100,
      render: (type: string) => (
        <Tag color={type === 'HTTP' ? 'blue' : 'purple'}>{type}</Tag>
      ),
    },
    {
      title: '方法',
      dataIndex: 'method',
      key: 'method',
      width: 80,
    },
    {
      title: '状态',
      dataIndex: 'responseStatus',
      key: 'responseStatus',
      width: 80,
      render: (status: number) => (
        <Tag color={getStatusColor(status)}>{status || '-'}</Tag>
      ),
    },
    {
      title: 'URI',
      dataIndex: 'uri',
      key: 'uri',
      ellipsis: true,
    },
    {
      title: '耗时',
      dataIndex: 'durationMs',
      key: 'durationMs',
      width: 80,
      render: (ms: number) => ms ? `${ms}ms` : '-',
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (name: string, record: ApiLogItem) => name || record.userId || '-',
    },
    {
      title: 'IP',
      dataIndex: 'ipAddress',
      key: 'ipAddress',
      width: 130,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: ApiLogItem) => (
        <Button type="link" size="small" onClick={() => onViewDetail(record)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={data}
      loading={loading}
      rowKey="id"
      pagination={pagination}
      onChange={(p) => onChange({ current: p.current ?? 1, pageSize: p.pageSize ?? 20 })}
      scroll={{ x: 1200 }}
    />
  );
};

export default ApiLogTable;
