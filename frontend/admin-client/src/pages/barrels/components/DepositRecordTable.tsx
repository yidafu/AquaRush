import React from 'react';
import { Space, Select, Button, Table, Tag, Tooltip } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import type { DepositRecord } from '../../../types/barrel';
import { DEPOSIT_RECORD_STATUS_MAP } from './constants';
import { pagination } from '@/utils/table';

const styles = stylex.create({
  filterRow: {
    marginBottom: '16px',
  },
  statusSelect: {
    width: '140px',
  },
  specSelect: {
    width: '160px',
  },
});

export interface DepositRecordTableProps {
  dataSource: DepositRecord[];
  loading: boolean;
  total: number;
  statusFilter?: string;
  specification?: string;
  page: number;
  onStatusFilterChange: (value: string | undefined) => void;
  onSpecificationChange: (value: string) => void;
  onPageChange: (page: number) => void;
  onRefresh: () => void;
}

/** Tab2 押金记录:状态/规格筛选 + 押金收取→退还流水(R-MON-03) */
const DepositRecordTable: React.FC<DepositRecordTableProps> = ({
  dataSource,
  loading,
  total,
  statusFilter,
  specification,
  page,
  onStatusFilterChange,
  onSpecificationChange,
  onPageChange,
  onRefresh,
}) => {
  const columns: ColumnsType<DepositRecord> = [
    {
      title: '押金单号',
      dataIndex: 'id',
      key: 'id',
      width: 180,
      ellipsis: true,
    },
    {
      title: '订单ID',
      dataIndex: 'orderId',
      key: 'orderId',
      width: 150,
      ellipsis: true,
    },
    {
      title: '规格',
      dataIndex: 'specification',
      key: 'specification',
      render: (spec?: string | null) => spec || '-',
    },
    {
      title: '桶数',
      dataIndex: 'bucketCount',
      key: 'bucketCount',
      width: 80,
      render: (count?: number | null) => count ?? '-',
    },
    {
      title: '单桶押金',
      dataIndex: 'unitDepositCents',
      key: 'unitDepositCents',
      width: 110,
      render: (cents?: number | string | null) => (cents != null ? displayCents(Number(cents)) : '-'),
    },
    {
      title: '押金金额',
      dataIndex: 'depositAmountCents',
      key: 'depositAmountCents',
      width: 110,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: DepositRecord['status']) => {
        const info = DEPOSIT_RECORD_STATUS_MAP[status] || { text: status, color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '收取时间',
      dataIndex: 'collectedAt',
      key: 'collectedAt',
      width: 170,
      render: (t?: string | null) => (t ? new Date(t).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '退还时间',
      dataIndex: 'refundedAt',
      key: 'refundedAt',
      width: 170,
      render: (t?: string | null) => (t ? new Date(t).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '退还备注',
      dataIndex: 'refundRemark',
      key: 'refundRemark',
      ellipsis: true,
      render: (remark?: string | null) =>
        remark ? <Tooltip title={remark}>{remark}</Tooltip> : '-',
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (t: string) => (t ? new Date(t).toLocaleString('zh-CN') : '-'),
    },
  ];

  return (
    <>
      <Space {...stylex.props(styles.filterRow)} wrap>
        <Select
          allowClear
          placeholder="状态筛选"
          {...stylex.props(styles.statusSelect)}
          value={statusFilter}
          onChange={onStatusFilterChange}
          options={Object.entries(DEPOSIT_RECORD_STATUS_MAP).map(([value, info]) => ({
            value,
            label: info.text,
          }))}
        />
        <Select
          allowClear
          showSearch
          placeholder="规格筛选"
          {...stylex.props(styles.specSelect)}
          value={specification || undefined}
          onChange={(value) => onSpecificationChange(value ?? '')}
          options={Array.from(new Set(dataSource.map((r) => r.specification).filter(Boolean))).map(
            (spec) => ({ value: spec as string, label: spec as string }),
          )}
        />
        <Button onClick={onRefresh}>刷新</Button>
      </Space>
      <Table
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        rowKey="id"
        scroll={{ x: 1400 }}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: 10,
          onChange: (p) => onPageChange(p - 1),
        })}
      />
    </>
  );
};

export default DepositRecordTable;
