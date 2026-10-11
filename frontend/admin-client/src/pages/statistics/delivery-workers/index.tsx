import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Table } from '@/ui';
import {
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from 'recharts';
import { useQuery, gql } from '@apollo/client';
import { displayCents } from '@aquarush/common';
import StatisticsCard from '@/components/StatisticsCard';
import ChartCard from '@/components/ChartCard';
import RefreshControls from '@/components/RefreshControls';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';
import { BRAND_PRIMARY } from '../../../config/theme';
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
  metricsRow: {
    marginTop: '16px',
  },
  chartsRow: {
    marginTop: '24px',
  },
});

const GET_DELIVERY_WORKER_STATISTICS = gql`
  query GetDeliveryWorkerStatistics {
    deliveryWorkerStatistics {
      totalWorkers
      todayActiveWorkers
      deliveringOrders
      todayCompletedOrders
      averageOrdersPerWorker
      averageFulfillmentMinutes
    }
    deliveryWorkerRanking(limit: 10) {
      workerId
      name
      todayCompletedOrders
      rating
      totalEarningsCents
    }
  }
`;

interface RankingItem {
  workerId: string;
  name: string;
  todayCompletedOrders: number;
  rating: number;
  totalEarningsCents: number;
}

interface DeliveryWorkerStatisticsData {
  deliveryWorkerStatistics: {
    totalWorkers: number;
    todayActiveWorkers: number;
    deliveringOrders: number;
    todayCompletedOrders: number;
    averageOrdersPerWorker: number | null;
    averageFulfillmentMinutes: number | null;
  };
  deliveryWorkerRanking: RankingItem[];
}

const DeliveryWorkerStatisticsPage: React.FC = () => {
  const { data, loading, error, refetch } = useQuery<DeliveryWorkerStatisticsData>(GET_DELIVERY_WORKER_STATISTICS);
  const [chartData, setChartData] = useState<{ name: string; orders: number }[]>([]);

  // 手动刷新 + 定时更新
  const { intervalMs, setIntervalMs, refreshing, refresh, lastUpdated } = useAutoRefresh({
    onRefresh: () => refetch(),
  });

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
      dataIndex: 'totalEarningsCents',
      key: 'totalEarningsCents',
      render: (earnings: number) => displayCents(earnings),
    },
  ];

  return (
    <div>
      <div {...stylex.props(styles.header)}>
        <h1 {...stylex.props(styles.pageTitle)}>送水员统计</h1>
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

      {/* 履约效率指标卡(G36):人均单量 = 今日完成订单总数/今日在岗送水员数;平均履约时长 = Σ(completedAt − 派单时间)/完成订单数 */}
      <Row gutter={[16, 16]} {...stylex.props(styles.metricsRow)}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="人均单量"
            value={stats?.averageOrdersPerWorker != null ? Number(stats.averageOrdersPerWorker.toFixed(1)) : '—'}
            suffix={stats?.averageOrdersPerWorker != null ? '单/人' : undefined}
            precision={stats?.averageOrdersPerWorker != null ? 1 : undefined}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="平均履约时长"
            value={stats?.averageFulfillmentMinutes != null ? Number(stats.averageFulfillmentMinutes.toFixed(1)) : '—'}
            suffix={stats?.averageFulfillmentMinutes != null ? '分钟' : undefined}
            precision={stats?.averageFulfillmentMinutes != null ? 1 : undefined}
            loading={loading}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} {...stylex.props(styles.chartsRow)}>
        <Col xs={24} lg={12}>
          <ChartCard title="送水员工作量排行" loading={loading} height={300}>
            <BarChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="name" tick={{ fontSize: 12 }} />
              <YAxis tick={{ fontSize: 12 }} />
              <Tooltip
                labelFormatter={(label) => `送水员: ${label}`}
                formatter={(value) => [`${Number(value)} 单`, '今日完成订单']}
              />
              <Bar dataKey="orders" name="今日完成订单" fill={BRAND_PRIMARY} radius={[4, 4, 0, 0]} />
            </BarChart>
          </ChartCard>
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
