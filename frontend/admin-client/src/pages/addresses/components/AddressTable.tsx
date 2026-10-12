import React from 'react';
import { Table, Tag, Button, Space, Popconfirm } from '@/ui';
import type { ColumnsType, TablePaginationConfig } from '@/ui';
import { EditOutlined, DeleteOutlined } from '@/ui/icons';
import * as stylex from '@stylexjs/stylex';
import type { Address } from './types';
import { pagination } from '@/utils/table';

// 局部色值 #666 为原有辅助文字灰,共享 token 无对应值,保持原样(报告 (a))
const styles = stylex.create({
  regionName: {
    fontWeight: 'bold',
    marginBottom: '4px',
  },
  regionDetail: {
    color: '#666',
    fontSize: '12px',
  },
});

interface AddressTableProps {
  addresses: Address[];
  loading?: boolean;
  total: number;
  current: number;
  pageSize: number;
  onEdit: (record: Address) => void;
  onDelete: (record: Address) => void;
  onChange: (pagination: TablePaginationConfig) => void;
}

export const AddressTable: React.FC<AddressTableProps> = ({
  addresses,
  loading = false,
  total,
  current,
  pageSize,
  onEdit,
  onDelete,
  onChange,
}) => {
  const columns: ColumnsType<Address> = [
    {
      title: '收货人姓名',
      dataIndex: 'receiverName',
      key: 'receiverName',
      width: 120,
    },
    {
      title: '手机号',
      dataIndex: 'phone',
      key: 'phone',
      width: 130,
    },
    {
      title: '地址',
      key: 'address',
      width: 300,
      render: (_, record) => (
        <div>
          <div {...stylex.props(styles.regionName)}>
            {record.province} {record.city} {record.district}
          </div>
          <div {...stylex.props(styles.regionDetail)}>
            {record.detailAddress}
          </div>
        </div>
      ),
    },
    {
      title: '关联用户',
      dataIndex: 'userId',
      key: 'userId',
      width: 100,
      render: (userId: string | null) => (
        userId ? (
          <Tag color="blue">已关联</Tag>
        ) : (
          <Tag color="default">未关联</Tag>
        )
      ),
    },
    {
      title: '默认地址',
      dataIndex: 'isDefault',
      key: 'isDefault',
      width: 100,
      render: (isDefault: boolean) => (
        <Tag color={isDefault ? 'success' : 'default'}>
          {isDefault ? '是' : '否'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 160,
      render: (createdAt: string) => new Date(createdAt).toLocaleString('zh-CN'),
    },
    {
      title: '操作',
      key: 'action',
      width: 120,
      fixed: 'right',
      render: (_, record) => (
        <Space size="small">
          <Button
            type="link"
            icon={<EditOutlined />}
            onClick={() => onEdit(record)}
            size="small"
          >
            编辑
          </Button>
          <Popconfirm
            title="确定删除此地址吗？"
            onConfirm={() => onDelete(record)}
            okText="确定"
            cancelText="取消"
          >
            <Button
              type="link"
              danger
              icon={<DeleteOutlined />}
              size="small"
            >
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <Table
      columns={columns}
      dataSource={addresses}
      rowKey="id"
      loading={loading}
      pagination={pagination(total, {
        current,
        pageSize,
        showSizeChanger: true,
        showQuickJumper: true,
      })}
      onChange={onChange}
      scroll={{ x: 1000 }}
    />
  );
};
