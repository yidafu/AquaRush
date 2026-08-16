import React, { useState } from 'react';
import { Alert, Button, Card, Col, DatePicker, Empty, Row, Select, Space, Spin } from 'antd';
import { useQuery, gql } from '@apollo/client';
import { centsToYuan, normalizeCents } from '@aquarush/common';
import {
  Bar,
  BarChart,
  CartesianGrid,
  Line,
  LineChart,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis,
} from 'recharts';
import dayjs, { Dayjs } from 'dayjs';
import StatisticsCard from '@/components/StatisticsCard';

const { RangePicker } = DatePicker;

type Granularity = 'DAY' | 'WEEK' | 'MONTH';

const getRangePickerFormat = (granularity: Granularity): string => {
  switch (granularity) {
    case 'MONTH':
      return 'YYYY-MM';
    default:
      return 'YYYY-MM-DD';
  }
};

// 单页面一次渲染一个 GraphQL 请求(CLAUDE.md 规则 10):按粒度 skip 只启用当前粒度查询,
// 每个查询 = orderStatistics(汇总卡)+ 对应粒度趋势(图表)
const GET_DAILY_REVENUE_STATISTICS = gql`
  query GetDailyRevenueStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      completedOrders
      totalRevenueCents
      averageOrderValueCents
    }
    dailyStatistics(input: $input) {
      date
      orderCount
      orderProductCount
      revenueCents
    }
  }
`;

const GET_WEEKLY_REVENUE_STATISTICS = gql`
  query GetWeeklyRevenueStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      completedOrders
      totalRevenueCents
      averageOrderValueCents
    }
    weeklyStatistics(input: $input) {
      startDate
      orderCount
      orderProductCount
      revenueCents
    }
  }
`;

const GET_MONTHLY_REVENUE_STATISTICS = gql`
  query GetMonthlyRevenueStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      completedOrders
      totalRevenueCents
      averageOrderValueCents
    }
    monthlyStatistics(input: $input) {
      year
      month
      orderCount
      orderProductCount
      revenueCents
    }
  }
`;

interface OrderStats {
  totalOrders: number;
  completedOrders: number;
  totalRevenueCents: number;
  averageOrderValueCents: number;
}

interface DailyStat {
  date: string;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
}

interface WeeklyStat {
  startDate: string;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
}

interface MonthlyStat {
  year: number;
  month: number;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
}

interface DailyRevenueStatisticsData {
  orderStatistics: OrderStats;
  dailyStatistics: DailyStat[];
}

interface WeeklyRevenueStatisticsData {
  orderStatistics: OrderStats;
  weeklyStatistics: WeeklyStat[];
}

interface MonthlyRevenueStatisticsData {
  orderStatistics: OrderStats;
  monthlyStatistics: MonthlyStat[];
}

interface TrendPoint {
  date: string;
  orders: number;
  products: number;
  revenueCents: number;
  revenue: number;
}

/**
 * 后端 Long 字段在网络上以字符串序列化(见 @aquarush/common normalizeCents 注释),统一归一为数字
 */
const toSafeNumber = (value: number | string | null | undefined): number => {
  const n = normalizeCents(value);
  return n ?? 0;
};

