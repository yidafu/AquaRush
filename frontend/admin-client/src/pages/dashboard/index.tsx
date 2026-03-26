import React from 'react';
import { Card, Row, Col, Statistic, Spin } from 'antd';
import { ShoppingCartOutlined, DollarOutlined } from '@ant-design/icons';
import { useQuery, gql } from '@apollo/client';
import dayjs from 'dayjs';
import DailyRevenueChart from './components/DailyRevenueChart';
import DailyOrderChart from './components/DailyOrderChart';

const GET_DAILY_STATISTICS = gql`
  query GetDailyStatistics($input: DateRangeInput!) {
    dailyStatistics(input: $input) {
      date
      orderCount
      orderProductCount
      revenue
    }
  }
`;

const Dashboard: React.FC = () => {
  const endDate = dayjs().endOf('day');
  const startDate = dayjs().subtract(30, 'day').startOf('day');

  const { data, loading } = useQuery(GET_DAILY_STATISTICS, {
    variables: {
      input: {
        startDate: startDate.format('YYYY-MM-DDTHH:mm:ss'),
        endDate: endDate.format('YYYY-MM-DDTHH:mm:ss'),
      },
    },
  });

  const dailyData = data?.dailyStatistics || [];

  const todayOrders = dailyData[dailyData.length - 1]?.orderCount || 0;
  const todayRevenue = (dailyData[dailyData.length - 1]?.revenue || 0);

  const totalRevenue = dailyData.reduce((sum: number, item: { revenue?: number }) => {
    return sum + (item.revenue || 0);
  }, 0);

  return (
    <div>
      <h1 className="text-3xl font-bold text-blue-500">仪表盘</h1>
      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={8}>
          <Card>
            <Statistic
              title="今日订单"
              value={todayOrders}
              prefix={<ShoppingCartOutlined />}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="今日营收"
              value={todayRevenue}
              prefix={<DollarOutlined />}
              suffix="元"
              precision={2}
            />
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="本月累计营收"
              value={totalRevenue}
              prefix={<DollarOutlined />}
              suffix="元"
              precision={2}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 24 }}>
        <Col span={24}>
          <Card title="最近30天营收趋势">
            {loading ? (
              <div style={{ textAlign: 'center', padding: 50 }}>
                <Spin />
              </div>
            ) : (
              <DailyRevenueChart data={dailyData} />
            )}
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginTop: 16 }}>
        <Col span={24}>
          <Card title="最近30天订单数">
            {loading ? (
              <div style={{ textAlign: 'center', padding: 50 }}>
                <Spin />
              </div>
            ) : (
              <DailyOrderChart data={dailyData} />
            )}
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default Dashboard;
