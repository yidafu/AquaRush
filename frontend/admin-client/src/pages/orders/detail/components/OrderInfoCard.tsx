import React from 'react';
import { Card, Descriptions, Tag, Space } from 'antd';
import { ShoppingCartOutlined } from '@ant-design/icons';
import { useOrderDetail, ORDER_STATUS_MAP, formatDateTime } from '../OrderDetailContext';
import { formatAdminTableAmount } from '../../../utils/money';

const OrderInfoCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order) return null;

  const statusInfo = ORDER_STATUS_MAP[order.status] || { text: order.status, color: 'default' };

  return (
    <Card
      title={
        <Space>
          <ShoppingCartOutlined />
          订单信息
        </Space>
      }
      style={{ marginBottom: '16px' }}
    >
      <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
        <Descriptions.Item label="订单编号">{order.orderNumber}</Descriptions.Item>
        <Descriptions.Item label="订单状态">
          <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
        </Descriptions.Item>
        <Descriptions.Item label="订单金额">
          {formatAdminTableAmount(order.amount)}
        </Descriptions.Item>
        <Descriptions.Item label="商品数量">{order.quantity}</Descriptions.Item>
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
