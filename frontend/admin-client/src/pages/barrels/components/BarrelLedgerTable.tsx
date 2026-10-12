import React from 'react';
import { Space, Input, Button, Switch, Table, Tag } from '@/ui';
import type { ColumnsType } from '@/ui';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import type { BarrelLedgerRow } from '../../../types/barrel';
import { pagination } from '@/utils/table';

// 局部色值 #999 为原有辅助文字灰,共享 token 无对应值,保持原样(报告 (a))
const styles = stylex.create({
  customerSub: {
    fontSize: '12px',
    color: '#999',
  },
  filterRow: {
    marginBottom: '16px',
  },
  search: {
    width: '240px',
  },
});

export interface BarrelLedgerTableProps {
  dataSource: BarrelLedgerRow[];
  loading: boolean;
  total: number;
  onlyOwed: boolean;
  page: number;
  onKeywordChange: (value: string) => void;
  onOnlyOwedChange: (value: boolean) => void;
  onPageChange: (page: number) => void;
  onRefresh: () => void;
}

/** Tab4 桶账台账:关键词 + 「仅看有欠桶」筛选,按客户维度展示循环水桶(F-PRO-012) */
const BarrelLedgerTable: React.FC<BarrelLedgerTableProps> = ({
  dataSource,
  loading,
  total,
  onlyOwed,
  page,
  onKeywordChange,
  onOnlyOwedChange,
  onPageChange,
  onRefresh,
}) => {
  const columns: ColumnsType<BarrelLedgerRow> = [
    {
      title: '客户',
      key: 'customer',
      width: 200,
      render: (_, record) => (
        <span>
          {record.nickname || record.userId}
          <br />
          <span {...stylex.props(styles.customerSub)}>{record.phone || record.userId}</span>
        </span>
      ),
    },
    {
      title: '在租桶(个)',
      dataIndex: 'heldBucketCount',
      key: 'heldBucketCount',
      width: 110,
      render: (count: number) => <Tag color="blue">{count}</Tag>,
    },
    {
      title: '在租押金',
      dataIndex: 'depositHeldCents',
      key: 'depositHeldCents',
      width: 120,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '已退押金',
      dataIndex: 'depositRefundedCents',
      key: 'depositRefundedCents',
      width: 120,
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '已交付(个)',
      dataIndex: 'deliveredBucketCount',
      key: 'deliveredBucketCount',
      width: 110,
    },
    {
      title: '已回收(个)',
      dataIndex: 'recoveredBucketCount',
      key: 'recoveredBucketCount',
      width: 110,
    },
    {
      title: '欠桶(个)',
      dataIndex: 'owedBucketCount',
      key: 'owedBucketCount',
      width: 100,
      render: (count: number) =>
        count > 0 ? <Tag color="red">{count}</Tag> : <Tag color="default">0</Tag>,
    },
  ];

  return (
    <>
      <Space {...stylex.props(styles.filterRow)} wrap>
        <Input.Search
          allowClear
          placeholder="按客户昵称/手机号搜索"
          {...stylex.props(styles.search)}
          onSearch={(value) => onKeywordChange(value.trim())}
        />
        <Space>
          <span>仅看有欠桶</span>
          <Switch checked={onlyOwed} onChange={onOnlyOwedChange} />
        </Space>
        <Button onClick={onRefresh}>刷新</Button>
      </Space>
      <Table
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        rowKey="userId"
        scroll={{ x: 900 }}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: 10,
          onChange: (p) => onPageChange(p - 1),
        })}
      />
    </>
  );
};

export default BarrelLedgerTable;
