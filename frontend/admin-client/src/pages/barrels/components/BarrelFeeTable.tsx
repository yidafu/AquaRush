import React from 'react';
import { Space, Select, Button, Table, Tag } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { displayCents } from '@aquarush/common';
import type { BarrelFee } from '../../../types/barrel';
import { FEE_STATUS_MAP } from './constants';
import { pagination } from '@/utils/table';

export interface BarrelFeeTableProps {
  dataSource: BarrelFee[];
  loading: boolean;
  total: number;
  statusFilter?: string;
  page: number;
  onFilterChange: (value: string | undefined) => void;
  onPageChange: (page: number) => void;
  onRefresh: () => void;
  onCreate: () => void;
  onEdit: (fee: BarrelFee) => void;
  onDisable: (fee: BarrelFee) => void;
}

/** 价目表 Tab(barrels/index.tsx 拆分):筛选 + 刷新/新增按钮 + 价目表格 */
const BarrelFeeTable: React.FC<BarrelFeeTableProps> = ({
  dataSource,
  loading,
  total,
  statusFilter,
  page,
  onFilterChange,
  onPageChange,
  onRefresh,
  onCreate,
  onEdit,
  onDisable,
}) => {
  const columns: ColumnsType<BarrelFee> = [
    {
      title: '规格',
      dataIndex: 'specification',
      key: 'specification',
    },
    {
      title: '价格(元)',
      dataIndex: 'priceCents',
      key: 'priceCents',
      render: (cents: number | string) => displayCents(Number(cents)),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: BarrelFee['status']) => {
        const info = FEE_STATUS_MAP[status] || { text: status, color: 'default' };
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
      title: '操作',
      key: 'action',
      fixed: 'right',
      width: 150,
      render: (_, record) => (
        <Space>
          <Button type="link" onClick={() => onEdit(record)}>
            编辑
          </Button>
          {record.status === 'ENABLED' && (
            <Button type="link" danger onClick={() => onDisable(record)}>
              停用
            </Button>
          )}
        </Space>
      ),
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
          options={Object.entries(FEE_STATUS_MAP).map(([value, info]) => ({ value, label: info.text }))}
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
        scroll={{ x: 800 }}
        pagination={pagination(total, {
          current: page + 1,
          pageSize: 10,
          onChange: (p) => onPageChange(p - 1),
        })}
      />
    </>
  );
};

export default BarrelFeeTable;
