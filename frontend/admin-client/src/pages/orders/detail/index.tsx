import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Typography,
  Button,
  Space,
  Spin,
  Alert,
  Tag,
  Popconfirm,
  message
} from 'antd';
import {
  ArrowLeftOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { useMutation } from '@apollo/client';
import { CANCEL_ORDER_MUTATION } from '../../../graphql/mutations/order.graphql';
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
    orderOperations,
    workers,
    workersLoading,
  } = useOrderDetail();

  // 取消订单(F-ORD-006,G15 接线):仅待支付/待派单/待配送订单展示,其余状态后端亦拦截
  const [cancelOrder, { loading: cancelling }] = useMutation(CANCEL_ORDER_MUTATION, {
    onError: (err) => {
      console.error('取消订单失败:', err);
      message.error('取消订单失败: ' + err.message);
    },
  });

  const handleCancelOrder = async () => {
    try {
      await cancelOrder({ variables: { orderId: order?.id } });
      message.success('订单已取消');
      refreshOrder();
    } catch (err) {
      // onError 已提示
    }
  };

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
  // 派单前置状态=待派单(R-ORD-01 状态机「待派单→配送中」,G09)
  const canAssign = order.status === 'PENDING_DISPATCH';
  // 可取消状态:待支付/待派单/待配送(已支付取消自动退款,配送中/已完成不可取消)
  const canCancel = ['PENDING_PAYMENT', 'PENDING_DISPATCH', 'PENDING_DELIVERY'].includes(order.status);

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
          {canAssign && (
            <AssignDeliveryButton
              orderId={order.id}
              isSelfCollect={order.isSelfCollect}
              workers={workers}
              workersLoading={workersLoading}
              onAssigned={refreshOrder}
              type="primary"
            >
              分配送水员
            </AssignDeliveryButton>
          )}
          <Button icon={<ReloadOutlined />} onClick={refreshOrder}> 更新状态</Button>
          {canCancel && (
            <Popconfirm
              title="确定要取消这个订单吗？"
              description="取消后无法恢复；已支付订单将自动退款。"
              okText="取消订单"
              okButtonProps={{ danger: true }}
              cancelText="返回"
              onConfirm={handleCancelOrder}
            >
              <Button danger loading={cancelling}>取消订单</Button>
            </Popconfirm>
          )}
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
