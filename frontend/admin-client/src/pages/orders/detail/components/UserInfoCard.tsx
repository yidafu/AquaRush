import React from 'react';
import { Card, Descriptions, Space } from 'antd';
import { UserOutlined, PhoneOutlined } from '@ant-design/icons';
import { useOrderDetail } from '../OrderDetailContext';

const UserInfoCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order) return null;

  return (
    <Card
      title={
        <Space>
          <UserOutlined />
          用户信息
        </Space>
      }
      style={{ marginBottom: '16px' }}
    >
      <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
        <Descriptions.Item label="用户昵称">{order.user?.nickname || '-'}</Descriptions.Item>
        <Descriptions.Item label="手机号">
          <Space>
            <PhoneOutlined />
            {order.user?.phone || '-'}
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="用户ID" span={2}>
          {order.user?.id}
        </Descriptions.Item>
      </Descriptions>
    </Card>
  );
};

export default UserInfoCard;
