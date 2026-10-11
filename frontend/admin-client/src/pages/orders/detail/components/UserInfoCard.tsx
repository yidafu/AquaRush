import React from 'react';
import { Card, Descriptions, Space } from '@/ui';
import { UserOutlined, PhoneOutlined } from '@/ui/icons';
import { useOrderDetail } from '../OrderDetailContext';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  card: {
    marginBottom: '16px',
  },
});

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
      {...stylex.props(styles.card)}
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
