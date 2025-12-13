import React from 'react';
import { Table, Tag, Space, Button } from 'antd';
import { formatAdminTableAmount } from '../utils/money.ts';

const Orders: React.FC = () => {
  const columns = [
    { title: '订单号', dataIndex: 'orderNumber', key: 'orderNumber' },
    { title: '用户', dataIndex: 'userName', key: 'userName' },
    { title: '产品', dataIndex: 'productName', key: 'productName' },
    {
      title: '金额',
      dataIndex: 'amount',
      key: 'amount',
      render: (amount: number | null | undefined) => (
        <span>{formatAdminTableAmount(amount)}</span>
      ),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => <Tag>{status}</Tag>,
    },
    { title: '下单时间', dataIndex: 'createdAt', key: 'createdAt' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">查看</Button>
          <Button type="link">分配配送</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h1>订单管理</h1>
      <Table columns={columns} dataSource={[]} style={{ marginTop: 16 }} />
    </div>
  );
};

export default Orders;
