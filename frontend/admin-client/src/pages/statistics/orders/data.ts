import { gql } from '@apollo/client';
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

/** 订单状态中文名与图表色(G37 状态分布,7 态,与 OrderStatus 枚举一致;色值按 docs/ui-spec.md §2.2) */
export const ORDER_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING_PAYMENT: { text: '待支付', color: '#FA8C16' },
  PENDING_DISPATCH: { text: '待派单', color: BRAND_PRIMARY },
  PENDING_DELIVERY: { text: '待配送', color: BRAND_PRIMARY },
  DELIVERING: { text: '配送中', color: '#389E0D' },
  COMPLETED: { text: '已完成', color: TEXT_SECONDARY },
  CANCELLED: { text: '已取消', color: '#CF1322' },
  REFUNDED: { text: '已退款', color: '#CF1322' },
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
