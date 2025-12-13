import React, { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  Row,
  Col,
  Statistic,
  Tag,
  Avatar,
  Button,
  Tabs,
  Table,
  Spin,
  Descriptions,
  Progress,
  Popover,
} from 'antd';
import {
  UserOutlined,
  PhoneOutlined,
  EnvironmentOutlined,
  StarOutlined,
  TrophyOutlined,
  ShoppingOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  InfoCircleOutlined,
} from '@ant-design/icons';
import type { TabsProps } from 'antd';
import { useQuery } from '@apollo/client';
import {
  DELIVERY_WORKER_DETAIL_QUERY,
  DELIVERY_WORKER_STATISTICS_QUERY,
  DELIVERY_WORKER_ORDERS_QUERY,
} from '../../../../graphql/queries/user.graphql';
import type {
  DeliveryWorker,
  DeliveryWorkerStatisticsResponse,
  Order,
} from '../../../../types/graphql';
import { formatAdminTableAmount } from '../../../../utils/money';
import { OrderStatus } from '../../../../types/graphql';


const DeliveryWorkerDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState<string>('delivering');

  const deliveryWorkerId = id;

  const getTabStatus = (tabKey: string): OrderStatus | undefined => {
    switch (tabKey) {
      case 'delivering':
        return OrderStatus.OUT_FOR_DELIVERY;
      case 'pending':
        return OrderStatus.READY_FOR_DELIVERY;
      case 'completed':
        return OrderStatus.DELIVERED;
      default:
        return undefined;
    }
  };

  // Query for delivery worker basic info
  const {
    data: workerData,
    loading: workerLoading,
    error: workerError,
  } = useQuery(DELIVERY_WORKER_DETAIL_QUERY, {
    variables: { id: deliveryWorkerId },
    skip: !deliveryWorkerId,
    errorPolicy: 'all',
  });

  // Query for delivery worker statistics
  const {
    data: statisticsData,
    loading: statisticsLoading,
    error: statisticsError,
  } = useQuery(DELIVERY_WORKER_STATISTICS_QUERY, {
    variables: { deliveryWorkerId },
    skip: !deliveryWorkerId,
    errorPolicy: 'all',
  });

  // Query for orders by status
  const {
    data: ordersData,
    loading: ordersLoading,
    error: ordersError,
    refetch: refetchOrders,
  } = useQuery(DELIVERY_WORKER_ORDERS_QUERY, {
    variables: {
      deliveryWorkerId,
      status: getTabStatus(activeTab),
    },
    skip: !deliveryWorkerId,
    errorPolicy: 'all',
  });

  useEffect(() => {
    if (deliveryWorkerId) {
      refetchOrders({
        deliveryWorkerId,
        status: getTabStatus(activeTab),
      });
    }
  }, [activeTab, deliveryWorkerId, refetchOrders]);

  const deliveryWorker: DeliveryWorker | undefined = workerData?.deliveryWorker;
  const statistics: DeliveryWorkerStatisticsResponse | undefined = statisticsData?.deliveryWorkerStatistics;
  const orders: Order[] = ordersData?.ordersByUserAndStatus || [];

  const handleTabChange = (key: string) => {
    setActiveTab(key);
  };

  const getStatusColor = (status: OrderStatus) => {
    switch (status) {
      case OrderStatus.OUT_FOR_DELIVERY:
        return 'processing';
      case OrderStatus.READY_FOR_DELIVERY:
        return 'warning';
      case OrderStatus.DELIVERED:
        return 'success';
      default:
        return 'default';
    }
  };

  const getStatusText = (status: OrderStatus) => {
    switch (status) {
      case OrderStatus.OUT_FOR_DELIVERY:
        return '送货中';
      case OrderStatus.READY_FOR_DELIVERY:
        return '待派送';
      case OrderStatus.DELIVERED:
        return '已完成';
      default:
        return status;
    }
  };

  const orderColumns = [
    {
      title: '订单编号',
      dataIndex: 'orderNumber',
      key: 'orderNumber',
      width: 150,
    },
    {
      title: '下单用户',
      dataIndex: ['user', 'nickname'],
      key: 'userNickname',
      width: 120,
      render: (nickname: string, record: Order) => (
        <div>
          <div>{nickname || '未知用户'}</div>
          <div style={{ fontSize: '12px', color: '#999' }}>
            {record.user?.phone}
          </div>
        </div>
      ),
    },
    {
      title: '商品信息',
      dataIndex: ['product', 'name'],
      key: 'product',
      render: (productName: string, record: Order) => (
        <div>
          <div>{productName}</div>
          <div style={{ fontSize: '12px', color: '#999' }}>
            数量: {record.quantity}
          </div>
        </div>
      ),
    },
    {
      title: '订单金额',
      dataIndex: 'amount',
      key: 'amount',
      width: 100,
      render: (amount: number | null | undefined) => (
        <span>{formatAdminTableAmount(amount)}</span>
      ),
    },
    {
      title: '收货地址',
      dataIndex: ['address', 'detailAddress'],
      key: 'address',
      width: 200,
      render: (detailAddress: string, record: Order) => (
        <div>
          <div>{detailAddress}</div>
          <div style={{ fontSize: '12px', color: '#999' }}>
            {record.address?.receiverName} - {record.address?.phone}
          </div>
        </div>
      ),
    },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 120,
      render: (date: string) => new Date(date).toLocaleString(),
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 100,
      render: (status: OrderStatus) => (
        <Tag color={getStatusColor(status)}>
          {getStatusText(status)}
        </Tag>
      ),
    },
  ];

  const getTabCount = () => {
    if (!ordersData?.ordersByUserAndStatus) return 0;
    return ordersData.ordersByUserAndStatus.length;
  };

  const tabItems: TabsProps['items'] = [
    {
      key: 'delivering',
      label: (
        <span>
          <ShoppingOutlined />
          送货中
          <Tag style={{ marginLeft: 8 }}>
            {getTabCount()}
          </Tag>
        </span>
      ),
      children: (
        <Table
          columns={orderColumns}
          dataSource={activeTab === 'delivering' ? orders : []}
          rowKey="id"
          loading={ordersLoading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `第 ${range[0]}-${range[1]} 条/共 ${total} 条`,
          }}
          scroll={{ x: 1200 }}
        />
      ),
    },
    {
      key: 'pending',
      label: (
        <span>
          <ClockCircleOutlined />
          待派送
          <Tag style={{ marginLeft: 8 }}>
            {getTabCount()}
          </Tag>
        </span>
      ),
      children: (
        <Table
          columns={orderColumns}
          dataSource={activeTab === 'pending' ? orders : []}
          rowKey="id"
          loading={ordersLoading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `第 ${range[0]}-${range[1]} 条/共 ${total} 条`,
          }}
          scroll={{ x: 1200 }}
        />
      ),
    },
    {
      key: 'completed',
      label: (
        <span>
          <CheckCircleOutlined />
          历史订单
          <Tag style={{ marginLeft: 8 }}>
            {getTabCount()}
          </Tag>
        </span>
      ),
      children: (
        <Table
          columns={orderColumns}
          dataSource={activeTab === 'completed' ? orders : []}
          rowKey="id"
          loading={ordersLoading}
          pagination={{
            pageSize: 10,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total, range) =>
              `第 ${range[0]}-${range[1]} 条/共 ${total} 条`,
          }}
          scroll={{ x: 1200 }}
        />
      ),
    },
  ];

  if (workerLoading || statisticsLoading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (workerError || statisticsError || !deliveryWorker) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <h2>加载失败</h2>
        <p>无法获取送水员信息</p>
        <Button type="primary" onClick={() => navigate('/users')}>
          返回用户列表
        </Button>
      </div>
    );
  }

  const completionRate = deliveryWorker.totalOrders > 0
    ? (deliveryWorker.completedOrders / deliveryWorker.totalOrders) * 100
    : 0;

  return (
    <div style={{  minHeight: '100vh' }}>
      {/* Header with back button */}
      <div style={{ marginBottom: '12px' }}>
        <Button onClick={() => navigate('/users')}>
          ← 返回用户列表
        </Button>
      </div>

      {/* Basic Information Card */}
      <Card style={{ marginBottom: '24px' }}>
        <Row gutter={24}>
          <Col span={4}>
            <Avatar
              size={120}
              src={deliveryWorker.avatarUrl}
              icon={<UserOutlined />}
              style={{ marginBottom: '16px' }}
            />
          </Col>
          <Col span={20}>
            <Descriptions title="基本信息" column={3}>
              <Descriptions.Item label="姓名">
                {deliveryWorker.name}
              </Descriptions.Item>
              <Descriptions.Item label="手机号">
                <PhoneOutlined /> {deliveryWorker.phone}
              </Descriptions.Item>
              <Descriptions.Item label="状态">
                <Tag color={deliveryWorker.isAvailable ? 'success' : 'warning'}>
                  {deliveryWorker.isAvailable ? '在线' : '离线'}
                </Tag>
              </Descriptions.Item>
              <Descriptions.Item label="当前坐标">
                <EnvironmentOutlined /> {deliveryWorker.coordinates || '未设置'}
              </Descriptions.Item>
              <Descriptions.Item label="当前位置">
                {deliveryWorker.currentLocation || '未更新'}
              </Descriptions.Item>
              <Descriptions.Item label="微信OpenID">
                {deliveryWorker.wechatOpenId}
              </Descriptions.Item>
            </Descriptions>
          </Col>
        </Row>
      </Card>

      {/* Statistics Cards */}
      <Row gutter={16} style={{ marginBottom: '24px' }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总订单数"
              value={deliveryWorker.totalOrders}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已完成订单"
              value={deliveryWorker.completedOrders}
              prefix={<CheckCircleOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>


        <Col span={6}>
          <Card>

            <Statistic
              title={(
                <Popover content={(<div>
                  {Object.entries(statistics?.ratingDistribution || {}).map(([rating, count]) => (
                    <div key={rating} style={{ marginBottom: '4px' }}>
                      <span style={{ width: '40px', display: 'inline-block' }}>
                        {rating}星
                      </span>
                      <Progress
                        percent={(count as number / (statistics?.totalReviews ?? 1)) * 100}
                        showInfo={false}
                        strokeColor="#faad14"
                        style={{ width: '100px', display: 'inline-block', marginLeft: '8px' }}
                      />
                      <span style={{ marginLeft: '8px' }}>{count}</span>
                    </div>
                  ))}
                </div>)} title='评分分布'>
                  平均评分<InfoCircleOutlined />
                </Popover>
              )}
              value={statistics?.averageRating ?? 0}
              prefix={<StarOutlined />}
              precision={2}
              valueStyle={{ color: '#faad14' }}
              suffix={`/ 5.0`}
            />
          </Card>

        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="总评价数"
              value={statistics?.totalReviews}
              prefix={<TrophyOutlined />}
              valueStyle={{ color: '#722ed1' }}
            />
          </Card>
        </Col>
      </Row>

      {/* Order Tabs */}
      <Card>
        <Tabs activeKey={activeTab} onChange={handleTabChange} items={tabItems}>
          <Table
            columns={orderColumns}
            dataSource={orders}
            rowKey="id"
            loading={ordersLoading}
            pagination={{
              pageSize: 10,
              showSizeChanger: true,
              showQuickJumper: true,
              showTotal: (total, range) =>
                `第 ${range[0]}-${range[1]} 条/共 ${total} 条`,
            }}
            scroll={{ x: 1200 }}
          />
        </Tabs>
      </Card>
    </div>
  );
};

export default DeliveryWorkerDetailPage;
