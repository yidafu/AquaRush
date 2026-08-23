import { gql } from '@apollo/client';
import { ORDER_STATUS_TEXT } from '@aquarush/common';
import { BRAND_PRIMARY, TEXT_SECONDARY } from '../../../config/theme';

/** 日期粒度(订单统计页) */
export type Granularity = 'DAY' | 'WEEK' | 'MONTH';

export const getRangePickerFormat = (granularity: Granularity): string => {
  switch (granularity) {
    case 'MONTH':
      return 'YYYY-MM';
    case 'WEEK':
      return 'YYYY-MM-DD';
    default:
      return 'YYYY-MM-DD';
  }
};

export const GET_DAILY_STATISTICS = gql`
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

export const GET_WEEKLY_STATISTICS = gql`
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

export const GET_MONTHLY_STATISTICS = gql`
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

/**
 * 订单状态展示与图表色(G37 状态分布,7 态,与 OrderStatus 枚举一致)。
 * 文本统一来自 common ORDER_STATUS_TEXT;色值为图表色板(hex,按 docs/ui-spec.md §2.2,
 * 仅供图表渲染,Tag 颜色走 constants/status.ts toneToAntDColor)。
 */
export const ORDER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: ORDER_STATUS_TEXT.PENDING_PAYMENT, color: '#FA8C16' },
  PENDING_DISPATCH: { text: ORDER_STATUS_TEXT.PENDING_DISPATCH, color: BRAND_PRIMARY },
  PENDING_DELIVERY: { text: ORDER_STATUS_TEXT.PENDING_DELIVERY, color: BRAND_PRIMARY },
  DELIVERING: { text: ORDER_STATUS_TEXT.DELIVERING, color: '#389E0D' },
  COMPLETED: { text: ORDER_STATUS_TEXT.COMPLETED, color: TEXT_SECONDARY },
  CANCELLED: { text: ORDER_STATUS_TEXT.CANCELLED, color: '#CF1322' },
  REFUNDED: { text: ORDER_STATUS_TEXT.REFUNDED, color: '#CF1322' },
};

export interface OrderStatusStat {
  status: string;
  count: number;
}

export interface OrderValueBucketStat {
  fromCents: number | null;
  toCents: number | null;
  label: string;
  count: number;
}

export interface PeakHourStat {
  hour: number;
  count: number;
}

export interface OrderStats {
  totalOrders: number;
  completedOrders: number;
  totalRevenueCents: number;
  averageOrderValueCents: number;
  statusDistribution: OrderStatusStat[];
  orderValueDistribution: OrderValueBucketStat[];
  peakHourDistribution: PeakHourStat[];
}

export interface DailyStat {
  date: string;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
}

export interface WeeklyStat {
  startDate: string;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
}

export interface MonthlyStat {
  year: number;
  month: number;
  monthName: string;
  orderCount: number;
  orderProductCount: number;
  revenueCents: number;
}

export interface DailyStatisticsData {
  orderStatistics: OrderStats;
  dailyStatistics: DailyStat[];
}

export interface WeeklyStatisticsData {
  orderStatistics: OrderStats;
  weeklyStatistics: WeeklyStat[];
}

export interface MonthlyStatisticsData {
  orderStatistics: OrderStats;
  monthlyStatistics: MonthlyStat[];
}

/** 趋势折线图数据点(页内派生) */
export interface TrendChartPoint {
  date: string;
  orders: number;
  revenue: number;
  products: number;
}
