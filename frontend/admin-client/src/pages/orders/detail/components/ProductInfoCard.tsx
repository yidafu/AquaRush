import React from 'react';
import { Card, Image, Typography, Space } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { useOrderDetail } from '../OrderDetailContext';

const { Text } = Typography;

const ProductInfoCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order) return null;

  return (
    <Card
      title={
        <Space>
          <ShoppingCartOutlined />
          商品信息
        </Space>
      }
      style={{ marginBottom: '16px' }}
    >
      <div style={{ display: 'flex', alignItems: 'center', gap: '16px' }}>
        {order.product?.imageUrl ? (
          <Image
            src={order.product.imageUrl}
            alt={order.product.name}
            width={80}
            height={80}
            style={{ objectFit: 'cover', borderRadius: '8px' }}
          />
        ) : (
          <div
            style={{
              width: 80,
              height: 80,
              display: 'flex',
              alignItems: 'center',
              justifyContent: 'center',
              background: '#f5f5f5',
              borderRadius: '8px',
            }}
          >
            <ShoppingCartOutlined style={{ fontSize: 24, color: '#999' }} />
          </div>
        )}
        <div>
          <Text strong style={{ fontSize: '16px' }}>
            {order.product?.name || '-'}
          </Text>
          <div>
            <Text type="secondary">商品ID: {order.product?.id}</Text>
          </div>
        </div>
      </div>
    </Card>
  );
};

export default ProductInfoCard;
