import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Select, Spin, DatePicker, Space, Empty } from 'antd';
import {
  LineChart,
  Line,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import { useQuery, gql } from '@apollo/client';
import { centsToYuan } from '@aquarush/common';
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
      completedOrders
      totalRevenueCents
      averageOrderValueCents
      statusDistribution {
        status
        count
      }
      orderValueDistribution {
        fromCents
        toCents
        label
        count
      }
      peakHourDistribution {
        hour
        count
      }
    }
    dailyStatistics(input: $input) {
      date
      orderCount
      orderProductCount
      revenueCents
    }
  }
`;

const GET_WEEKLY_STATISTICS = gql`
  query GetWeeklyStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      completedOrders
      totalRevenueCents
      averageOrderValueCents
      statusDistribution {
        status
        count
      }
      orderValueDistribution {
        fromCents
        toCents
        label
        count
      }
      peakHourDistribution {
        hour
        count
      }
    }
    weeklyStatistics(input: $input) {
      startDate
      orderCount
      orderProductCount
      revenueCents
    }
  }
`;

const GET_MONTHLY_STATISTICS = gql`
  query GetMonthlyStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      completedOrders
      totalRevenueCents
      averageOrderValueCents
      statusDistribution {
        status
        count
      }
      orderValueDistribution {
        fromCents
        toCents
        label
        count
      }
      peakHourDistribution {
        hour
        count
      }
    }
    monthlyStatistics(input: $input) {
      year
      month
      monthName
      orderCount
      orderProductCount
      revenueCents
    }
  }
