import React from 'react';
import { Table, Tag, Space, Button, Popconfirm, Tooltip } from '@/ui';
import type { TableProps } from '@/ui';
import { WarningOutlined } from '@/ui/icons';
import {
  TIME_SLOT_TEXT,
  formatCycleRule,
  formatItemsSummary,
  getSubscriptionStatusColor,
  getSubscriptionStatusText,
} from '../../../utils/subscription';
import type { SubscriptionData } from '../types';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// #999 为表格辅助灰字,共享 token 无对应项,保留字面量
const styles = stylex.create({
  meta: {
    fontSize: '12px',
    color: '#999',
  },
  warningIcon: {
    color: tokens.warning,
  },
});

export interface SubscriptionTableProps {
  dataSource: SubscriptionData[];
  loading: boolean;
  pagination: TableProps<SubscriptionData>['pagination'];
  /** 状态操作(暂停/恢复/终止)进行中:禁用行内操作按钮 */
  actionLoading?: boolean;
  onPause: (record: SubscriptionData) => void;
  onResume: (record: SubscriptionData) => void;
  onTerminate: (record: SubscriptionData) => void;
  onChange: TableProps<SubscriptionData>['onChange'];
}

/**
 * 周期订购计划表格(pages/subscriptions/index.tsx 拆分):
 * 列口径对齐设计稿(用户/商品摘要/周期规则/下次配送时间/状态/操作);
 * lastError 非空时状态列附「上次执行失败」警示(不静默失败)。
 */
const SubscriptionTable: React.FC<SubscriptionTableProps> = ({
  dataSource,
  loading,
  pagination,
  actionLoading = false,
  onPause,
  onResume,
  onTerminate,
  onChange,
}) => {
  const columns: TableProps<SubscriptionData>['columns'] = [
    {
      title: '用户',
      key: 'user',
      render: (_: unknown, record: SubscriptionData) => (
        <Space direction="vertical" size={0}>
          <span>{record.user?.nickname || '-'}</span>
          <span {...stylex.props(styles.meta)}>{record.user?.phone || '-'}</span>
        </Space>
      ),
    },
    {
      title: '商品摘要',
      key: 'items',
      render: (_: unknown, record: SubscriptionData) => formatItemsSummary(record.items),
      ellipsis: true,
    },
    {
      title: '周期规则',
      key: 'cycleRule',
      render: (_: unknown, record: SubscriptionData) => (
        <Space direction="vertical" size={0}>
          <span>{formatCycleRule(record)}</span>
          <span {...stylex.props(styles.meta)}>
            {TIME_SLOT_TEXT[record.timeSlot] || record.timeSlot}
          </span>
        </Space>
      ),
    },
    {
      title: '下次配送时间',
      dataIndex: 'nextRunDate',
      key: 'nextRunDate',
      render: (nextRunDate: string | null, record: SubscriptionData) =>
        record.status === 'TERMINATED' ? '—' : nextRunDate || '—',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string, record: SubscriptionData) => (
        <Space size={4}>
          <Tag color={getSubscriptionStatusColor(status)}>{getSubscriptionStatusText(status)}</Tag>
          {record.lastError && (
            <Tooltip title={`上次执行失败:${record.lastError}`}>
              <WarningOutlined {...stylex.props(styles.warningIcon)} aria-label="上次执行失败" />
            </Tooltip>
          )}
        </Space>
      ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_: unknown, record: SubscriptionData) => (
        <Space>
          {/* 状态机:生效中→暂停;已暂停→恢复;已终止不提供任何操作(不可恢复,需新建) */}
          {record.status === 'ACTIVE' && (
            <Button type="link" size="small" disabled={actionLoading} onClick={() => onPause(record)}>
              暂停
            </Button>
          )}
          {record.status === 'PAUSED' && (
            <Button type="link" size="small" disabled={actionLoading} onClick={() => onResume(record)}>
              恢复
            </Button>
          )}
          {record.status !== 'TERMINATED' && (
            <Popconfirm
              title="终止后不可恢复"
              description="终止计划后无法恢复,如需继续订购请重新新建"
              okText="终止"
              okButtonProps={{ danger: true }}
              cancelText="取消"
              onConfirm={() => onTerminate(record)}
            >
              <Button type="link" size="small" danger disabled={actionLoading}>
                终止
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table<SubscriptionData>
      columns={columns}
      dataSource={dataSource}
      loading={loading}
      rowKey="id"
      pagination={pagination}
      onChange={onChange}
      locale={{ emptyText: '暂无周期订购计划' }}
    />
  );
};

export default SubscriptionTable;
