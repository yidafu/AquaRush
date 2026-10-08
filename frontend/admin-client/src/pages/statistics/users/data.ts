import { gql } from '@apollo/client';

export const GET_USER_STATISTICS = gql`
  query GetUserStatistics {
    userStatistics {
      totalUsers
      todayNewUsers
      monthNewUsers
      activeUsers
      dailyNewUsers {
        date
        orderCount
      }
      loginStatistics {
        todayLogins
        totalLogins
        dailyLogins {
          date
          loginCount
        }
      }
      orderDistribution {
        orderingUsers
        orderingUserRatio
        repurchaseRate
        tiers {
          label
          userCount
        }
      }
    }
  }
`;

export interface DailyUserStat {
  date: string;
  orderCount: number;
}

export interface DailyLoginStat {
  date: string;
  loginCount: number;
}

export interface LoginStatistics {
  todayLogins: number;
  totalLogins: number;
  dailyLogins: DailyLoginStat[];
}

export interface OrderUserTier {
  label: string;
  userCount: number;
}

export interface OrderUserDistribution {
  orderingUsers: number;
  orderingUserRatio: number;
  repurchaseRate: number;
  tiers: OrderUserTier[];
}

export interface UserStatisticsData {
  userStatistics: {
    totalUsers: number;
    todayNewUsers: number;
    monthNewUsers: number;
    activeUsers: number;
    dailyNewUsers: DailyUserStat[];
    loginStatistics: LoginStatistics;
    orderDistribution: OrderUserDistribution;
  };
}

/**
 * 比率(0..1)转百分比文案(两位小数);null/非法值 → 0
 */
export function formatRatio(ratio: number | undefined | null): number {
  const value = typeof ratio === 'number' && Number.isFinite(ratio) ? ratio : 0;
  return Number((value * 100).toFixed(2));
}

/** 下单次数分层环形图配色(G35,复用订单状态分布色板风格) */
export const TIER_COLORS = ['#1677ff', '#52c41a', '#fa8c16'];