`;

/** 订单状态中文名与图表色（G37 状态分布，7 态，与 OrderStatus 枚举一致） */
const ORDER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: '待支付', color: '#8c8c8c' },
  PENDING_DISPATCH: { text: '待派单', color: '#13c2c2' },
  PENDING_DELIVERY: { text: '待配送', color: '#1677ff' },
  DELIVERING: { text: '配送中', color: '#fa8c16' },
  COMPLETED: { text: '已完成', color: '#52c41a' },
  CANCELLED: { text: '已取消', color: '#f5222d' },
  REFUNDED: { text: '已退款', color: '#722ed1' },
};

interface OrderStatusStat {
  status: string;
  count: number;
}

interface OrderValueBucketStat {
  fromCents: number | null;
  toCents: number | null;
  label: string;
  count: number;
}

interface PeakHourStat {
  hour: number;
  count: number;
}

interface OrderStats {
  totalOrders: number;
  completedOrders: number;
  totalRevenueCents: number;
  averageOrderValueCents: number;
  statusDistribution: OrderStatusStat[];
  orderValueDistribution: OrderValueBucketStat[];
  peakHourDistribution: PeakHourStat[];
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
  monthName: string;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
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
      revenue: centsToYuan(item.revenueCents),
      products: item.orderProductCount,
    }));
  } else if (granularity === 'WEEK' && weeklyData) {
    orderStats = weeklyData.orderStatistics;
    chartData = weeklyData.weeklyStatistics.map((item: WeeklyStat) => ({
      date: item.startDate.slice(5),
      orders: item.orderCount,
      revenue: centsToYuan(item.revenueCents),
      products: item.orderProductCount,
    }));
  } else if (granularity === 'MONTH' && monthlyData) {
    orderStats = monthlyData.orderStatistics;
    chartData = monthlyData.monthlyStatistics.map((item: MonthlyStat) => ({
      date: `${item.year}-${item.month}`,
      orders: item.orderCount,
      revenue: centsToYuan(item.revenueCents),
      products: item.orderProductCount,
    }));
  }

  const totalRevenue = orderStats?.totalRevenueCents ? centsToYuan(orderStats.totalRevenueCents) : 0;
  const avgOrderValue = orderStats?.averageOrderValueCents ? centsToYuan(orderStats.averageOrderValueCents) : 0;

  // ===== G37 三分析维度数据 =====
  const totalOrders = orderStats?.totalOrders ?? 0;
  const hasOrders = totalOrders > 0;

  // 状态分布:过滤零值扇区,合计 = 订单总数
  const statusPieData = (orderStats?.statusDistribution || [])
    .filter((item) => item.count > 0)
    .map((item) => ({
      name: ORDER_STATUS_MAP[item.status]?.text ?? item.status,
      status: item.status,
      value: item.count,
      color: ORDER_STATUS_MAP[item.status]?.color ?? '#8c8c8c',
    }));

  // 客单价分布:过滤零值区间(空态时无数据点),合计 = 订单总数
  const valueDistributionData = (orderStats?.orderValueDistribution || [])
    .filter((item) => item.count > 0)
    .map((item) => ({
      label: item.label,
      count: item.count,
    }));

  // 高峰时段:24 小时柱状,标注高峰小时
  const peakHourData = (orderStats?.peakHourDistribution || []).map((item) => ({
    hour: `${item.hour}时`,
    count: item.count,
  }));
  const maxHourCount = peakHourData.reduce((max, item) => Math.max(max, item.count), 0);
  const peakHours = maxHourCount > 0
    ? peakHourData.filter((item) => item.count === maxHourCount).map((item) => item.hour)
    : [];
  const peakHourExtra = hasOrders && peakHours.length > 0
    ? `高峰时段: ${peakHours.join('、')} (${maxHourCount} 单)`
    : null;

  return (
    <div style={{ padding: 24 }}>
      <h1 style={{ fontSize: 24, marginBottom: 24 }}>订单统计</h1>

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
            value={totalOrders}
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
            value={orderStats?.completedOrders ?? 0}
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
                formatter={(value, name) => {
                  const numValue = typeof value === 'number' ? value : Number(value ?? 0);
                  if (name === 'orders' || name === 'products') {
                    return [`${numValue} 单`, name === 'orders' ? '订单数' : '送水桶数'];
                  }
                  return [`¥${numValue.toFixed(2)}`, '营收'];
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

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <Col xs={24} lg={8}>
          <Card title="订单状态分布" style={{ height: '100%' }}>
            <Spin spinning={loading}>
              {hasOrders ? (
                <>
                  <ResponsiveContainer width="100%" height={280}>
                    <PieChart>
                      <Pie
                        data={statusPieData}
                        dataKey="value"
                        nameKey="name"
                        cx="50%"
                        cy="50%"
                        innerRadius={60}
                        outerRadius={95}
                        paddingAngle={2}
                      >
                        {statusPieData.map((entry) => (
                          <Cell key={entry.status} fill={entry.color} />
                        ))}
                      </Pie>
                      <Tooltip
                        formatter={(value, name) => {
                          const numValue = typeof value === 'number' ? value : Number(value ?? 0);
                          const percent = totalOrders > 0 ? ((numValue / totalOrders) * 100).toFixed(1) : '0.0';
                          return [`${numValue} 单 (${percent}%)`, String(name)];
                        }}
                      />
                      <Legend />
                    </PieChart>
                  </ResponsiveContainer>
                  <div style={{ textAlign: 'center', color: '#999', fontSize: 13 }}>
                    合计: {totalOrders} 单
                  </div>
                </>
              ) : (
                <Empty description="所选范围暂无数据" style={{ paddingTop: 80 }} />
              )}
            </Spin>
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card title="客单价分布" style={{ height: '100%' }}>
            <Spin spinning={loading}>
              {hasOrders ? (
                <>
                  <ResponsiveContainer width="100%" height={280}>
                    <BarChart data={valueDistributionData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                      <CartesianGrid strokeDasharray="3 3" />
                      <XAxis dataKey="label" tick={{ fontSize: 11 }} />
                      <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
                      <Tooltip
                        formatter={(value) => {
                          const numValue = typeof value === 'number' ? value : Number(value ?? 0);
                          return [`${numValue} 单`, '订单数'];
                        }}
                      />
                      <Bar dataKey="count" name="订单数" fill="#1890ff" radius={[4, 4, 0, 0]} />
                    </BarChart>
                  </ResponsiveContainer>
                  <div style={{ textAlign: 'center', color: '#999', fontSize: 13 }}>
                    区间合计 = 订单总数 ({totalOrders} 单)
                  </div>
                </>
              ) : (
                <Empty description="所选范围暂无数据" style={{ paddingTop: 80 }} />
              )}
            </Spin>
          </Card>
        </Col>

        <Col xs={24} lg={8}>
          <Card title="高峰时段分析" extra={peakHourExtra} style={{ height: '100%' }}>
            <Spin spinning={loading}>
              {hasOrders ? (
                <ResponsiveContainer width="100%" height={280}>
                  <BarChart data={peakHourData} margin={{ top: 5, right: 20, left: 0, bottom: 5 }}>
                    <CartesianGrid strokeDasharray="3 3" />
                    <XAxis dataKey="hour" tick={{ fontSize: 10 }} interval={1} />
                    <YAxis tick={{ fontSize: 12 }} allowDecimals={false} />
                    <Tooltip
                      formatter={(value) => {
                        const numValue = typeof value === 'number' ? value : Number(value ?? 0);
                        return [`${numValue} 单`, '订单数'];
                      }}
                    />
                    <Bar dataKey="count" name="订单数">
                      {peakHourData.map((entry) => (
                        <Cell
                          key={entry.hour}
                          fill={entry.count === maxHourCount && maxHourCount > 0 ? '#faad14' : '#1677ff'}
                        />
                      ))}
                    </Bar>
                  </BarChart>
                </ResponsiveContainer>
              ) : (
                <Empty description="所选范围暂无数据" style={{ paddingTop: 80 }} />
              )}
            </Spin>
          </Card>
        </Col>
      </Row>
    </div>
  );
};

export default OrderStatisticsPage;
