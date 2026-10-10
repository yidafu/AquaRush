import React from 'react';
import { Space, Select, Input, Button, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { displayCents } from '@aquarush/common';
import type { DepositPrice } from '../../../types/barrel';
import { DEPOSIT_PRICE_STATUS_MAP } from './constants';
import { pagination } from '@/utils/table';

export interface DepositPriceTableProps {
  dataSource: DepositPrice[];
  loading: boolean;
  total: number;
  statusFilter?: string;
  page: number;
  onStatusFilterChange: (value: string | undefined) => void;
  onKeywordChange: (value: string) => void;
  onPageChange: (page: number) => void;
  onRefresh: () => void;
  onCreate: () => void;
  onEdit: (record: DepositPrice) => void;
}

/** Tab1 押金价目:状态/关键词筛选 + 新增/编辑(押金按规格独立配置,R-MON-03) */
const DepositPriceTable: React.FC<DepositPriceTableProps> = ({
  dataSource,
  loading,
  total,
  statusFilter,
  page,
  onStatusFilterChange,
  onKeywordChange,
  onPageChange,
  onRefresh,
  onCreate,
  onEdit,
}) => {
  const columns: ColumnsType<DepositPrice> = [
    {
      title: '规格',
      dataIndex: 'specification',
      key: 'specification',
    },
    {
      title: '押金(元)',
      dataIndex: 'depositCents',
      key: 'depositCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: DepositPrice['status']) => {
        const info = DEPOSIT_PRICE_STATUS_MAP[status] || { text: status, color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '排序',
      dataIndex: 'sortOrder',
      key: 'sortOrder',
      width: 90,
    },
    {
      title: '更新时间',
      dataIndex: 'updatedAt',
      key: 'updatedAt',
      width: 170,
      render: (t: string) => (t ? new Date(t).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 110,
      render: (_, record) => (
        <Button type="link" onClick={() => onEdit(record)}>
          编辑
        </Button>
      ),
    },
  ];

  return (
    <>
      <Space style={{ marginBottom: 16 }} wrap>
        <Select
          allowClear
          placeholder="状态筛选"
          style={{ width: 140 }}
          value={statusFilter}
          onChange={onStatusFilterChange}
          options={Object.entries(DEPOSIT_PRICE_STATUS_MAP).map(([value, info]) => ({
            value,
            label: info.text,
          }))}
        />
        <Input.Search
          allowClear
          placeholder="按规格搜索"
          style={{ width: 200 }}
          onSearch={(value) => onKeywordChange(value.trim())}
        />
        <Button onClick={onRefresh}>刷新</Button>
        <Button type="primary" onClick={onCreate}>
          新增价目
        </Button>
      </Space>
      <Table
        columns={columns}
        dataSource={dataSource}
        loading={loading}
        rowKey="id"
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

export default DepositPriceTable;
