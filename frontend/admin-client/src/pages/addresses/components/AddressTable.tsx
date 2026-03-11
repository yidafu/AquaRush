import React from 'react';
import { Table, Tag, Button, Space, Popconfirm } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import { EditOutlined, DeleteOutlined } from '@ant-design/icons';
import type { Address } from './types';

interface AddressTableProps {
  addresses: Address[];
  loading?: boolean;
  onEdit: (record: Address) => void;
  onDelete: (record: Address) => void;
}

export const AddressTable: React.FC<AddressTableProps> = ({
  addresses,
  loading = false,
  onEdit,
  onDelete,
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
          <div style={{ fontWeight: 'bold', marginBottom: 4 }}>
            {record.province} {record.city} {record.district}
          </div>
          <div style={{ color: '#666', fontSize: '12px' }}>
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
      pagination={{
        pageSize: 20,
        showSizeChanger: true,
        showTotal: (total) => `共 ${total} 条`,
      }}
      scroll={{ x: 1000 }}
    />
  );
};
