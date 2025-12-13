import React from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card, Row,
  Col,
  Statistic,
  Tag,
  Avatar,
  Button,
  Table,
  Spin,
  Descriptions,
  Typography,
  Empty,
  Tabs,

} from 'antd';
import {
  UserOutlined,
  PhoneOutlined,
  ShoppingOutlined,
  DollarOutlined,
  CalendarOutlined,
} from '@ant-design/icons';
import { useQuery } from '@apollo/client';
import {
  GET_USER_DETAIL_QUERY,
  USER_ORDERS_QUERY,
} from '../../../graphql/queries/user.graphql';
import type {
  User,
  Order,
  Address,
} from '../../../types/graphql';
import { OrderStatus } from '../../../types/graphql';
import { formatAdminTableAmount, centsToYuan } from '../../../utils/money';
import AddressListUsingList from '../components/AddressListUsingList';

const { Title, Text } = Typography;

const UserDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();

  const userId = id;

  // Query for user basic info
  const {
    data: userData,
    loading: userLoading,
    error: userError,
  } = useQuery(GET_USER_DETAIL_QUERY, {
    variables: { id: userId },
    skip: !userId,
    errorPolicy: 'all',
  });

  // Query for user orders
  const {
    data: ordersData,
    loading: ordersLoading,
  } = useQuery(USER_ORDERS_QUERY, {
    variables: { userId },
    skip: !userId,
    errorPolicy: 'all',
  });

  const user: User | undefined = userData?.user;
  const userAddresses: Address[] = userData?.userAddresses || [];
  const orders: Order[] = ordersData?.ordersByUser || [];

  const getStatusColor = (status: OrderStatus) => {
    switch (status) {
      case OrderStatus.PENDING:
        return 'default';
      case OrderStatus.CONFIRMED:
        return 'processing';
      case OrderStatus.PREPARING:
        return 'processing';
      case OrderStatus.READY_FOR_DELIVERY:
        return 'warning';
      case OrderStatus.OUT_FOR_DELIVERY:
        return 'processing';
      case OrderStatus.DELIVERED:
        return 'success';
      case OrderStatus.CANCELLED:
        return 'error';
      case OrderStatus.REFUNDED:
        return 'error';
      default:
        return 'default';
    }
  };

  const getStatusText = (status: OrderStatus) => {
    switch (status) {
      case OrderStatus.PENDING:
        return '待确认';
      case OrderStatus.CONFIRMED:
        return '已确认';
      case OrderStatus.PREPARING:
        return '准备中';
      case OrderStatus.READY_FOR_DELIVERY:
        return '待派送';
      case OrderStatus.OUT_FOR_DELIVERY:
        return '送货中';
      case OrderStatus.DELIVERED:
        return '已完成';
      case OrderStatus.CANCELLED:
        return '已取消';
      case OrderStatus.REFUNDED:
        return '已退款';
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
      width: 120,
      render: (amount: number | null | undefined) => (
        <span>{formatAdminTableAmount(amount)}</span>
      ),
    },
    {
      title: '收货地址',
      dataIndex: ['address', 'detailAddress'],
      key: 'address',
      width: 250,
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
      title: '配送员',
      dataIndex: ['deliveryWorker', 'name'],
      key: 'deliveryWorker',
      width: 100,
      render: (name: string, record: Order) => (
        name ? (
          <div>
            <div>{name}</div>
            <div style={{ fontSize: '12px', color: '#999' }}>
              {record.deliveryWorker?.phone}
            </div>
          </div>
        ) : (
          <Text type="secondary">未分配</Text>
        )
      ),
    },
    {
      title: '支付方式',
      dataIndex: 'paymentMethod',
      key: 'paymentMethod',
      width: 100,
      render: (method: string) => {
        const methodMap: Record<string, string> = {
          WECHAT_PAY: '微信支付',
          ALIPAY: '支付宝',
          CASH_ON_DELIVERY: '货到付款',
        };
        return methodMap[method] || method;
      },
    },
    {
      title: '下单时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 150,
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

  // Calculate statistics
  const totalOrders = orders.length;
  const totalAmount = orders.reduce((sum, order) => sum + (order.amount || 0), 0);
  const totalAmountInYuan = centsToYuan(totalAmount);
  const completedOrders = orders.filter(order => order.status === OrderStatus.DELIVERED).length;
  const lastOrderTime = orders.length > 0
    ? new Date(Math.max(...orders.map(order => new Date(order.createdAt).getTime()))).toLocaleString()
    : '暂无订单';

  // Handle address actions
  const handleEditAddress = (id: string) => {
    console.log('Edit address:', id);
    // Implement edit address logic here
  };

  const handleDeleteAddress = (id: string) => {
    console.log('Delete address:', id);
    // Implement delete address logic here
  };

  const handleSetDefaultAddress = (id: string) => {
    console.log('Set default address:', id);
    // Implement set default address logic here
  };

  if (userLoading) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <Spin size="large" />
      </div>
    );
  }

  if (userError || !user) {
    return (
      <div style={{ padding: '24px', textAlign: 'center' }}>
        <h2>加载失败</h2>
        <p>无法获取用户信息</p>
        <Button type="primary" onClick={() => navigate('/users')}>
          返回用户列表
        </Button>
      </div>
    );
  }

  const tabItems = [
    {
      key: 'address',
      label: '收货地址',
      children: (
        <>
          {userAddresses && userAddresses.length > 0 ? (
            <AddressListUsingList
              addresses={userAddresses}
              loading={userLoading}
              onEdit={handleEditAddress}
              onDelete={handleDeleteAddress}
              onSetDefault={handleSetDefaultAddress}
            />
          ) : (
            <Empty description="暂无收货地址" />
          )}
        </>
      )
    }, {
      key: 'order',
      label: '订单记录',
      children: (
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
          scroll={{ x: 1400 }}
          locale={{
            emptyText: '该用户暂无订单记录',
          }}
        />
      )
    }
  ]

  return (
    <div style={{  minHeight: '100vh' }}>
      {/* Header with back button */}
      <div style={{ marginBottom: '12px' }}>
        <Button onClick={() => navigate('/users')} >
          ← 返回用户列表
        </Button>
      </div>

      {/* Basic Information Card */}
      <Card style={{ marginBottom: '24px' }}>
        <Row gutter={24}>
          <Col span={4}>
            <Avatar
              size={120}
              src={user.avatarUrl}
              icon={<UserOutlined />}
              style={{ marginBottom: '16px' }}
            />
          </Col>
          <Col span={20}>
            <Descriptions title="基本信息" column={3}>

              <Descriptions.Item label="用户ID">
                 {user.id || '未绑定'}
              </Descriptions.Item>

              <Descriptions.Item label="昵称">
                {user.nickname || '未设置'}
              </Descriptions.Item>
              <Descriptions.Item label="手机号">
                <PhoneOutlined /> {user.phone || '未绑定'}
              </Descriptions.Item>
              <Descriptions.Item label="微信OpenID">
                {user.wechatOpenId}
              </Descriptions.Item>

              <Descriptions.Item label="账户状态">
                <Tag color="success">正常</Tag>
              </Descriptions.Item>

              <Descriptions.Item label="最近下单时间">
                <CalendarOutlined /> {new Date(lastOrderTime).toLocaleString()}
              </Descriptions.Item>

              <Descriptions.Item label="注册时间">
                <CalendarOutlined /> {new Date(user.createdAt).toLocaleString()}
              </Descriptions.Item>
              <Descriptions.Item label="最后更新">
                {new Date(user.updatedAt).toLocaleString()}
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
              value={totalOrders}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="总消费金额"
              value={totalAmountInYuan}
              prefix={<DollarOutlined />}
              precision={2}
              valueStyle={{ color: '#f5222d' }}
              suffix="元"
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="已完成订单"
              value={completedOrders}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card>
            <Statistic
              title="平均订单金额"
              value={totalOrders > 0 ? totalAmountInYuan / totalOrders : 0}
              prefix={<DollarOutlined />}
              precision={2}
              suffix="元"
              valueStyle={{ color: '#f5222d' }}
            />
          </Card>
        </Col>
      </Row>

      <Tabs items={ tabItems}>
      </Tabs>

    </div>
  );
};

export default UserDetailPage;
