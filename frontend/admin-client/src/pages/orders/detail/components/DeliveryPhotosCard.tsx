import React from 'react';
import { Card, Image, Space } from 'antd';
import { CarOutlined } from '@ant-design/icons';
import { useOrderDetail } from '../OrderDetailContext';

const DeliveryPhotosCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order || !order.deliveryPhotos || order.deliveryPhotos.length === 0) {
    return null;
  }

  return (
    <Card
      title={
        <Space>
          <CarOutlined />
          交付照片
        </Space>
      }
      style={{ marginBottom: '16px' }}
    >
      <Space size="middle" wrap>
        {order.deliveryPhotos.map((photo, index) => (
          <Image
            key={index}
            src={photo}
            alt={`交付照片 ${index + 1}`}
            width={120}
            height={120}
            style={{ objectFit: 'cover', borderRadius: '8px' }}
          />
        ))}
      </Space>
    </Card>
  );
};

export default DeliveryPhotosCard;
