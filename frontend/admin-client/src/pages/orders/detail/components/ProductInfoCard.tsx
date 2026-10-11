import React from 'react';
import { Card, Image, Typography, Space, List, Tag } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { useOrderDetail } from '../OrderDetailContext';
import { displayCents } from '@aquarush/common';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

const { Text } = Typography;

const styles = stylex.create({
  card: {
    marginBottom: '16px',
  },
  itemRow: {
    display: 'flex',
    alignItems: 'center',
    columnGap: '16px',
    width: '100%',
  },
  cover: {
    objectFit: 'cover',
    borderRadius: '8px',
  },
  placeholder: {
    width: '64px',
    height: '64px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    backgroundColor: tokens.bg,
    borderRadius: '8px',
  },
  placeholderIcon: {
    fontSize: '20px',
    color: tokens.textSub,
  },
  main: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: '0%',
  },
  priceCol: {
    textAlign: 'right',
  },
  price: {
    color: tokens.brand,
  },
});

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
      {...stylex.props(styles.card)}
    >
      {items.length === 0 ? (
        <Text type="secondary">暂无商品信息</Text>
      ) : (
        <List
          dataSource={items}
          renderItem={(item) => (
            <List.Item>
              <div {...stylex.props(styles.itemRow)}>
                {item.product?.coverImageUrl ? (
                  <Image
                    src={item.product.coverImageUrl}
                    alt={item.product.name}
                    width={64}
                    height={64}
                    {...stylex.props(styles.cover)}
                  />
                ) : (
                  <div {...stylex.props(styles.placeholder)}>
                    <ShoppingCartOutlined {...stylex.props(styles.placeholderIcon)} />
                  </div>
                )}
                <div {...stylex.props(styles.main)}>
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
                <div {...stylex.props(styles.priceCol)}>
                  <div>
                    <Text strong {...stylex.props(styles.price)}>
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