import React from 'react';
import { Space, Select, Button, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { displayCents } from '@aquarush/common';
import type { BarrelPurchase } from '../../../types/barrel';
import { PURCHASE_STATUS_MAP } from './constants';

export interface BarrelPurchaseTableProps {
  dataSource: BarrelPurchase[];
  loading: boolean;
  total: number;
  statusFilter?: string;
  page: number;
  onFilterChange: (value: string | undefined) => void;
  onPageChange: (page: number) => void;
  onRefresh: () => void;
  onRefund: (record: BarrelPurchase) => void;
}

/** 购桶记录 Tab(barrels/index.tsx 拆分):筛选 + 刷新按钮 + 购桶单表格 */
const BarrelPurchaseTable: React.FC<BarrelPurchaseTableProps> = ({
  dataSource,
  loading,
  total,
  statusFilter,
  page,
  onFilterChange,
  onPageChange,
  onRefresh,
  onRefund,
}) => {
  const columns: ColumnsType<BarrelPurchase> = [
    {
      title: '购桶单号',
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 170,
    },
    {
      title: '用户ID',
      dataIndex: 'userId',
      key: 'userId',
      width: 130,
    },
    {
      title: '购买明细',
      key: 'items',
      render: (_, record) =>
        record.items.map((item) => `${item.specification} × ${item.quantity}`).join('、') || '-',
    },
    {
      title: '金额',
      dataIndex: 'totalAmountCents',
      key: 'totalAmountCents',
      width: 110,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '状态',
      dataIndex: 'paymentStatus',
      key: 'paymentStatus',
      render: (status: string) => {
        const info = PURCHASE_STATUS_MAP[status] || { text: status, color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '支付时间',
      dataIndex: 'paidAt',
      key: 'paidAt',
      render: (t?: string) => (t ? new Date(t).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '退还时间/人',
      key: 'refunded',
      render: (_, record) =>
        record.refundedAt ? (
          <span style={{ fontSize: 12 }}>
            {new Date(record.refundedAt).toLocaleString('zh-CN')}
            <br />
            退还人: {record.refundedBy || '-'}
          </span>
        ) : (
          '-'
        ),
    },
    {
      title: '退还备注',
      dataIndex: 'refundRemark',
      key: 'refundRemark',
      ellipsis: true,
      render: (remark?: string) => remark || '-',
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 110,
      render: (_, record) =>
        record.paymentStatus === 'PAID' ? (
          <Button type="link" onClick={() => onRefund(record)}>
            退还水桶费
          </Button>
        ) : null,
    },
  ];

  return (
    <>
      <Space style={{ marginBottom: 16 }}>
        <Select
          allowClear
          placeholder="状态筛选"
          style={{ width: 140 }}
          value={statusFilter}
          onChange={onFilterChange}
          options={Object.entries(PURCHASE_STATUS_MAP).map(([value, info]) => ({ value, label: info.text }))}
        />
        <Button onClick={onRefresh}>刷新</Button>
      </Space>
      <Table
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1100 }}
        pagination={{
          current: page + 1,
          pageSize: 10,
          total,
          onChange: (p) => onPageChange(p - 1),
          showTotal: (total) => `共 ${total} 条`,
        }}
      />
    </>
  );
};

export default BarrelPurchaseTable;
