import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Table, Spin } from 'antd';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
} from 'recharts';
import { useQuery, gql } from '@apollo/client';
import StatisticsCard from '@/components/StatisticsCard';

const GET_DELIVERY_WORKER_STATISTICS = gql`
  query GetDeliveryWorkerStatistics {
    deliveryWorkerStatistics {
      totalWorkers
      todayActiveWorkers
      deliveringOrders
      todayCompletedOrders
    }
    deliveryWorkerRanking(limit: 10) {
      workerId
      name
      todayCompletedOrders
      rating
      totalEarnings
    }
  }
`;

interface RankingItem {
  workerId: string;
  name: string;
  todayCompletedOrders: number;
  rating: number;
  totalEarnings: number;
}

interface DeliveryWorkerStatisticsData {
  deliveryWorkerStatistics: {
    totalWorkers: number;
    todayActiveWorkers: number;
    deliveringOrders: number;
    todayCompletedOrders: number;
  };
  deliveryWorkerRanking: RankingItem[];
}

const DeliveryWorkerStatisticsPage: React.FC = () => {
  const { data, loading, error } = useQuery<DeliveryWorkerStatisticsData>(GET_DELIVERY_WORKER_STATISTICS);
  const [chartData, setChartData] = useState<{ name: string; orders: number }[]>([]);

  useEffect(() => {
    if (data?.deliveryWorkerRanking) {
      const formatted = data.deliveryWorkerRanking.slice(0, 10).map((item) => ({
        name: item.name,
        orders: item.todayCompletedOrders,
      }));
      setChartData(formatted);
    }
  }, [data]);

  if (error) {
    return <div>加载失败: {error.message}</div>;
  }

  const stats = data?.deliveryWorkerStatistics;
  const ranking = data?.deliveryWorkerRanking || [];

  const columns = [
    {
      title: '排名',
      dataIndex: 'index',
      key: 'index',
      width: 60,
      render: (_: unknown, __: unknown, index: number) => index + 1,
    },
    {
      title: '姓名',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '今日完成订单',
      dataIndex: 'todayCompletedOrders',
      key: 'todayCompletedOrders',
    },
    {
      title: '好评率',
      dataIndex: 'rating',
      key: 'rating',
      render: (rating: number) => `${(rating * 100).toFixed(1)}%`,
    },
    {
      title: '累计收入',
      dataIndex: 'totalEarnings',
      key: 'totalEarnings',
      render: (earnings: number) => `¥${(earnings / 100).toFixed(2)}`,
    },
  ];

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 24, marginBottom: 24 }}>送水员统计</h1>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="送水员总数"
            value={stats?.totalWorkers ?? 0}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="今日在岗"
            value={stats?.todayActiveWorkers ?? 0}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="配送中订单"
            value={stats?.deliveringOrders ?? 0}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="今日完成"
            value={stats?.todayCompletedOrders ?? 0}
            loading={loading}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="送水员工作量排行">
            <Spin spinning={loading}>
              <ResponsiveContainer width="100%" height={300}>
                <BarChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                  <CartesianGrid strokeDasharray="3 3" />
                  <XAxis dataKey="name" tick={{ fontSize: 12 }} />
                  <YAxis tick={{ fontSize: 12 }} />
                  <Tooltip
                    labelFormatter={(label) => `送水员: ${label}`}
                    formatter={(value: number) => [`${value} 单`, '今日完成订单']}
                  />
                  <Bar dataKey="orders" name="今日完成订单" fill="#1890ff" radius={[4, 4, 0, 0]} />
                </BarChart>
              </ResponsiveContainer>
            </Spin>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="送水员排行榜">
            <Table
              dataSource={ranking.map((item, index) => ({ ...item, key: item.workerId || index }))}
              columns={columns}
              pagination={false}
              loading={loading}
              size="small"
            />
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default DeliveryWorkerStatisticsPage;
