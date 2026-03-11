import React, { useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Row,
  Col,
  Typography,
  Button,
  Space,
  Spin,
  Alert,
  Card,
  Descriptions,
  Tag,
  Image,
  Empty,
  Form,
  Select,
  Modal,
  message
} from 'antd';
import Icon, {
  ArrowLeftOutlined,
  UserOutlined,
  EnvironmentOutlined,
  CarOutlined,
  DollarOutlined,
  ShoppingCartOutlined,
  PhoneOutlined,
  ClockCircleOutlined,
  ReloadOutlined
} from '@ant-design/icons';
import { useLazyQuery, useQuery, useMutation } from '@apollo/client';
import { GET_ORDER_DETAIL_QUERY } from '../../../graphql/queries/order.graphql';
import { ASSIGN_DELIVERY_WORKER_MUTATION } from '../../../graphql/mutations/order.graphql';
import { GET_ONLINE_DELIVERY_WORKERS_QUERY } from '../../../graphql/queries/delivery.graphql';
import { formatAdminTableAmount } from '../../../utils/money';

const { Title, Text } = Typography;

interface OrderDetailData {
  id: number;
  orderNumber: string;
  user: {
    id: number;
    nickname: string;
    phone: string;
    avatarUrl?: string;
  };
  status: string;
  amount: number;
  quantity: number;
  product: {
    id: number;
    name: string;
    imageUrl?: string;
  };
  address: {
    receiverName: string;
    phone: string;
    province: string;
    city: string;
    district: string;
    detailAddress: string;
    longitude?: number;
    latitude?: number;
  };
  deliveryWorker?: {
    id: number;
    name: string;
    phone: string;
    avatarUrl?: string;
  };
  paymentMethod?: string;
  paymentTransactionId?: string;
  paymentTime?: string;
  paymentType?: string;
  deliveryPhotos?: string[];
  isSelfCollect: boolean;
  deliveryStartedAt?: string;
  deliveryConfirmedAt?: string;
  completedAt?: string;
  remark?: string;
  createdAt: string;
  updatedAt: string;
}

interface OrderDetailQueryResult {
  order: OrderDetailData;
}

const ORDER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: '待支付', color: 'default' },
  PENDING_DELIVERY: { text: '待配送', color: 'blue' },
  DELIVERING: { text: '配送中', color: 'processing' },
  COMPLETED: { text: '已完成', color: 'green' },
  CANCELLED: { text: '已取消', color: 'red' },
  REFUNDED: { text: '已退款', color: 'magenta' },
};

const OrderDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  // 分配配送员相关状态
  const [assignModalVisible, setAssignModalVisible] = useState(false);
  const [selectedWorkerId, setSelectedWorkerId] = useState<number | null>(null);
  const [isSelfCollect, setIsSelfCollect] = useState(false);

  const [loadOrder, { data, loading, error }] = useLazyQuery<OrderDetailQueryResult>(
    GET_ORDER_DETAIL_QUERY,
    {
      fetchPolicy: 'cache-and-network',
      errorPolicy: 'all',
    }
  );

  // 查询在线配送员
  const { data: workersData, loading: workersLoading } = useQuery(GET_ONLINE_DELIVERY_WORKERS_QUERY, {
    errorPolicy: 'all',
  });

  // 分配配送员 mutation
  const [assignDeliveryWorker, { loading: assignLoading }] = useMutation(ASSIGN_DELIVERY_WORKER_MUTATION);

  const workers = workersData?.onlineDeliveryWorkers || [];

  React.useEffect(() => {
    if (id) {
      loadOrder({ variables: { id: parseInt(id, 10) } });
    }
  }, [id, loadOrder]);

  const handleBack = () => {
    navigate('/orders');
  };

  const handleRefreshOrder = () => {
    if (id) {
      loadOrder({ variables: { id: parseInt(id, 10) }, fetchPolicy: 'network-only' });
    }
  };

  const handleOpenAssignModal = () => {
    setSelectedWorkerId(null);
    setIsSelfCollect(order?.isSelfCollect || false);
    setAssignModalVisible(true);
  };

  const handleAssignWorker = async () => {
    if (!selectedWorkerId) {
      message.warning('请选择配送员');
      return;
    }

    try {
      await assignDeliveryWorker({
        variables: {
          orderId: parseInt(id!, 10),
          workerId: selectedWorkerId,
          isSelfCollect,
        },
      });
      message.success('分配配送员成功');
      setAssignModalVisible(false);
      // 刷新订单数据
      loadOrder({ variables: { id: parseInt(id!, 10) }, fetchPolicy: 'network-only' });
    } catch (err: any) {
      message.error(err.message || '分配配送员失败');
    }
  };

  if (loading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Spin size="large" tip="加载订单详情..." />
      </div>
    );
  }

  if (error || !data?.order) {
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

  const order = data.order;
  const statusInfo = ORDER_STATUS_MAP[order.status] || { text: order.status, color: 'default' };

  const formatDateTime = (dateStr?: string) => {
    if (!dateStr) return '-';
    return new Date(dateStr).toLocaleString('zh-CN');
  };

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
            <Button type="primary" onClick={handleOpenAssignModal}>
              分配配送员
            </Button>
          )}
          <Button icon={<ReloadOutlined />} onClick={handleRefreshOrder}> 更新状态</Button>
          <Button danger>取消订单</Button>
        </Space>
        <Text type="secondary" >订单号: <Text copyable>{order.orderNumber}</Text></Text>
      </div>

      {/* 订单基本信息 */}
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

      {/* 商品信息 */}
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

      {/* 用户信息 */}
      <Card
        title={
          <Space>
            <UserOutlined />
            用户信息
          </Space>
        }
        style={{ marginBottom: '16px' }}
      >
        <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
          <Descriptions.Item label="用户昵称">{order.user?.nickname || '-'}</Descriptions.Item>
          <Descriptions.Item label="手机号">
            <Space>
              <PhoneOutlined />
              {order.user?.phone || '-'}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="用户ID" span={2}>
            {order.user?.id}
          </Descriptions.Item>
        </Descriptions>
      </Card>

      {/* 收货地址 */}
      <Card
        title={
          <Space>
            <EnvironmentOutlined />
            收货地址
          </Space>
        }
        style={{ marginBottom: '16px' }}
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="收货人">{order.address?.receiverName || '-'}</Descriptions.Item>
          <Descriptions.Item label="联系电话">
            <Space>
              <PhoneOutlined />
              {order.address?.phone || '-'}
            </Space>
          </Descriptions.Item>
          <Descriptions.Item label="详细地址">
            {order.address
              ? `${order.address.province}${order.address.city}${order.address.district}${order.address.detailAddress}`
              : '-'}
          </Descriptions.Item>
          {order.address?.longitude && order.address?.latitude && (
            <Descriptions.Item label="坐标">
              {order.address.longitude}, {order.address.latitude}
            </Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      {/* 配送员信息 */}
      <Card
        title={
          <Space>
            <CarOutlined />
            配送信息
          </Space>
        }
        style={{ marginBottom: '16px' }}
      >
        {order.deliveryWorker ? (
          <Descriptions column={{ xs: 1, sm: 2 }} bordered size="small">
            <Descriptions.Item label="配送员">{order.deliveryWorker.name || '-'}</Descriptions.Item>
            <Descriptions.Item label="联系电话">
              <Space>
                <PhoneOutlined />
                {order.deliveryWorker.phone || '-'}
              </Space>
            </Descriptions.Item>
            <Descriptions.Item label="配送员ID" span={2}>
              {order.deliveryWorker.id}
            </Descriptions.Item>
            <Descriptions.Item label="开始配送时间">
              {formatDateTime(order.deliveryStartedAt)}
            </Descriptions.Item>
            <Descriptions.Item label="送达时间">
              {formatDateTime(order.deliveryConfirmedAt)}
            </Descriptions.Item>
          </Descriptions>
        ) : (
          <Empty description="暂无配送员分配" />
        )}
      </Card>

      {/* 支付信息 */}
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

      {/* 配送照片 */}
      {order.deliveryPhotos && order.deliveryPhotos.length > 0 && (
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
      )}

      {/* 时间线信息 */}
      <Card
        title={
          <Space>
            <ClockCircleOutlined />
            时间信息
          </Space>
        }
      >
        <Descriptions column={1} bordered size="small">
          <Descriptions.Item label="创建时间">{formatDateTime(order.createdAt)}</Descriptions.Item>
          <Descriptions.Item label="更新时间">{formatDateTime(order.updatedAt)}</Descriptions.Item>
          {order.completedAt && (
            <Descriptions.Item label="完成时间">{formatDateTime(order.completedAt)}</Descriptions.Item>
          )}
        </Descriptions>
      </Card>

      {/* 分配配送员弹窗 */}
      <Modal
        title="分配配送员"
        open={assignModalVisible}
        onOk={handleAssignWorker}
        onCancel={() => setAssignModalVisible(false)}
        confirmLoading={assignLoading}
        width={500}
      >
        <Form layout="vertical">
          <Form.Item
            label="选择配送员"
            rules={[{ required: true, message: '请选择配送员' }]}
          >
            <Select
              placeholder="选择配送员"
              loading={workersLoading}
              onChange={(value) => setSelectedWorkerId(value)}
              value={selectedWorkerId}
            >
              {workers.map((worker: any) => (
                <Select.Option key={worker.id} value={worker.id}>
                  {worker.name} - {worker.phone}
                </Select.Option>
              ))}
            </Select>
          </Form.Item>

          <Form.Item label="收款方式">
            <Select
              placeholder="选择收款方式"
              onChange={(value) => setIsSelfCollect(value === 'SELF_COLLECT')}
              value={isSelfCollect ? 'SELF_COLLECT' : 'NORMAL'}
            >
              <Select.Option value="NORMAL">普通订单（现场收款）</Select.Option>
              <Select.Option value="SELF_COLLECT">自收（水钱已收）</Select.Option>
            </Select>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default OrderDetailPage;
