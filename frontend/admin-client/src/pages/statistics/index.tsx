import React, { useState } from 'react';
import { Alert, Button, Row } from '@/ui';
import { useQuery } from '@apollo/client';
import { centsToYuan } from '@aquarush/common';
import RefreshControls from '@/components/RefreshControls';
import { useAutoRefresh } from '@/hooks/useAutoRefresh';
import dayjs, { Dayjs } from 'dayjs';
import {
  GET_DAILY_REVENUE_STATISTICS,
  GET_WEEKLY_REVENUE_STATISTICS,
  GET_MONTHLY_REVENUE_STATISTICS,
  toSafeNumber,
  type Granularity,
  type DailyRevenueStatisticsData,
  type WeeklyRevenueStatisticsData,
  type MonthlyRevenueStatisticsData,
  type OrderStats,
  type TrendPoint,
} from './data';
import {
  StatisticsFilters,
  RevenueSummaryCards,
  OrderTrendBarChart,
  RevenueTrendLineChart,
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
  errorAlert: {
    marginTop: '24px',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  chartsRow: {
    marginTop: '24px',
  },
});

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

  // 手动刷新 + 定时更新:仅作用于当前生效粒度查询(活跃 refetch 经 ref 保持最新)
  const { intervalMs, setIntervalMs, refreshing, refresh, lastUpdated } = useAutoRefresh({
    onRefresh: () => refetch(),
  });

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
          {...stylex.props(styles.errorAlert)}
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
    chartData = dailyData.dailyStatistics.map((item) => {
      const revenueCents = toSafeNumber(item.revenueCents);
      const refundCents = toSafeNumber(item.refundCents);
      return {
        date: item.date.slice(5),
        orders: toSafeNumber(item.orderCount),
        products: toSafeNumber(item.orderProductCount),
        revenueCents,
        revenue: centsToYuan(revenueCents),
        refundCents,
        refund: centsToYuan(refundCents),
        netRevenue: centsToYuan(revenueCents - refundCents),
      };
    });
  } else if (granularity === 'WEEK' && weeklyData) {
    orderStats = weeklyData.orderStatistics;
    chartData = weeklyData.weeklyStatistics.map((item) => {
      const revenueCents = toSafeNumber(item.revenueCents);
      const refundCents = toSafeNumber(item.refundCents);
      return {
        date: item.startDate.slice(5),
        orders: toSafeNumber(item.orderCount),
        products: toSafeNumber(item.orderProductCount),
        revenueCents,
        revenue: centsToYuan(revenueCents),
        refundCents,
        refund: centsToYuan(refundCents),
        netRevenue: centsToYuan(revenueCents - refundCents),
      };
    });
  } else if (granularity === 'MONTH' && monthlyData) {
    orderStats = monthlyData.orderStatistics;
    chartData = monthlyData.monthlyStatistics.map((item) => {
      const revenueCents = toSafeNumber(item.revenueCents);
      const refundCents = toSafeNumber(item.refundCents);
      return {
        date: `${item.year}-${item.month}`,
        orders: toSafeNumber(item.orderCount),
        products: toSafeNumber(item.orderProductCount),
        revenueCents,
        revenue: centsToYuan(revenueCents),
        refundCents,
        refund: centsToYuan(refundCents),
        netRevenue: centsToYuan(revenueCents - refundCents),
      };
    });
  }

  // 汇总卡:营收总额 = 已完成订单金额合计(PRD F-STA-002 域级口径);
  // 日均营收 = 营收总额 / 覆盖天数;最大周期营收 = 当前粒度趋势最大值
  const totalRevenueCents = orderStats ? toSafeNumber(orderStats.totalRevenueCents) : 0;
  const totalRefundCents = orderStats ? toSafeNumber(orderStats.refundAmountCents) : 0;
  const netRevenueCents = orderStats ? toSafeNumber(orderStats.netRevenueCents) : 0;
  const daysInRange = Math.max(1, dateRange[1].diff(dateRange[0], 'day') + 1);
  const averageDailyRevenueCents = totalRevenueCents / daysInRange;
  const maxPeriodRevenueCents = chartData.reduce((max, point) => Math.max(max, point.revenueCents), 0);
  const maxPeriodTitle =
    granularity === 'DAY' ? '最大单日营收' : granularity === 'WEEK' ? '最大周营收' : '最大月营收';
  // 退款率 = 退款金额 / 营收总额;营收为 0 时显示「—」(避免除零)
  const refundRate =
    totalRevenueCents > 0 ? ((totalRefundCents / totalRevenueCents) * 100).toFixed(1) : null;

  return (
    <div>
      <div {...stylex.props(styles.header)}>
        <h1 {...stylex.props(styles.pageTitle)}>营收统计</h1>
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

      <RevenueSummaryCards
        loading={loading}
        totalRevenueYuan={centsToYuan(totalRevenueCents)}
        averageDailyRevenueYuan={centsToYuan(averageDailyRevenueCents)}
        maxPeriodRevenueYuan={centsToYuan(maxPeriodRevenueCents)}
        maxPeriodTitle={maxPeriodTitle}
        totalRefundYuan={centsToYuan(totalRefundCents)}
        netRevenueYuan={centsToYuan(netRevenueCents)}
        refundRate={refundRate}
      />

      <Row gutter={[16, 16]} {...stylex.props(styles.chartsRow)}>
        <OrderTrendBarChart
          data={chartData.map((point) => ({ date: point.date, orders: point.orders }))}
          loading={loading}
        />
        <RevenueTrendLineChart
          data={chartData.map((point) => ({
            date: point.date,
            revenue: point.revenue,
            netRevenue: point.netRevenue,
            refund: point.refund,
          }))}
          loading={loading}
        />
      </Row>
    </div>
  );
};

export default RevenueStatistics;
