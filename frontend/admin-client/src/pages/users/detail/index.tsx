import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import {
  Card,
  Button,
  Spin,
  Empty,
  Tabs,
} from '@aquarush/admin-ui';
import { useQuery } from '@apollo/client';
import {
  USER_DETAIL_PAGE_DATA_QUERY,
} from '../../../graphql/queries/user.graphql';
import type {
  UserVo,
  OrderVo,
  AddressVo,
} from '@aquarush/common';
import AddressListUsingList from '../components/AddressListUsingList';
import CouponSection from './sections/CouponSection';
import TicketSection from './sections/TicketSection';
import FeedbackSection from './sections/FeedbackSection';
import ReviewSection from './sections/ReviewSection';
import FavoriteSection from './sections/FavoriteSection';
import { UserBasicInfoCard, UserStatisticsCards, OrderHistoryTable } from './components';
import type { UserDetailPageData } from './types';
import * as stylex from '@stylexjs/stylex';

const styles = stylex.create({
  stateBox: {
    paddingTop: '24px',
    paddingRight: '24px',
    paddingBottom: '24px',
    paddingLeft: '24px',
    textAlign: 'center',
  },
  page: {
    minHeight: '100vh',
  },
  backBar: {
    marginBottom: '12px',
  },
});

/**
 * 用户详情页(users/:id,spec/prd/07-user.md §F-USR-022):
 * 基本信息卡(含角色/邮箱/所属分组/总消费;无账户余额——用户储值不做,2026-10-09 决策)
 * → 统计卡(全量口径) → Tabs
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

  const user: UserVo | undefined = data?.user;
  const userAddresses: AddressVo[] = data?.userAddresses || [];
  const orders: OrderVo[] = data?.orders.content || [];

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
      <div {...stylex.props(styles.stateBox)}>
        <Spin size="large" />
      </div>
    );
  }

  if (userError || !user) {
    return (
      <div {...stylex.props(styles.stateBox)}>
        <h2>加载失败</h2>
        <p>无法获取用户信息</p>
        <Button type="primary" onClick={() => navigate('/users')}>
          返回用户列表
        </Button>
      </div>
    );
  }

  return (
    <div {...stylex.props(styles.page)}>
      {/* Header with back button */}
      <div {...stylex.props(styles.backBar)}>
        <Button onClick={() => navigate('/users')}>
          ← 返回用户列表
        </Button>
      </div>

      {/* Basic Information Card */}
      <UserBasicInfoCard user={user} lastOrderTime={lastOrderTime} />

      {/* Statistics Cards (全量口径) */}
      <UserStatisticsCards
        totalOrders={totalOrders}
        totalAmountInYuan={totalAmountInYuan}
        completedOrders={completedOrders}
        averageAmountInYuan={averageAmountInYuan}
      />

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
                <OrderHistoryTable
                  orders={orders}
                  loading={userLoading}
                  page={page}
                  pageSize={pageSize}
                  total={data?.orders.totalElements ?? 0}
                  onPageChange={(nextPage, nextPageSize) => {
                    setPage(nextPage);
                    setPageSize(nextPageSize);
                  }}
                  onViewOrder={(orderId) => navigate(`/orders/${orderId}`)}
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
