import { gql } from '@apollo/client';
import { normalizeCents } from '@aquarush/common';

/** 日期粒度(营收统计页) */
export type Granularity = 'DAY' | 'WEEK' | 'MONTH';

export const getRangePickerFormat = (granularity: Granularity): string => {
  switch (granularity) {
    case 'MONTH':
      return 'YYYY-MM';
    default:
      return 'YYYY-MM-DD';
  }
};

// 单页面一次渲染一个 GraphQL 请求(AGENTS.md 规则 7):按粒度 skip 只启用当前粒度查询,
// 每个查询 = orderStatistics(汇总卡,含退款/净营收)+ 对应粒度趋势(图表,含退款,PRD F-STA-002)
export const GET_DAILY_REVENUE_STATISTICS = gql`
  query GetDailyRevenueStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      completedOrders
      totalRevenueCents
      averageOrderValueCents
      refundAmountCents
      netRevenueCents
    }
    dailyStatistics(input: $input) {
      date
      orderCount
      orderProductCount
      revenueCents
      refundCents
    }
  }
`;

export const GET_WEEKLY_REVENUE_STATISTICS = gql`
  query GetWeeklyRevenueStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      completedOrders
      totalRevenueCents
      averageOrderValueCents
      refundAmountCents
      netRevenueCents
    }
    weeklyStatistics(input: $input) {
      startDate
      orderCount
      orderProductCount
      revenueCents
      refundCents
    }
  }
`;

export const GET_MONTHLY_REVENUE_STATISTICS = gql`
  query GetMonthlyRevenueStatistics($input: DateRangeInput!) {
    orderStatistics(input: $input) {
      totalOrders
      completedOrders
      totalRevenueCents
      averageOrderValueCents
      refundAmountCents
      netRevenueCents
    }
    monthlyStatistics(input: $input) {
      year
      month
      orderCount
      orderProductCount
      revenueCents
      refundCents
    }
  }
`;

export interface OrderStats {
  totalOrders: number;
  completedOrders: number;
  totalRevenueCents: number;
  averageOrderValueCents: number;
  refundAmountCents: number;
  netRevenueCents: number;
}

export interface DailyStat {
  date: string;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
  refundCents: number;
}

export interface WeeklyStat {
  startDate: string;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
  refundCents: number;
}

export interface MonthlyStat {
  year: number;
  month: number;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
  refundCents: number;
}

export interface DailyRevenueStatisticsData {
  orderStatistics: OrderStats;
  dailyStatistics: DailyStat[];
}

export interface WeeklyRevenueStatisticsData {
  orderStatistics: OrderStats;
  weeklyStatistics: WeeklyStat[];
}

export interface MonthlyRevenueStatisticsData {
  orderStatistics: OrderStats;
  monthlyStatistics: MonthlyStat[];
}

export interface TrendPoint {
  date: string;
  orders: number;
  products: number;
  revenueCents: number;
  revenue: number;
  refundCents: number;
  refund: number;
  netRevenue: number;
}

/**
 * 后端 Long 字段在网络上以字符串序列化(见 @aquarush/common normalizeCents 注释),统一归一为数字
 */
export const toSafeNumber = (value: number | string | null | undefined): number => {
  const n = normalizeCents(value);
  return n ?? 0;
};
