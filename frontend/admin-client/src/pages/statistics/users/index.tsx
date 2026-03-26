import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Spin } from 'antd';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { useQuery, gql } from '@apollo/client';
import StatisticsCard from '@/components/StatisticsCard';

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

interface UserStatisticsData {
  userStatistics: {
    totalUsers: number;
    todayNewUsers: number;
    monthNewUsers: number;
    activeUsers: number;
    dailyNewUsers: DailyUserStat[];
    loginStatistics: LoginStatistics;
  };
}

const UserStatisticsPage: React.FC = () => {
  const { data, loading, error } = useQuery<UserStatisticsData>(GET_USER_STATISTICS);
  const [chartData, setChartData] = useState<{ date: string; count: number }[]>([]);
  const [loginChartData, setLoginChartData] = useState<{ date: string; count: number }[]>([]);

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

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 24, marginBottom: 24 }}>用户统计</h1>

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
                stroke="#1890ff"
                strokeWidth={2}
                dot={{ fill: '#1890ff', r: 3 }}
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
    </div>
  );
};

export default UserStatisticsPage;
