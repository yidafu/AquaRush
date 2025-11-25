import React from 'react';
import { Table, Tag, Button, Space } from 'antd';

const Delivery: React.FC = () => {
  const columns = [
    { title: '配送员', dataIndex: 'workerName', key: 'workerName' },
    { title: '手机号', dataIndex: 'phone', key: 'phone' },
    {
      title: '在线状态',
      dataIndex: 'onlineStatus',
      key: 'onlineStatus',
      render: (status: string) => (
        <Tag color={status === 'ONLINE' ? 'green' : 'default'}>
          {status === 'ONLINE' ? '在线' : '离线'}
        </Tag>
      ),
    },
    { title: '今日配送', dataIndex: 'todayCount', key: 'todayCount' },
    {
      title: '操作',
      key: 'action',
      render: () => (
        <Space>
          <Button type="link">查看详情</Button>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <h1>配送管理</h1>
      <Table columns={columns} dataSource={[]} style={{ marginTop: 16 }} />
    </div>
  );
};

export default Delivery;