const RevenueStatistics: React.FC = () => {
  const [granularity, setGranularity] = useState<Granularity>('DAY');
  const [dateRange, setDateRange] = useState<[Dayjs, Dayjs]>([
    dayjs().subtract(29, 'day'),
    dayjs(),
  ]);

  const input = {
    startDate: dateRange[0].startOf('day').format('YYYY-MM-DDTHH:mm:ss'),
    endDate: dateRange[1].endOf('day').format('YYYY-MM-DDTHH:mm:ss'),
  };
  const variables = { input };

  const { data: dailyData, loading: dailyLoading, error: dailyError, refetch: dailyRefetch } =
    useQuery<DailyRevenueStatisticsData>(GET_DAILY_REVENUE_STATISTICS, {
      variables,
      skip: granularity !== 'DAY',
    });

  const { data: weeklyData, loading: weeklyLoading, error: weeklyError, refetch: weeklyRefetch } =
    useQuery<WeeklyRevenueStatisticsData>(GET_WEEKLY_REVENUE_STATISTICS, {
      variables,
      skip: granularity !== 'WEEK',
    });

  const { data: monthlyData, loading: monthlyLoading, error: monthlyError, refetch: monthlyRefetch } =
    useQuery<MonthlyRevenueStatisticsData>(GET_MONTHLY_REVENUE_STATISTICS, {
      variables,
      skip: granularity !== 'MONTH',
    });

  const loading = granularity === 'DAY' ? dailyLoading : granularity === 'WEEK' ? weeklyLoading : monthlyLoading;
  const error = granularity === 'DAY' ? dailyError : granularity === 'WEEK' ? weeklyError : monthlyError;
  const refetch = granularity === 'DAY' ? dailyRefetch : granularity === 'WEEK' ? weeklyRefetch : monthlyRefetch;

  const handleGranularityChange = (value: Granularity) => {
    setGranularity(value);
    const now = dayjs();
    if (value === 'MONTH') {
      setDateRange([now.subtract(1, 'year'), now]);
    } else if (value === 'WEEK') {
      setDateRange([now.subtract(12, 'week'), now]);
    } else {
      setDateRange([now.subtract(29, 'day'), now]);
    }
  };

  if (error) {
    return (
      <div>
        <Alert
          style={{ marginTop: 24 }}
          type="error"
          showIcon
          message="数据加载失败"
          description={error.message}
          action={
            <Button size="small" onClick={() => refetch()}>
              重试
            </Button>
          }
        />
      </div>
    );
  }

  let orderStats: OrderStats | undefined;
  let chartData: TrendPoint[] = [];

  if (granularity === 'DAY' && dailyData) {
    orderStats = dailyData.orderStatistics;
    chartData = dailyData.dailyStatistics.map((item: DailyStat) => {
      const revenueCents = toSafeNumber(item.revenueCents);
      return {
        date: item.date.slice(5),
        orders: toSafeNumber(item.orderCount),
        products: toSafeNumber(item.orderProductCount),
        revenueCents,
        revenue: centsToYuan(revenueCents),
      };
    });
  } else if (granularity === 'WEEK' && weeklyData) {
    orderStats = weeklyData.orderStatistics;
    chartData = weeklyData.weeklyStatistics.map((item: WeeklyStat) => {
      const revenueCents = toSafeNumber(item.revenueCents);
      return {
        date: item.startDate.slice(5),
        orders: toSafeNumber(item.orderCount),
        products: toSafeNumber(item.orderProductCount),
        revenueCents,
        revenue: centsToYuan(revenueCents),
      };
    });
  } else if (granularity === 'MONTH' && monthlyData) {
    orderStats = monthlyData.orderStatistics;
    chartData = monthlyData.monthlyStatistics.map((item: MonthlyStat) => {
      const revenueCents = toSafeNumber(item.revenueCents);
      return {
        date: `${item.year}-${item.month}`,
        orders: toSafeNumber(item.orderCount),
        products: toSafeNumber(item.orderProductCount),
        revenueCents,
        revenue: centsToYuan(revenueCents),
      };
    });
  }

  // 汇总卡:营收总额 = 已完成订单金额合计(PRD F-STA-002 域级口径);
  // 日均营收 = 营收总额 / 覆盖天数;最大周期营收 = 当前粒度趋势最大值
  const totalRevenueCents = orderStats ? toSafeNumber(orderStats.totalRevenueCents) : 0;
  const daysInRange = Math.max(1, dateRange[1].diff(dateRange[0], 'day') + 1);
  const averageDailyRevenueCents = totalRevenueCents / daysInRange;
  const maxPeriodRevenueCents = chartData.reduce((max, point) => Math.max(max, point.revenueCents), 0);
  const maxPeriodTitle =
    granularity === 'DAY' ? '最大单日营收' : granularity === 'WEEK' ? '最大周营收' : '最大月营收';

  return (
    <div>
      <h1>营收统计</h1>
      <Space style={{ marginTop: 16 }} wrap>
        <span>日期粒度:</span>
        <Select value={granularity} onChange={handleGranularityChange} style={{ width: 120 }}>
          <Select.Option value="DAY">按日</Select.Option>
          <Select.Option value="WEEK">按周</Select.Option>
          <Select.Option value="MONTH">按月</Select.Option>
        </Select>
        <RangePicker
          value={dateRange}
          format={getRangePickerFormat(granularity)}
          disabledDate={(current) => current.isAfter(dayjs(), 'day')}
          onChange={(dates) => {
            if (dates && dates[0] && dates[1]) {
              setDateRange([dates[0], dates[1]]);
            }
          }}
        />
      </Space>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} sm={12} lg={8}>
          <StatisticsCard
            title="营收总额"
            value={centsToYuan(totalRevenueCents)}
            prefix="¥"
            precision={2}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <StatisticsCard
            title="日均营收"
            value={centsToYuan(averageDailyRevenueCents)}
            prefix="¥"
            precision={2}
            loading={loading}
          />
        </Col>
        <Col xs={24} sm={12} lg={8}>
          <StatisticsCard
            title={maxPeriodTitle}
            value={centsToYuan(maxPeriodRevenueCents)}
            prefix="¥"
            precision={2}
            loading={loading}
          />
        </Col>
      </Row>

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={12}>
          <Card title="订单趋势">
            <Spin spinning={loading}>
              {chartData.length === 0 ? (
                <Empty description="所选范围暂无数据" style={{ padding: 40 }} />
              ) : (
                <ResponsiveContainer width="100%" height={320}>
                  <BarChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
                    <Tooltip
                      labelFormatter={(label) => `日期: ${label}`}
                      formatter={(value) => [`${Number(value)} 单`, '订单数']}
                    />
                    <Bar dataKey="orders" name="订单数" fill="#1890ff" radius={[4, 4, 0, 0]} />
                  </BarChart>
                </ResponsiveContainer>
              )}
            </Spin>
          </Card>
        </Col>
        <Col xs={24} lg={12}>
          <Card title="营收趋势">
            <Spin spinning={loading}>
              {chartData.length === 0 ? (
                <Empty description="所选范围暂无数据" style={{ padding: 40 }} />
              ) : (
                <ResponsiveContainer width="100%" height={320}>
                  <LineChart data={chartData} margin={{ top: 5, right: 30, left: 20, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="date" tick={{ fontSize: 12 }} />
                    <YAxis tick={{ fontSize: 12 }} tickFormatter={(v) => `¥${v}`} />
                    <Tooltip
                      labelFormatter={(label) => `日期: ${label}`}
                      formatter={(value) => [`¥${Number(value).toFixed(2)}`, '营收']}
                    />
                    <Line
                      type="monotone"
                      dataKey="revenue"
                      name="营收"
                      stroke="#52c41a"
                      strokeWidth={2}
                      dot={{ fill: '#52c41a', r: 3 }}
                    />
                  </LineChart>
                </ResponsiveContainer>
              )}
            </Spin>
          </Card>
        </Col>
      </Row>

      {/* 退款/净营收维度依赖支付链路(G01),链路落地后补充 —— PRD F-STA-002 */}
      <Alert
        style={{ marginTop: 24 }}
        type="info"
        showIcon
        message="退款/净营收维度依赖支付链路(G01),链路落地后补充"
      />
    </div>
  );
};

export default RevenueStatistics;
