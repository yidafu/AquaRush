import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Typography,
  Button,
  Space,
  Spin,
  Alert,
  Tag
} from 'antd';
import {
  ArrowLeftOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { OrderDetailProvider, useOrderDetail, ORDER_STATUS_MAP } from './OrderDetailContext';
import AssignDeliveryButton from '../../../components/AssignDeliveryButton';
import OrderOperationTimeline from '../../../components/OrderOperationTimeline';
import {
  OrderInfoCard,
  ProductInfoCard,
  UserInfoCard,
  AddressCard,
  DeliveryInfoCard,
  PaymentInfoCard,
  DeliveryPhotosCard,
} from './components';

const { Title, Text } = Typography;

const OrderDetailContent: React.FC = () => {
  const navigate = useNavigate();

  const {
    order,
    loading,
    error,
    refreshOrder,
    orderOperations
  } = useOrderDetail();

  const handleBack = () => {
    navigate('/orders');
  };

  if (loading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Spin size="large" tip="加载订单详情..." />
      </div>
    );
  }

  if (error || !order) {
    return (
      <div style={{ padding: '24px' }}>
        <Alert
          message="加载失败"
          description="订单详情加载失败，请稍后重试。"
          type="error"
          action={
            <Space>
              <Button onClick={handleBack}>返回列表</Button>
              <Button type="primary" onClick={() => window.location.reload()}>
                重新加载
              </Button>
            </Space>
          }
        />
      </div>
    );
  }

  const statusInfo = ORDER_STATUS_MAP[order.status] || { text: order.status, color: 'default' };

  return (
    <div style={{ padding: '24px' }}>
      {/* 页面头部 */}
      <div
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          marginBottom: '24px',
        }}
      >
        <Space wrap>
          <Button icon={<ArrowLeftOutlined />} onClick={handleBack}>
            返回列表
          </Button>
          <Title level={3} style={{ margin: 0 }}>
            订单详情
          </Title>
          <Tag color={statusInfo.color}>{statusInfo.text}</Tag>
          {order.status === 'PENDING_DELIVERY' && (
            <AssignDeliveryButton
              orderId={order.id}
              isSelfCollect={order.isSelfCollect}
              onAssigned={refreshOrder}
              type="primary"
            >
              分配送水员
            </AssignDeliveryButton>
          )}
          <Button icon={<ReloadOutlined />} onClick={refreshOrder}> 更新状态</Button>
          <Button danger>取消订单</Button>
        </Space>
        <Text type="secondary" >订单号: <Text copyable>{order.orderNo}</Text></Text>
      </div>

      {/* 订单卡片组件 */}
      <OrderInfoCard />
      <ProductInfoCard />
      <UserInfoCard />
      <AddressCard />
      <DeliveryInfoCard />
      <PaymentInfoCard />
      <DeliveryPhotosCard />

      {/* 操作记录时间线 */}
      <OrderOperationTimeline operations={orderOperations} />
    </div>
  );
};

const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();

  if (!id) {
    return (
      <div style={{ padding: '24px' }}>
        <Alert
          message="参数错误"
          description="缺少订单ID参数"
          type="error"
        />
      </div>
    );
  }

  return (
    <OrderDetailProvider orderId={id}>
      <OrderDetailContent />
    </OrderDetailProvider>
  );
};

export default OrderDetailPage;
