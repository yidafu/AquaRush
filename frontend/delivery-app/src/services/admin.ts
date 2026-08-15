import { networkManager } from '@/utils/network-manager';
import type {
  DailyStatistic,
  DeliveryWorker,
  DeliveryWorkerOverview,
  DeliveryWorkerRankingItem,
  Order,
  OrderListInput,
  OrderStatus,
  OrderVoPage,
  PendingOrderBreakdown,
  Scalars,
  UserStatistics,
} from '@aquarush/common';

/** LocalDateTime 标量使用 ISO_LOCAL_DATE_TIME（如 2026-08-09T00:00:00）。 */
function toIsoDateTime(d: Date): string {
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}T00:00:00`;
}

// ==================== 经营看板 ====================

/** 待处理订单三态细分 */
export const getPendingOrderBreakdown = () => {
  return networkManager.query<{ pendingOrderBreakdown: PendingOrderBreakdown }>(`
    query GetPendingOrderBreakdown {
      pendingOrderBreakdown {
        pendingPayment
        pendingDispatch
        pendingDelivery
        total
      }
    }
  `);
};

/** 送水员统计概览 */
export const getDeliveryWorkerStatistics = () => {
  return networkManager.query<{ deliveryWorkerStatistics: DeliveryWorkerOverview }>(`
    query GetDeliveryWorkerStatistics {
      deliveryWorkerStatistics {
        totalWorkers
        todayActiveWorkers
        deliveringOrders
        todayCompletedOrders
      }
    }
  `);
};

/** 用户统计（管理员） */
export const getUserStatistics = () => {
  return networkManager.query<{ userStatistics: Pick<UserStatistics, 'totalUsers' | 'todayNewUsers' | 'activeUsers'> }>(`
    query GetUserStatistics {
      userStatistics {
        totalUsers
        todayNewUsers
        activeUsers
      }
    }
  `);
};

/** 送水员排行榜 */
export const getDeliveryWorkerRanking = (limit: number = 10) => {
  return networkManager.query<{ deliveryWorkerRanking: readonly DeliveryWorkerRankingItem[] }>(
    `
    query GetDeliveryWorkerRanking($limit: Int) {
      deliveryWorkerRanking(limit: $limit) {
        workerId
        name
        todayCompletedOrders
        rating
        totalEarningsCents
      }
    }
  `,
    { limit },
  );
};

/** 按日统计（管理员）— 供看板近7天趋势 */
export const getDailyStatistics = (startDate: Date, endDate: Date) => {
  return networkManager.query<{ dailyStatistics: readonly DailyStatistic[] }>(
    `
    query GetDailyStatistics($input: DateRangeInput!) {
      dailyStatistics(input: $input) {
        date
        orderCount
        orderProductCount
        revenueCents
      }
    }
  `,
    { input: { startDate: toIsoDateTime(startDate), endDate: toIsoDateTime(endDate) } },
  );
};

// ==================== 全部订单管理 ====================

export interface AdminOrderListOptions {
  status?: OrderStatus;
  search?: string;
  page?: number;
  size?: number;
}

/** 管理员全部订单（分页），search 仅匹配订单号 */
export const getAdminOrders = (options: AdminOrderListOptions = {}) => {
  const input: OrderListInput = {
    page: options.page ?? 0,
    size: options.size ?? 20,
    status: options.status,
    search: options.search || undefined,
  };
  return networkManager.query<{ orders: OrderVoPage }>(
    `
    query GetAdminOrders($input: OrderListInput) {
      orders(input: $input) {
        content {
          id
          orderNo
          amountCents
          status
          isSelfCollect
          createdAt
          deliveryConfirmedAt
          user {
            nickname
            phone
          }
          address {
            detailAddress
            receiverName
            phone
          }
          items {
            product {
              name
            }
            quantity
          }
          deliveryWorker {
            id
            name
          }
        }
        totalElements
        totalPages
        number
        size
      }
    }
  `,
    { input },
  );
};

// ==================== 送水员管理 ====================

/** 全部送水员（管理员） */
export const getDeliveryWorkers = () => {
  return networkManager.query<{ deliveryWorkers: readonly DeliveryWorker[] }>(`
    query GetDeliveryWorkers {
      deliveryWorkers {
        id
        name
        phone
        onlineStatus
        isAvailable
        currentTaskCount
        earningCents
      }
    }
  `);
};

/** 在线送水员（管理员）— 派单候选 */
export const getOnlineDeliveryWorkers = () => {
  return networkManager.query<{ onlineDeliveryWorkers: readonly DeliveryWorker[] }>(`
    query GetOnlineDeliveryWorkers {
      onlineDeliveryWorkers {
        id
        name
        phone
        onlineStatus
        isAvailable
        currentTaskCount
      }
    }
  `);
};

// ==================== 订单操作 ====================

/** 管理员为订单分配送水员 */
export const assignDeliveryWorker = (
  orderId: Scalars['PrimaryId']['input'],
  workerId: Scalars['PrimaryId']['input'],
  isSelfCollect: boolean,
) => {
  return networkManager.mutate<{ assignDeliveryWorker: Order }>(
    `
    mutation AssignDeliveryWorker($orderId: PrimaryId!, $workerId: PrimaryId!, $isSelfCollect: Boolean!) {
      assignDeliveryWorker(orderId: $orderId, workerId: $workerId, isSelfCollect: $isSelfCollect) {
        id
        status
        deliveryWorker {
          id
          name
        }
      }
    }
  `,
    { orderId, workerId, isSelfCollect },
  );
};

export type { DeliveryWorker, DeliveryWorkerOverview, DeliveryWorkerRankingItem, OrderVoPage, PendingOrderBreakdown, UserStatistics };
