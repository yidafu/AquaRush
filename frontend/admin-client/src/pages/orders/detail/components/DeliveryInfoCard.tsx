import React from 'react';
import { Card, Descriptions, Empty, Space } from 'antd';
import { CarOutlined, PhoneOutlined } from '@ant-design/icons';
import { useOrderDetail, formatDateTime } from '../OrderDetailContext';

const DeliveryInfoCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order) return null;

  return (
    <Card
      title={
        <Space>
          <CarOutlined />
          配送信息
        </Space>
      }
      style={{ marginBottom: '16px' }}
    >
      {order.deliveryWorker ? (
        <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
          <Descriptions.Item label="配送员">{order.deliveryWorker.name || '-'}</Descriptions.Item>
          <Descriptions.Item label="联系电话">
            <Space>
              <PhoneOutlined />
              {order.deliveryWorker.phone || '-'}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="配送员ID" span={2}>
            {order.deliveryWorker.id}
          </Descriptions.Item>
          <Descriptions.Item label="开始配送时间">
            {formatDateTime(order.deliveryStartedAt)}
          </Descriptions.Item>
          <Descriptions.Item label="送达时间">
            {formatDateTime(order.deliveryConfirmedAt)}
          </Descriptions.Item>
        </Descriptions>
      ) : (
        <Empty description="暂无配送员分配" />
      )}
    </Card>
  );
};

export default DeliveryInfoCard;
