import React from 'react';
import { Table, Tag, Button } from '@aquarush/admin-ui';
import type { ColumnsType } from '@aquarush/admin-ui';
import type { BusinessLogVo } from '@aquarush/common';
import { OPERATION_CONFIG } from '../../../../components/BusinessLogTimeline';
import { getLogLevelInfo } from './constants';
import { pagination } from '@/utils/table';

export interface BusinessLogTableProps {
  list: readonly BusinessLogVo[];
  loading: boolean;
  pageNum: number;
  pageSize: number;
  total: number;
  onPageChange: (pageNum: number, pageSize: number) => void;
  onViewDetail: (log: BusinessLogVo) => void;
}

/** 业务日志表格(logs/business/index.tsx 拆分) */
const BusinessLogTable: React.FC<BusinessLogTableProps> = ({
  list,
  loading,
  pageNum,
  pageSize,
  total,
  onPageChange,
  onViewDetail,
}) => {
  const columns: ColumnsType<BusinessLogVo> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 180,
      render: (date: string) => date ? new Date(date).toLocaleString('zh-CN') : '-',
    },
    {
      title: '级别',
      dataIndex: 'level',
      key: 'level',
      width: 90,
      render: (level: string) => {
        const info = getLogLevelInfo(level);
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '操作类型',
      dataIndex: 'operation',
      key: 'operation',
      width: 140,
      render: (operation: string | undefined, record: BusinessLogVo) => {
        const op = operation || record.message?.match(/Operation:\s*([^,\s]+)/)?.[1];
        return op ? (OPERATION_CONFIG[op]?.label ?? op) : '-';
      },
    },
    {
      title: 'Logger',
      dataIndex: 'loggerName',
      key: 'loggerName',
      width: 140,
      ellipsis: true,
    },
    {
      title: '消息',
      dataIndex: 'message',
      key: 'message',
      ellipsis: true,
    },
    {
      title: '用户',
      dataIndex: 'username',
      key: 'username',
      width: 120,
      render: (name: string, record: BusinessLogVo) => name || record.userId || '-',
    },
    {
      title: '关联ID',
      dataIndex: 'correlationId',
      key: 'correlationId',
      width: 180,
      ellipsis: true,
    },
    {
      title: '操作',
      key: 'action',
      width: 100,
      render: (_: unknown, record: BusinessLogVo) => (
        <Button type="link" size="small" onClick={() => onViewDetail(record)}>
          详情
        </Button>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={list}
      loading={loading}
      rowKey="id"
      pagination={pagination(total, {
        current: pageNum + 1,
        pageSize,
        showSizeChanger: true,
        showQuickJumper: true,
      })}
      onChange={(pagination) => {
        onPageChange((pagination.current ?? 1) - 1, pagination.pageSize ?? pageSize);
      }}
      scroll={{ x: 1200 }}
    />
  );
};

export default BusinessLogTable;
