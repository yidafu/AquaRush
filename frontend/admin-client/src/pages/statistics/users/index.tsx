import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Spin, Statistic } from 'antd';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  PieChart,
  Pie,
  Cell,
} from 'recharts';
import { useQuery, gql } from '@apollo/client';
import StatisticsCard from '@/components/StatisticsCard';
import RefreshControls from '@/components/RefreshControls';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';
import { BRAND_PRIMARY } from '../../../config/theme';

const GET_USER_STATISTICS = gql`
  query GetUserStatistics {
    userStatistics {
      totalUsers
      todayNewUsers
      monthNewUsers
      activeUsers
      dailyNewUsers {
        date
        orderCount
      }
      loginStatistics {
        todayLogins
        totalLogins
        dailyLogins {
          date
          loginCount
        }
      }
      orderDistribution {
        orderingUsers
        orderingUserRatio
        repurchaseRate
        tiers {
          label
          userCount
        }
      }
    }
  }
`;

interface DailyUserStat {
  date: string;
  orderCount: number;
}

interface DailyLoginStat {
  date: string;
  loginCount: number;
}

interface LoginStatistics {
  todayLogins: number;
  totalLogins: number;
  dailyLogins: DailyLoginStat[];
}

interface OrderUserTier {
  label: string;
  userCount: number;
}

interface OrderUserDistribution {
  orderingUsers: number;
  orderingUserRatio: number;
  repurchaseRate: number;
  tiers: OrderUserTier[];
}

interface UserStatisticsData {
  userStatistics: {
    totalUsers: number;
    todayNewUsers: number;
    monthNewUsers: number;
    activeUsers: number;
    dailyNewUsers: DailyUserStat[];
    loginStatistics: LoginStatistics;
    orderDistribution: OrderUserDistribution;
  };
}

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
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, margin: 0 }}>用户统计</h1>
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

      <Row gutter={[16, 16]} style={{ marginTop: 16 }}>
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

      <Card title="用户增长趋势（近30天）" style={{ marginTop: 24 }}>
        <Spin spinning={loading}>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip
                labelFormatter={(label) => `日期: ${label}`}
                formatter={(value) => [`${value ?? 0} 人`, '新增用户']}
              />
              <Line
                type="monotone"
                dataKey="count"
                name="新增用户"
                stroke={BRAND_PRIMARY}
                strokeWidth={2}
                dot={{ fill: BRAND_PRIMARY, r: 3 }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Spin>
      </Card>

      <Card title="登录趋势（近30天）" style={{ marginTop: 24 }}>
        <Spin spinning={loading}>
          <ResponsiveContainer width="100%" height={300}>
            <LineChart data={loginChartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip
                labelFormatter={(label) => `日期: ${label}`}
                formatter={(value) => [`${value ?? 0} 人`, '登录用户']}
              />
              <Line
                type="monotone"
                dataKey="count"
                name="登录用户"
                stroke="#52c41a"
                strokeWidth={2}
                dot={{ fill: '#52c41a', r: 3 }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </Spin>
      </Card>

      {/* 下单用户分布(近30天,G35):环形图按累计下单次数分层 + 下单用户占比/复购率 */}
      <Card title="下单用户分布（近30天）" style={{ marginTop: 24 }}>
        <Spin spinning={loading}>
          <Row gutter={[16, 16]} align="middle">
            <Col xs={24} lg={12}>
              <ResponsiveContainer width="100%" height={280}>
                <PieChart>
                  <Pie
                    data={orderTierPieData}
                    dataKey="value"
                    nameKey="name"
                    cx="50%"
                    cy="50%"
                    innerRadius={60}
                    outerRadius={95}
                    paddingAngle={2}
                  >
                    {orderTierPieData.map((entry) => (
                      <Cell key={entry.name} fill={entry.color} />
                    ))}
                  </Pie>
                  <Tooltip
                    formatter={(value) => [`${Number(value ?? 0)} 人`, '用户数']}
                  />
                </PieChart>
              </ResponsiveContainer>
              <div style={{ textAlign: 'center', color: '#999', fontSize: 13 }}>
                按累计下单次数分层（近30天下单用户去重）
              </div>
            </Col>
            <Col xs={24} lg={12}>
              <Row gutter={[16, 16]}>
                <Col xs={24} sm={12}>
                  <Card>
                    <Statistic
                      title="下单用户数"
                      value={orderDistribution?.orderingUsers ?? 0}
                      suffix="人"
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={12}>
                  <Card>
                    <Statistic
                      title="下单用户占比"
                      value={formatRatio(orderDistribution?.orderingUserRatio)}
                      suffix="%"
                      precision={2}
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={12}>
                  <Card>
                    <Statistic
                      title="复购率"
                      value={formatRatio(orderDistribution?.repurchaseRate)}
                      suffix="%"
                      precision={2}
                    />
                  </Card>
                </Col>
                <Col xs={24} sm={12}>
                  <Card>
                    <Statistic
                      title="总用户数"
                      value={stats?.totalUsers ?? 0}
                      suffix="人"
                    />
                  </Card>
                </Col>
              </Row>
            </Col>
          </Row>
        </Spin>
      </Card>
    </div>
  );
};

/**
 * 下单次数分层环形图配色(G35,复用订单状态分布色板风格)
 */
const TIER_COLORS = [BRAND_PRIMARY, '#52c41a', '#fa8c16'];

/**
 * 比率(0..1)转百分比文案(两位小数);null/非法值 → 0
 */
function formatRatio(ratio: number | undefined | null): number {
  const value = typeof ratio === 'number' && Number.isFinite(ratio) ? ratio : 0;
  return Number((value * 100).toFixed(2));
}

export default UserStatisticsPage;
