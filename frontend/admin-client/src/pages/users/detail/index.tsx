import React, { useEffect, useState } from 'react';
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
  TeamOutlined,
  WalletOutlined,
  MailOutlined,
} from '@ant-design/icons';
import { useQuery } from '@apollo/client';
import {
  USER_DETAIL_PAGE_DATA_QUERY,
} from '../../../graphql/queries/user.graphql';
import type {
  User,
  Order,
  Address,
  UserRole,
} from '@aquarush/common';
import { OrderStatus, UserRole as UserRoleEnum } from '@aquarush/common';
import { displayCents } from '@aquarush/common'
import AddressListUsingList from '../components/AddressListUsingList';
import { STATUS_MAPPINGS } from '../components/types';
import CouponSection from './sections/CouponSection';
import TicketSection from './sections/TicketSection';
import FeedbackSection from './sections/FeedbackSection';
import ReviewSection from './sections/ReviewSection';
import FavoriteSection from './sections/FavoriteSection';
import { BRAND_PRIMARY } from '../../../config/theme';

const { Text } = Typography;

const ROLE_LABELS: Record<UserRole, string> = {
  [UserRoleEnum.NONE]: '未分配',
  [UserRoleEnum.USER]: '普通用户',
  [UserRoleEnum.ADMIN]: '管理员',
  [UserRoleEnum.WORKER]: '送水员',
};

/** 用户详情页单页查询(UserDetailPageData)返回结构 */
interface UserDetailPageData {
  user?: User;
  userAddresses: Address[];
  orders: {
    content: Order[];
    totalElements: number;
    totalPages: number;
    number: number;
    size: number;
  };
  completedOrders: {
    totalElements: number;
  };
}

/**
 * 用户详情页(users/:id,PRD/07-user.md §F-USR-022):
 * 基本信息卡(含角色/邮箱/所属分组/余额/总消费) → 统计卡(全量口径) → Tabs
 * (收货地址/订单记录/优惠券/水票/意见反馈/评价记录/收藏商品),订单记录可下钻。
 */
const UserDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(10);

  const userId = id;

  // 单页单请求:用户基本信息 + 地址 + 分页订单 + 已完成订单数一次取回(规则 #10);
  // 分页/尺寸变化经 variables 变化自动重取
  const {
    data,
    loading: userLoading,
    error: userError,
  } = useQuery<UserDetailPageData>(USER_DETAIL_PAGE_DATA_QUERY, {
    variables: { id: userId as string, page, size: pageSize },
    skip: !userId,
    errorPolicy: 'all',
  });

  // 最近下单时间(orders 默认 createdAt 倒序,取第 0 页首条,分页时保持首载值)
  const [latestOrderTime, setLatestOrderTime] = useState<string | null>(null);
  useEffect(() => {
    if (page === 0 && data?.orders?.content?.length) {
      setLatestOrderTime(data.orders.content[0].createdAt);
    }
  }, [data, page]);

  const user: User | undefined = data?.user;
  const userAddresses: Address[] = data?.userAddresses || [];
  const orders: Order[] = data?.orders.content || [];

  const getStatusColor = (status: OrderStatus) => {
    switch (status) {
      case OrderStatus.PENDING_PAYMENT:
        return 'default';
      case OrderStatus.PENDING_DELIVERY:
        return 'processing';
      case OrderStatus.DELIVERING:
        return 'processing';
      case OrderStatus.COMPLETED:
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
      case OrderStatus.PENDING_PAYMENT:
        return '待支付';
      case OrderStatus.PENDING_DELIVERY:
        return '待配送';
      case OrderStatus.DELIVERING:
        return '配送中';
      case OrderStatus.COMPLETED:
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
      dataIndex: 'orderNo',
      key: 'orderNo',
      width: 150,
      render: (orderNo: string, record: Order) => (
        <Button
          type="link"
          size="small"
          style={{ padding: 0 }}
          onClick={(e) => {
            e.stopPropagation();
            navigate(`/orders/${record.id}`);
          }}
        >
          {orderNo}
        </Button>
      ),
    },
    {
      title: '商品信息',
      dataIndex: 'items',
      key: 'items',
      render: (items: Order['items']) => {
        const productNames = (items ?? []).map((item) => item.product?.name).filter(Boolean);
        const quantity = (items ?? []).reduce((sum, item) => sum + item.quantity, 0);
        return (
          <div>
            <div>{productNames.join('、') || '-'}</div>
            <div style={{ fontSize: '12px', color: '#999' }}>
              数量: {quantity}
            </div>
          </div>
        );
      },
    },
    {
      title: '订单金额',
      dataIndex: 'amountCents',
      key: 'amountCents',
      width: 120,
      render: (amountCents: number | null | undefined) => (
        <span>{displayCents(amountCents)}</span>
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

  // 统计口径(2026-09 修正:不再按当前分页计算)
  // 总订单数 = orders.totalElements(全量);总消费 = user.totalSpentCents(持久化权威值);
  // 已完成订单 = 按状态 COMPLETED 计数查询(completedOrders 别名);平均订单金额 = 总消费/总订单(除零守卫)
  const totalOrders = data?.orders.totalElements ?? 0;
  const totalSpentCents = user?.totalSpentCents ?? 0;
  const totalAmountInYuan = totalSpentCents / 100;
  const completedOrders = data?.completedOrders.totalElements ?? 0;
  const averageAmountInYuan = totalOrders > 0 ? totalAmountInYuan / totalOrders : 0;
  const lastOrderTime = latestOrderTime
    ? new Date(latestOrderTime).toLocaleString()
    : '暂无订单';

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

  const statusConfig = STATUS_MAPPINGS[user.status as keyof typeof STATUS_MAPPINGS];

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

              <Descriptions.Item label="用户角色">
                <Tag color="geekblue">{ROLE_LABELS[user.role] ?? user.role}</Tag>
              </Descriptions.Item>

              <Descriptions.Item label="账户状态">
                <Tag color={statusConfig?.color ?? 'default'}>
                  {statusConfig?.text ?? user.status}
                </Tag>
              </Descriptions.Item>

              <Descriptions.Item label="所属客户分组">
                <TeamOutlined /> {user.customerGroupName || '未分组'}
              </Descriptions.Item>

              <Descriptions.Item label="账户余额">
                <WalletOutlined /> {displayCents(user.balanceCents)}
              </Descriptions.Item>

              <Descriptions.Item label="总消费金额">
                <DollarOutlined /> {displayCents(user.totalSpentCents)}
              </Descriptions.Item>

              <Descriptions.Item label="邮箱">
                <MailOutlined /> {user.email || '未设置'}
              </Descriptions.Item>

              <Descriptions.Item label="最近下单时间">
                <CalendarOutlined /> {lastOrderTime}
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


      {/* Statistics Cards (全量口径) */}
      <Row gutter={16} style={{ marginBottom: '24px' }}>
        <Col span={6}>
          <Card>
            <Statistic
              title="总订单数"
              value={totalOrders}
              prefix={<ShoppingOutlined />}
              valueStyle={{ color: BRAND_PRIMARY }}
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
              value={averageAmountInYuan}
              prefix={<DollarOutlined />}
              precision={2}
              suffix="元"
              valueStyle={{ color: '#f5222d' }}
            />
          </Card>
        </Col>
      </Row>

      {/* 各区块以 Tab 切换(2026-09 决策:保持 Tabs 布局,PRD 07-user.md §F-USR-022) */}
      <Tabs
        defaultActiveKey="address"
        items={[
          {
            key: 'address',
            label: '收货地址',
            children: (
              <Card>
                {userAddresses && userAddresses.length > 0 ? (
                  <AddressListUsingList
                    addresses={userAddresses}
                    loading={userLoading}
                  />
                ) : (
                  <Empty description="暂无收货地址" />
                )}
              </Card>
            ),
          },
          {
            key: 'orders',
            label: '订单记录',
            children: (
              <Card>
                <Table
                  columns={orderColumns}
                  dataSource={orders}
                  rowKey="id"
                  loading={userLoading}
                  onRow={(record) => ({
                    onClick: () => navigate(`/orders/${record.id}`),
                  })}
                  rowClassName={() => 'clickable-row'}
                  pagination={{
                    current: page + 1,
                    pageSize,
                    total: data?.orders.totalElements ?? 0,
                    showSizeChanger: true,
                    showQuickJumper: true,
                    showTotal: (total, range) =>
                      `第 ${range[0]}-${range[1]} 条/共 ${total} 条`,
                    onChange: (nextPage, nextPageSize) => {
                      setPage(nextPage - 1);
                      setPageSize(nextPageSize);
                    },
                  }}
                  scroll={{ x: 1400 }}
                  locale={{
                    emptyText: '该用户暂无订单记录',
                  }}
                />
              </Card>
            ),
          },
          {
            key: 'coupons',
            label: '优惠券',
            children: <CouponSection userId={user.id} />,
          },
          {
            key: 'tickets',
            label: '水票',
            children: <TicketSection userId={user.id} />,
          },
          {
            key: 'feedback',
            label: '意见反馈',
            children: <FeedbackSection userId={user.id} />,
          },
          {
            key: 'reviews',
            label: '评价记录',
            children: <ReviewSection userId={user.id} />,
          },
          {
            key: 'favorites',
            label: '收藏商品',
            children: <FavoriteSection userId={user.id} />,
          },
        ]}
      />

    </div>
  );
};

export default UserDetailPage;
