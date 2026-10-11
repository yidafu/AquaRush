import React from 'react';
import { Card, Descriptions, Space } from '@/ui';
import { EnvironmentOutlined, PhoneOutlined } from '@/ui/icons';
import { useOrderDetail } from '../OrderDetailContext';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  card: {
    marginBottom: '16px',
  },
});

const AddressCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order) return null;

  return (
    <Card
      title={
        <Space>
          <EnvironmentOutlined />
          收货地址
        </Space>
      }
      {...stylex.props(styles.card)}
    >
      <Descriptions column={1} bordered size="small">
        <Descriptions.Item label="收货人">{order.address?.receiverName || '-'}</Descriptions.Item>
        <Descriptions.Item label="联系电话">
          <Space>
            <PhoneOutlined />
            {order.address?.phone || '-'}
          </Space>
        </Descriptions.Item>
        <Descriptions.Item label="详细地址">
          {order.address
            ? `${order.address.province}${order.address.city}${order.address.district}${order.address.detailAddress}`
            : '-'}
        </Descriptions.Item>
        {order.address?.longitude && order.address?.latitude && (
          <Descriptions.Item label="坐标">
            {order.address.longitude}, {order.address.latitude}
          </Descriptions.Item>
        )}
      </Descriptions>
    </Card>
  );
};

export default AddressCard;
