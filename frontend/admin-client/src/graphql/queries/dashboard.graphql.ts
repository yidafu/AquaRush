import { gql } from '@apollo/client';

/**
 * 仪表盘页(Dashboard)单页查询:
 * 一次请求取回「日统计(时间范围)+ 待处理订单存量快照」,满足
 * AGENTS.md 规则 7 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const DASHBOARD_PAGE_DATA_QUERY = gql`
  query DashboardPageData($input: DateRangeInput!) {
    dailyStatistics(input: $input) {
      date
      orderCount
      orderProductCount
      revenueCents
    }
    pendingOrderBreakdown {
      pendingPayment
      pendingDispatch
      pendingDelivery
      total
    }
  }
`;

/** DashboardPageData 操作名(供 refetchQueries 引用) */
export const DASHBOARD_PAGE_DATA = 'DashboardPageData';
