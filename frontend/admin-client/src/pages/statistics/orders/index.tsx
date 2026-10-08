import React, { useEffect, useState } from 'react';
import { Row } from 'antd';
import { useQuery } from '@apollo/client';
import { centsToYuan } from '@aquarush/common';
import RefreshControls from '@/components/RefreshControls';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';
import dayjs, { Dayjs } from 'dayjs';
import {
  GET_DAILY_STATISTICS,
  GET_WEEKLY_STATISTICS,
  GET_MONTHLY_STATISTICS,
  ORDER_STATUS_MAP,
  type Granularity,
  type DailyStatisticsData,
  type WeeklyStatisticsData,
  type MonthlyStatisticsData,
  type OrderStats,
  type TrendChartPoint,
} from './data';
import {
  StatisticsFilters,
  StatisticsSummaryCards,
  TrendLineChart,
  StatusPieChart,
  ValueDistributionBarChart,
  PeakHourBarChart,
} from './components';

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
  const activeRefetch = granularity === 'DAY' ? dailyRefetch : granularity === 'WEEK' ? weeklyRefetch : monthlyRefetch;

  // 手动刷新 + 定时更新:仅作用于当前生效粒度查询(活跃 refetch 经 ref 保持最新)
  const { intervalMs, setIntervalMs, refreshing, refresh, lastUpdated } = useAutoRefresh({
    onRefresh: () => activeRefetch(),
  });

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
  let chartData: TrendChartPoint[] = [];

  if (granularity === 'DAY' && dailyData) {
    orderStats = dailyData.orderStatistics;
    chartData = dailyData.dailyStatistics.map((item) => ({
      date: item.date.slice(5),
      orders: item.orderCount,
      revenue: centsToYuan(item.revenueCents),
      products: item.orderProductCount,
    }));
  } else if (granularity === 'WEEK' && weeklyData) {
    orderStats = weeklyData.orderStatistics;
    chartData = weeklyData.weeklyStatistics.map((item) => ({
      date: item.startDate.slice(5),
      orders: item.orderCount,
      revenue: centsToYuan(item.revenueCents),
      products: item.orderProductCount,
    }));
  } else if (granularity === 'MONTH' && monthlyData) {
    orderStats = monthlyData.orderStatistics;
    chartData = monthlyData.monthlyStatistics.map((item) => ({
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

  // 状态分布:7 态全量展示(零值状态无扇区但图例显示计数),合计 = 订单总数
  const statusPieData = (orderStats?.statusDistribution || []).map((item) => ({
    name: `${ORDER_STATUS_MAP[item.status]?.text ?? item.status} (${item.count}单)`,
    status: item.status,
    value: item.count,
    color: ORDER_STATUS_MAP[item.status]?.color ?? '#8c8c8c',
  }));

  // 客单价分布:5 档区间全量展示(含 0 值柱),合计 = 订单总数
  const valueDistributionData = (orderStats?.orderValueDistribution || []).map((item) => ({
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
  const peakHourExtra = peakHours.length > 0
    ? `高峰时段: ${peakHours.join('、')} (${maxHourCount} 单)`
    : null;

  return (
    <div style={{ padding: 24 }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h1 style={{ fontSize: 24, margin: 0 }}>订单统计</h1>
        <RefreshControls
          interval={intervalMs}
          onIntervalChange={setIntervalMs}
          onRefresh={refresh}
          refreshing={refreshing}
          lastUpdated={lastUpdated}
        />
      </div>

      <StatisticsFilters
        granularity={granularity}
        dateRange={dateRange}
        onGranularityChange={handleGranularityChange}
        onDateRangeChange={setDateRange}
      />

      <StatisticsSummaryCards
        loading={loading}
        totalOrders={totalOrders}
        totalRevenue={totalRevenue}
        avgOrderValue={avgOrderValue}
        completedOrders={orderStats?.completedOrders ?? 0}
      />

      <TrendLineChart data={chartData} loading={loading} />

      <Row gutter={[16, 16]} style={{ marginTop: 24 }}>
        <StatusPieChart data={statusPieData} totalOrders={totalOrders} loading={loading} />
        <ValueDistributionBarChart data={valueDistributionData} totalOrders={totalOrders} loading={loading} />
        <PeakHourBarChart data={peakHourData} maxHourCount={maxHourCount} peakHourExtra={peakHourExtra} loading={loading} />
      </Row>
    </div>
  );
};

export default OrderStatisticsPage;
