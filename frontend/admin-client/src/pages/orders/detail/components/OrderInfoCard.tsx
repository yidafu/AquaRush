import React from 'react';
import { Card, Descriptions, Tag, Space } from '@aquarush/admin-ui';
import { ShoppingCartOutlined } from '@aquarush/admin-ui/icons';
import { useOrderDetail, formatDateTime } from '../OrderDetailContext';
import { displayCents, normalizeCents } from '@aquarush/common'
import { getOrderStatusConfig } from '../../../../constants/order';
import { toneToAntDColor } from '../../../../constants/status';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  card: {
    marginBottom: '16px',
  },
});

const OrderInfoCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order) return null;

  const { text: statusText, tone: statusTone } = getOrderStatusConfig(order.status);

  return (
    <Card
      title={
        <Space>
          <ShoppingCartOutlined />
          订单信息
        </Space>
      }
      {...stylex.props(styles.card)}
    >
      <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
        <Descriptions.Item label="订单编号">{order.orderNo}</Descriptions.Item>
        <Descriptions.Item label="订单状态">
          <Tag color={toneToAntDColor[statusTone]}>{statusText}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="订单金额">
          {displayCents(normalizeCents(order.amountCents))}
        </Descriptions.Item>
        <Descriptions.Item label="商品数量">
          {Array.isArray(order.items) && order.items.length > 0
            ? order.items.reduce((sum, it) => sum + (Number(it.quantity) || 0), 0)
            : '-'}
        </Descriptions.Item>
        <Descriptions.Item label="下单时间">{formatDateTime(order.createdAt)}</Descriptions.Item>
        <Descriptions.Item label="更新时间">{formatDateTime(order.updatedAt)}</Descriptions.Item>
        <Descriptions.Item label="收款类型">
          {order.isSelfCollect ? '自收' : '线下收款'}
        </Descriptions.Item>
        <Descriptions.Item label="备注" span={1}>
          {order.remark || '-'}
        </Descriptions.Item>
      </Descriptions>
    </Card>
  );
};

export default OrderInfoCard;