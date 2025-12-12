import React from 'react';
import { Table, Tag, Empty, Card } from 'antd';
import type { ColumnsType } from 'antd/es/table';
import type { Address } from './types';

interface AddressListProps {
  addresses: Address[];
  loading?: boolean;
}

const AddressList: React.FC<AddressListProps> = ({ addresses, loading = false }) => {
  const columns: ColumnsType<Address> = [
    {
      title: '收货人姓名',
      dataIndex: 'receiverName',
      key: 'receiverName',
      width: 120,
    },
    {
      title: '收货人手机号',
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
  ];

  if (!addresses || addresses.length === 0) {
    return (
      <Card title="地址信息" style={{ marginTop: 16 }}>
        <Empty
          description="暂无地址信息"
          image={Empty.PRESENTED_IMAGE_SIMPLE}
        />
      </Card>
    );
  }

  return (
    <Card title="地址信息" style={{ marginTop: 16 }}>
      <Table
        columns={columns}
        dataSource={addresses}
        rowKey="id"
        loading={loading}
        pagination={false}
        size="small"
        scroll={{ x: 800 }}
      />
    </Card>
  );
};

export default AddressList;