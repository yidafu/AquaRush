import React from 'react';
import { Card, Descriptions, Space } from '@aquarush/admin-ui';
import { DollarOutlined } from '@aquarush/admin-ui/icons';
import { useOrderDetail, formatDateTime } from '../OrderDetailContext';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  card: {
    marginBottom: '16px',
  },
});

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
      {...stylex.props(styles.card)}
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
