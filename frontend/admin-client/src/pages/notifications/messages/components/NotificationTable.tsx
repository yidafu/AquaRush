import React from 'react';
import { Table, Tag, Button } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import dayjs from 'dayjs';
import type { NotificationRecord } from './constants';
import { TYPE_LABELS, RECEIVER_LABELS } from './constants';

export interface NotificationTableProps {
  list: readonly NotificationRecord[];
  loading: boolean;
  total: number;
  pageNum: number;
  pageSize: number;
  onPageChange: (pageNum: number, pageSize: number) => void;
  onViewDetail: (record: NotificationRecord) => void;
}

/** 消息列表表格(notifications/messages/index.tsx 拆分) */
const NotificationTable: React.FC<NotificationTableProps> = ({
  list,
  loading,
  total,
  pageNum,
  pageSize,
  onPageChange,
  onViewDetail,
}) => {
  const columns: ColumnsType<NotificationRecord> = [
    {
      title: '时间',
      dataIndex: 'createdAt',
      width: 170,
      render: (v: string) => dayjs(v).format('YYYY-MM-DD HH:mm'),
    },
    {
      title: '类型',
      dataIndex: 'type',
      width: 90,
      render: (v: string) => {
        const info = TYPE_LABELS[v] ?? { label: v, color: 'default' };
        return <Tag color={info.color}>{info.label}</Tag>;
      },
    },
    {
      title: '标题',
      dataIndex: 'title',
      ellipsis: true,
    },
    {
      title: '摘要',
      dataIndex: 'summary',
      ellipsis: true,
      render: (v?: string | null) => v || '-',
    },
    {
      title: '接收人',
      dataIndex: 'receiverType',
      width: 90,
      render: (v: string) => RECEIVER_LABELS[v] ?? v ?? '—',
    },
    {
      title: '关联单号',
      dataIndex: 'relatedNo',
      width: 130,
      render: (v?: string | null) => v || '-',
    },
    {
      title: '状态',
      dataIndex: 'read',
      width: 90,
      render: (v: boolean) => (v ? <Tag color='default'>已读</Tag> : <Tag color='red'>未读</Tag>),
    },
    {
      title: '操作',
      width: 80,
      render: (_, record) => <Button type='link' size='small' onClick={() => onViewDetail(record)}>详情</Button>,
    },
  ];

  return (
    <Table<NotificationRecord>
      rowKey='id'
      columns={columns}
      dataSource={list}
      loading={loading}
      pagination={{
        current: pageNum + 1,
        pageSize,
        total,
        showSizeChanger: true,
        pageSizeOptions: [10, 20, 50],
        onChange: (page, size) => {
          onPageChange(page - 1, size);
        },
        onShowSizeChange: (_current, size) => {
          onPageChange(0, size);
        },
      }}
    />
  );
};

export default NotificationTable;
