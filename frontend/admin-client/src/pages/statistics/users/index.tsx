import React, { useEffect, useState } from 'react';
import { Row, Col } from '@/ui';
import { useQuery } from '@apollo/client';
import StatisticsCard from '@/components/StatisticsCard';
import RefreshControls from '@/components/RefreshControls';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';
import {
  GET_USER_STATISTICS,
  formatRatio,
  TIER_COLORS,
  type UserStatisticsData,
} from './data';
import {
  UserGrowthChart,
  LoginTrendChart,
  OrderUserDistributionCard,
} from './components';
import * as stylex from '@stylexjs/stylex';
import { tokens } from '@aquarush/common/theme/tokens.stylex';

// .page-title(index.css 自研类)内联进组件(原 margin: 0 拆 longhand)
const styles = stylex.create({
  pageTitle: {
    marginTop: '0px',
    marginRight: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    fontSize: tokens.fsPage,
    lineHeight: tokens.lhPage,
    fontWeight: 600,
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: '24px',
  },
  loginRow: {
    marginTop: '16px',
  },
});

const UserStatisticsPage: React.FC = () => {
  const { data, loading, error, refetch } = useQuery<UserStatisticsData>(GET_USER_STATISTICS);
  const [chartData, setChartData] = useState<{ date: string; count: number }[]>([]);
  const [loginChartData, setLoginChartData] = useState<{ date: string; count: number }[]>([]);

  // 手动刷新 + 定时更新
  const { intervalMs, setIntervalMs, refreshing, refresh, lastUpdated } = useAutoRefresh({
    onRefresh: () => refetch(),
  });

  useEffect(() => {
    if (data?.userStatistics?.dailyNewUsers) {
      const formatted = data.userStatistics.dailyNewUsers.map((item) => ({
        date: item.date.slice(5),
        count: item.orderCount,
      }));
      setChartData(formatted);
    }
  }, [data]);

  useEffect(() => {
    if (data?.userStatistics?.loginStatistics?.dailyLogins) {
      const formatted = data.userStatistics.loginStatistics.dailyLogins.map((item) => ({
        date: item.date.slice(5),
        count: item.loginCount,
      }));
      setLoginChartData(formatted);
    }
  }, [data]);

  if (error) {
    return <div>加载失败: {error.message}</div>;
  }

  const stats = data?.userStatistics;
  const loginStats = stats?.loginStatistics;
  const orderDistribution = stats?.orderDistribution;

  // 下单次数分层环形图数据(G35)
  const orderTierPieData = (orderDistribution?.tiers || []).map((tier, index) => ({
    name: tier.label,
    value: tier.userCount,
    color: TIER_COLORS[index % TIER_COLORS.length],
  }));

  return (
    <div>
      <div {...stylex.props(styles.header)}>
        <h1 {...stylex.props(styles.pageTitle)}>用户统计</h1>
        <RefreshControls
          interval={intervalMs}
          onIntervalChange={setIntervalMs}
          onRefresh={refresh}
          refreshing={refreshing}
          lastUpdated={lastUpdated}
        />
      </div>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="总用户数"
            value={stats?.totalUsers ?? 0}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="今日新增"
            value={stats?.todayNewUsers ?? 0}
            loading={loading}
            trend={stats?.todayNewUsers && stats.todayNewUsers > 0 ? 'up' : 'neutral'}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="本月新增"
            value={stats?.monthNewUsers ?? 0}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="活跃用户"
            value={stats?.activeUsers ?? 0}
            suffix="人"
            loading={loading}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} {...stylex.props(styles.loginRow)}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="今日登录"
            value={loginStats?.todayLogins ?? 0}
            suffix="人"
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="累计登录"
            value={loginStats?.totalLogins ?? 0}
            suffix="人"
            loading={loading}
          />
        </Col>
      </Row>

      <UserGrowthChart data={chartData} loading={loading} />
      <LoginTrendChart data={loginChartData} loading={loading} />

      {/* 下单用户分布(近30天,G35) */}
      <OrderUserDistributionCard
        tiers={orderTierPieData}
        orderingUsers={orderDistribution?.orderingUsers ?? 0}
        orderingUserRatio={formatRatio(orderDistribution?.orderingUserRatio)}
        repurchaseRate={formatRatio(orderDistribution?.repurchaseRate)}
        totalUsers={stats?.totalUsers ?? 0}
        loading={loading}
      />
    </div>
  );
};

export default UserStatisticsPage;
