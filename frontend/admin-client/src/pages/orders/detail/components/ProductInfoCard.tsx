import React from 'react';
import { Card, Image, Typography, Space, List, Tag } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { useOrderDetail } from '../OrderDetailContext';
import { displayCents } from '@aquarush/common';
import { BRAND_PRIMARY, TEXT_SECONDARY } from '../../../../config/theme';

const { Text } = Typography;

/**
 * 商品信息卡(F-ORD-004 多商品下单:展示订单商品明细 items,随单快照)
 */
const ProductInfoCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order) return null;

  const items = Array.isArray(order.items) && order.items.length > 0 ? order.items : [];

  return (
    <Card
      title={
        <Space>
          <ShoppingCartOutlined />
          商品信息{items.length > 1 ? '（共' + items.length + '种）' : ''}
        </Space>
      }
      style={{ marginBottom: '16px' }}
    >
      {items.length === 0 ? (
        <Text type="secondary">暂无商品信息</Text>
      ) : (
        <List
          dataSource={items}
          renderItem={(item) => (
            <List.Item>
              <div style={{ display: 'flex', alignItems: 'center', gap: '16px', width: '100%' }}>
                {item.product?.coverImageUrl ? (
                  <Image
                    src={item.product.coverImageUrl}
                    alt={item.product.name}
                    width={64}
                    height={64}
                    style={{ objectFit: 'cover', borderRadius: '8px' }}
                  />
                ) : (
                  <div
                    style={{
                      width: 64,
                      height: 64,
                      display: 'flex',
                      alignItems: 'center',
                      justifyContent: 'center',
                      background: '#F5F7FA',
                      borderRadius: '8px',
                    }}
                  >
                    <ShoppingCartOutlined style={{ fontSize: 20, color: TEXT_SECONDARY }} />
                  </div>
                )}
                <div style={{ flex: 1 }}>
                  <Space>
                    <Text strong>{item.product?.name || '商品'}</Text>
                    {item.product?.specification && (
                      <Tag>{item.product.specification}</Tag>
                    )}
                  </Space>
                  <div>
                    <Text type="secondary">商品ID: {item.productId}</Text>
                  </div>
                </div>
                <div style={{ textAlign: 'right' }}>
                  <div>
                    <Text strong style={{ color: BRAND_PRIMARY }}>
                      {displayCents(Number(item.unitPriceCents) || 0)}
                    </Text>
                    <Text type="secondary"> × {item.quantity}</Text>
                  </div>
                  <Text type="secondary">小计 {displayCents(Number(item.totalPriceCents) || 0)}</Text>
                </div>
              </div>
            </List.Item>
          )}
        />
      )}
    </Card>
  );
};

export default ProductInfoCard;