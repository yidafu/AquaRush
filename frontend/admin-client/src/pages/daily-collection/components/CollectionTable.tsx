import React from 'react';
import { Table, Tag, Button, Space } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';
import type { DailyCollectionRecord } from '../../../types/daily-collection';
import { statusColorMap, statusTextMap } from './constants';

const styles = stylex.create({
  diffOk: {
    color: tokens.success,
  },
  diffBad: {
    color: tokens.error,
  },
  rejectInfo: {
    color: tokens.error,
    fontSize: '12px',
  },
});

export interface CollectionTableProps {
  dataSource: DailyCollectionRecord[];
  loading: boolean;
  onConfirm: (record: DailyCollectionRecord) => void;
  onReject: (record: DailyCollectionRecord) => void;
  /** 服务端分页配置(规则 #13) */
  pagination?: {
    current: number;
    pageSize: number;
    total: number;
    showSizeChanger?: boolean;
    onChange: (page: number, pageSize: number) => void;
  };
}

/** 每日收款记录表格(daily-collection/index.tsx 拆分) */
const CollectionTable: React.FC<CollectionTableProps> = ({
  dataSource,
  loading,
  onConfirm,
  onReject,
  pagination,
}) => {
  const columns: ColumnsType<DailyCollectionRecord> = [
    {
      title: '配送员ID',
      dataIndex: 'deliveryWorkerId',
      key: 'deliveryWorkerId',
    },
    {
      title: '收款日期',
      dataIndex: 'collectionDate',
      key: 'collectionDate',
    },
    {
      title: '现金金额',
      dataIndex: 'cashAmountCents',
      key: 'cashAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '水票数量',
      dataIndex: 'waterTicketCount',
      key: 'waterTicketCount',
    },
    {
      title: '扫码金额',
      dataIndex: 'qrCodeAmountCents',
      key: 'qrCodeAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '订单数量',
      dataIndex: 'orderCount',
      key: 'orderCount',
    },
    {
      title: '订单金额',
      dataIndex: 'orderAmountCents',
      key: 'orderAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '系统计算现金',
      dataIndex: 'calculatedCashAmountCents',
      key: 'calculatedCashAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '系统计算扫码',
      dataIndex: 'calculatedQrCodeAmountCents',
      key: 'calculatedQrCodeAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '系统计算水票',
      dataIndex: 'calculatedWaterTicketCount',
      key: 'calculatedWaterTicketCount',
    },
    {
      title: '系统计算订单金额',
      dataIndex: 'calculatedOrderAmountCents',
      key: 'calculatedOrderAmountCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '差异',
      dataIndex: 'differenceAmountCents',
      key: 'differenceAmountCents',
      render: (cents: number | string) => {
        const value = Number(cents);
        return (
          <span {...stylex.props(Math.abs(value) <= 100 ? styles.diffOk : styles.diffBad)}>
            {displayCents(value)}
          </span>
        );
      },
    },
    {
      title: '差异说明',
      dataIndex: 'notes',
      key: 'notes',
      ellipsis: true,
      render: (notes?: string) => notes || '-',
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: DailyCollectionRecord['status']) => (
        <Tag color={statusColorMap[status]}>{statusTextMap[status] || status}</Tag>
      ),
    },
    {
      title: '驳回信息',
      key: 'rejectInfo',
      render: (_, record) =>
        record.rejectedAt ? (
          <span {...stylex.props(styles.rejectInfo)}>
            驳回: {record.rejectReason || '-'}
            <br />
            时间: {record.rejectedAt}
          </span>
        ) : (
          '-'
        ),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          {(record.status === 'SUBMITTED' || record.status === 'DISPUTED') && (
            <Button type="link" onClick={() => onConfirm(record)}>
              确认
            </Button>
          )}
          {(record.status === 'SUBMITTED' || record.status === 'DISPUTED') && (
            <Button type="link" danger onClick={() => onReject(record)}>
              驳回
            </Button>
          )}
        </Space>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={dataSource}
      loading={loading}
      rowKey="id"
      pagination={
        pagination ?? {
          pageSize: 10,
        }
      }
    />
  );
};

export default CollectionTable;
