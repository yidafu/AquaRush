import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Select, Spin, DatePicker, Space } from 'antd';
import {
  LineChart,
  Line,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { useQuery, gql } from '@apollo/client';
import StatisticsCard from '@/components/StatisticsCard';
import dayjs, { Dayjs } from 'dayjs';

const { RangePicker } = DatePicker;

type Granularity = 'DAY' | 'WEEK' | 'MONTH';

const getRangePickerFormat = (granularity: Granularity): string => {
  switch (granularity) {
    case 'MONTH':
      return 'YYYY-MM';
    case 'WEEK':
      return 'YYYY-MM-DD';
    default:
      return 'YYYY-MM-DD';
  }
};

const GET_DAILY_STATISTICS = gql`
  query GetDailyStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      totalRevenue
      averageOrderValue
    }
    dailyStatistics(input: $input) {
      date
      orderCount
      orderProductCount
      revenue
    }
  }
`;

const GET_WEEKLY_STATISTICS = gql`
  query GetWeeklyStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      totalRevenue
      averageOrderValue
    }
    weeklyStatistics(input: $input) {
      startDate
      orderCount
      orderProductCount
      revenue
    }
  }
`;

const GET_MONTHLY_STATISTICS = gql`
  query GetMonthlyStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      totalRevenue
      averageOrderValue
    }
    monthlyStatistics(input: $input) {
      year
      month
      monthName
      orderCount
      orderProductCount
      revenue
    }
  }
`;

interface OrderStats {
  totalOrders: number;
  totalRevenue: number;
  averageOrderValue: number;
}

interface DailyStat {
  date: string;
  orderCount: number;
  orderProductCount: number;
  revenue: number;
}

interface WeeklyStat {
  startDate: string;
  orderCount: number;
  orderProductCount: number;
  revenue: number;
}

interface MonthlyStat {
  year: number;
  month: number;
  monthName: string;
  orderCount: number;
  orderProductCount: number;
  revenue: number;
}

interface DailyStatisticsData {
  orderStatistics: OrderStats;
  dailyStatistics: DailyStat[];
}

interface WeeklyStatisticsData {
  orderStatistics: OrderStats;
  weeklyStatistics: WeeklyStat[];
}

interface MonthlyStatisticsData {
  orderStatistics: OrderStats;
  monthlyStatistics: MonthlyStat[];
}

const OrderStatisticsPage: React.FC = () => {
  const [granularity, setGranularity] = useState<Granularity>('DAY');
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(30, 'day'),
    dayjs(),
  ]);

  const startDate = dateRange[0].format('YYYY-MM-DDTHH:mm:ss');
  const endDate = dateRange[1].format('YYYY-MM-DDTHH:mm:ss');

  const variables = {
    input: {
      startDate,
      endDate,
    },
  };

  const { data: dailyData, loading: dailyLoading, error: dailyError, refetch: dailyRefetch } = useQuery<DailyStatisticsData>(GET_DAILY_STATISTICS, {
    variables,
    skip: granularity !== 'DAY',
  });

  const { data: weeklyData, loading: weeklyLoading, error: weeklyError, refetch: weeklyRefetch } = useQuery<WeeklyStatisticsData>(GET_WEEKLY_STATISTICS, {
    variables,
    skip: granularity !== 'WEEK',
  });

  const { data: monthlyData, loading: monthlyLoading, error: monthlyError, refetch: monthlyRefetch } = useQuery<MonthlyStatisticsData>(GET_MONTHLY_STATISTICS, {
    variables,
    skip: granularity !== 'MONTH',
  });

  const loading = granularity === 'DAY' ? dailyLoading : granularity === 'WEEK' ? weeklyLoading : monthlyLoading;
  const error = granularity === 'DAY' ? dailyError : granularity === 'WEEK' ? weeklyError : monthlyError;

  useEffect(() => {
    if (granularity === 'DAY') {
      dailyRefetch(variables);
    } else if (granularity === 'WEEK') {
      weeklyRefetch(variables);
    } else {
      monthlyRefetch(variables);
    }
  }, [granularity, dateRange, dailyRefetch, weeklyRefetch, monthlyRefetch]);

  const handleGranularityChange = (value: Granularity) => {
    setGranularity(value);
    const now = dayjs();
    if (value === 'MONTH') {
      setDateRange([now.subtract(1, 'year'), now]);
    } else if (value === 'WEEK') {
      setDateRange([now.subtract(12, 'week'), now]);
    } else {
      setDateRange([now.subtract(30, 'day'), now]);
    }
  };

  if (error) {
    return <div>加载失败: {error.message}</div>;
  }

  let orderStats: OrderStats | undefined;
  let chartData: { date: string; orders: number; revenue: number; products: number }[] = [];

  if (granularity === 'DAY' && dailyData) {
    orderStats = dailyData.orderStatistics;
    chartData = dailyData.dailyStatistics.map((item: DailyStat) => ({
      date: item.date.slice(5),
      orders: item.orderCount,
      revenue: item.revenue ,
      products: item.orderProductCount,
    }));
  } else if (granularity === 'WEEK' && weeklyData) {
    orderStats = weeklyData.orderStatistics;
    chartData = weeklyData.weeklyStatistics.map((item: WeeklyStat) => ({
      date: item.startDate.slice(5),
      orders: item.orderCount,
      revenue: item.revenue ,
      products: item.orderProductCount,
    }));
  } else if (granularity === 'MONTH' && monthlyData) {
    orderStats = monthlyData.orderStatistics;
    chartData = monthlyData.monthlyStatistics.map((item: MonthlyStat) => ({
      date: `${item.year}-${item.month}`,
      orders: item.orderCount,
      revenue: item.revenue ,
      products: item.orderProductCount,
    }));
  }

  const totalRevenue = orderStats?.totalRevenue ? orderStats.totalRevenue  : 0;
  const avgOrderValue = orderStats?.averageOrderValue ? orderStats.averageOrderValue  : 0;

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 24, marginBottom: 24 }}>订单营收统计</h1>

      <Space style={{ marginBottom: 16 }}>
        <span>日期粒度:</span>
        <Select
          value={granularity}
          onChange={handleGranularityChange}
          style={{ width: 120 }}
        >
          <Select.Option value="DAY">按日</Select.Option>
          <Select.Option value="WEEK">按周</Select.Option>
          <Select.Option value="MONTH">按月</Select.Option>
        </Select>
        <RangePicker
          value={dateRange}
          format={getRangePickerFormat(granularity)}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              setDateRange([dates[0], dates[1]]);
            }
          }}
        />
      </Space>

      <Row gutter={[16, 16]}>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="订单总数"
            value={orderStats?.totalOrders ?? 0}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="营收总额"
            value={totalRevenue}
            prefix="¥"
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="平均客单价"
            value={avgOrderValue}
            prefix="¥"
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={6}>
          <StatisticsCard
            title="已完成订单"
            value={orderStats?.totalOrders ?? 0}
            loading={loading}
          />
        </Col>
      </Row>

      <Card title="订单与营收趋势" style={{ marginTop: 24 }}>
        <Spin spinning={loading}>
          <ResponsiveContainer width="100%" height={350}>
            <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
              <CartesianGrid strokeDasharray="3 3" />
              <XAxis dataKey="date" tick={{ fontSize: 12 }} />
              <YAxis yAxisId="left" tick={{ fontSize: 12 }} />
              <YAxis yAxisId="right" orientation="right" tick={{ fontSize: 12 }} tickFormatter={(v) => `¥${v}`} />
              <Tooltip
                labelFormatter={(label) => `日期: ${label}`}
                formatter={(value: number, name: string) => {
                  if (name === 'orders' || name === 'products') return [`${value} 单`, name === 'orders' ? '订单数' : '送水桶数'];
                  return [`¥${value.toFixed(2)}`, '营收'];
                }}
              />
              <Legend />
              <Line yAxisId="left" type="monotone" dataKey="orders" name="订单数" stroke="#1890ff" strokeWidth={2} dot={{ fill: '#1890ff', r: 3 }} />
              <Line yAxisId="left" type="monotone" dataKey="products" name="送水桶数" stroke="#faad14" strokeWidth={2} dot={{ fill: '#faad14', r: 3 }} />
              <Line yAxisId="right" type="monotone" dataKey="revenue" name="营收" stroke="#52c41a" strokeWidth={2} dot={{ fill: '#52c41a', r: 3 }} />
            </LineChart>
          </ResponsiveContainer>
        </Spin>
      </Card>
    </div>
  );
};

export default OrderStatisticsPage;
