import React from 'react';
import { Table, Button, Space } from 'antd';
import { PlusOutlined } from '@ant-design/icons';
import { formatAdminTableAmount } from '../utils/money.ts';

const Products: React.FC = () => {
  const columns = [
    { title: '产品名称', dataIndex: 'name', key: 'name' },
    {
      title: '价格',
      dataIndex: 'price',
      key: 'price',
      render: (price: number | null | undefined) => (
        <span>{formatAdminTableAmount(price)}</span>
      ),
    },
    { title: '库存', dataIndex: 'stock', key: 'stock' },
    { title: '状态', dataIndex: 'status', key: 'status' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">编辑</Button>
          <Button type="link" danger>删除</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ marginBottom: 16 }}>
        <Button type="primary" icon={<PlusOutlined />}>
          新增产品
        </Button>
      </div>
      <Table columns={columns} dataSource={[]} />
    </div>
  );
};

export default Products;
