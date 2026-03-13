import React from 'react';
import { Card, Descriptions, Space } from 'antd';
import { DollarOutlined } from '@ant-design/icons';
import { useOrderDetail, formatDateTime } from '../OrderDetailContext';

const PaymentInfoCard: React.FC = () => {
  const { order } = useOrderDetail();

  if (!order) return null;

  return (
    <Card
      title={
        <Space>
          <DollarOutlined />
          支付信息
        </Space>
      }
      style={{ marginBottom: '16px' }}
    >
      <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
        <Descriptions.Item label="支付方式">{order.paymentMethod || '-'}</Descriptions.Item>
        <Descriptions.Item label="支付类型">{order.paymentType || '-'}</Descriptions.Item>
        <Descriptions.Item label="支付时间">{formatDateTime(order.paymentTime)}</Descriptions.Item>
        <Descriptions.Item label="交易流水号">{order.paymentTransactionId || '-'}</Descriptions.Item>
      </Descriptions>
    </Card>
  );
};

export default PaymentInfoCard;
