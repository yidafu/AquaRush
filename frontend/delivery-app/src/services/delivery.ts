import { networkManager } from '@/utils/network-manager';
import type {
  Order,
  OrderStatus,
  OrderVoPage,
  ProductVoPage,
  Address,
  Scalars,
  TodayStatistics,
  UpdateOrderAddressInput,
  WeekStatistics,
} from '@aquarush/common';

// 订单操作记录类型
interface OrderOperation {
  id: string;
  operationType: string;
  description?: string;
  createdAt: string;
}

// ==================== 派单相关 API ====================

// 获取待接单订单列表
export const getPendingDeliveryOrders = () => {
  return networkManager.query<{ pendingDeliveryOrders: readonly Order[] }>(`
    query {
      pendingDeliveryOrders {
        id
        orderNo
        amountCents
        status
        isSelfCollect
        createdAt
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
          productId
          quantity
          product {
            name
            coverImageUrl
          }
        }
      }
    }
  `);
};

// 获取我的已接单订单（未开始配送）
export const getMyAssignedOrders = () => {
  return networkManager.query<{ assignedOrders: readonly Order[] }>(`
    query GetAssignedOrders {
      assignedOrders {
        id
        orderNo
        amountCents
        status
        isSelfCollect
        createdAt
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
          productId
          quantity
          product {
            name
            coverImageUrl
          }
        }
      }
    }
  `);
};

// 获取我的配送中订单
export const getMyDeliveringOrders = () => {
  return networkManager.query<{ deliveringOrders: readonly Order[] }>(`
    query GetDeliveringOrders {
      deliveringOrders {
        id
        orderNo
        amountCents
        status
        isSelfCollect
        deliveryStartedAt
        createdAt
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
          productId
          quantity
          product {
            name
            coverImageUrl
          }
        }
      }
    }
  `);
};

// 获取订单详情
export const getOrderDetail = (orderNo: string) => {
  return networkManager.query<{ orderByNo: Order }>(
    `
    query GetOrderDetail($orderNo: String!) {
      orderByNo(orderNo: $orderNo) {
        id
        orderNo
        status
        amountCents
        deliveryFeeCents
        payAmountCents
        isSelfCollect
        remark
        createdAt
        deliveryStartedAt
        deliveryConfirmedAt
        paymentType
        deliveryPhotos
        # 配送备注(完成配送时配送员填写,R-ORD-07;完成态「配送信息」卡回显)
        deliveryRemark
        # 营销计价快照(R-MKT-02/07/08/09/10:费用明细行展示依据)
        promotionDiscountCents
        promotionActivityName
        firstOrderDiscountCents
        firstOrderActivityName
        couponDiscountCents
        couponName
        ticketRedeemCount
        ticketDiscountCents
        giftItems {
          productId
          productName
          quantity
        }
        items {
          productId
          quantity
          unitPriceCents
          totalPriceCents
          product {
            id
            name
            priceCents
            coverImageUrl
            specification
          }
        }
        user {
          id
          nickname
          phone
        }
        address {
          id
          receiverName
          phone
          province
          city
          district
          detailAddress
          # 楼层/定位(WGS-84,订单页地址信息修正 F-ORD-024 编辑回显用)
          floor
          longitude
          latitude
        }
        deliveryWorker {
          id
          name
          phone
        }
      }
    }
  `,
    { orderNo },
  );
};

// 获取订单操作记录
export const getOrderOperations = (orderId: string) => {
  return networkManager.query<{ orderOperations: OrderOperation[] }>(
    `
    query GetOrderOperations($orderId: PrimaryId!) {
      orderOperations(orderId: $orderId) {
        id
        operationType
        description
        createdAt
      }
    }
  `,
    { orderId },
  );
};

// 配送员接单
export const acceptDelivery = (orderId: Scalars['PrimaryId']['input']) => {
  return networkManager.mutate<{ acceptDelivery: Order }>(
    `
    mutation AcceptDelivery($orderId: PrimaryId!) {
      acceptDelivery(orderId: $orderId) {
        id
        status
      }
    }
  `,
    { orderId },
  );
};

// 开始配送
export const startDelivery = (orderId: Scalars['PrimaryId']['input']) => {
  return networkManager.mutate<{ startDelivery: Order }>(
    `
    mutation StartDelivery($orderId: PrimaryId!) {
      startDelivery(orderId: $orderId) {
        id
        status
        deliveryStartedAt
      }
    }
  `,
    { orderId },
  );
};

// 完成配送
export const completeDelivery = (
  orderId: Scalars['PrimaryId']['input'],
  photos: string[],
  paymentType?: string,
  remark?: string,
) => {
  return networkManager.mutate<{ completeDelivery: Order }>(
    `
    mutation CompleteDelivery($orderId: PrimaryId!, $photos: [String!]!, $paymentType: PaymentType, $remark: String) {
      completeDelivery(orderId: $orderId, photos: $photos, paymentType: $paymentType, remark: $remark) {
        id
        status
        deliveryConfirmedAt
      }
    }
  `,
    { orderId, photos, paymentType, remark },
  );
};

// 配送任务中修正订单收货地址(F-ORD-024):收货人/电话/详细地址/楼层/定位(WGS-84 经纬度)。
// 仅传入字段生效(空值字段不覆盖原地址),终态订单(已完成/已取消/已退款)不可修改。
export const updateOrderAddress = (
  orderId: Scalars['PrimaryId']['input'],
  input: UpdateOrderAddressInput,
) => {
  return networkManager.mutate<{ updateOrderAddress: Order }>(
    `
    mutation UpdateOrderAddress($orderId: PrimaryId!, $input: UpdateOrderAddressInput!) {
      updateOrderAddress(orderId: $orderId, input: $input) {
        id
        status
        address {
          id
          receiverName
          phone
          detailAddress
          floor
          longitude
          latitude
        }
      }
    }
  `,
    { orderId, input },
  );
};

// ==================== 统计与历史订单相关 API ====================

/**
 * 工作台首页单请求(规则 #10):今日统计 + 近7天统计一次取回,
 * 均按当前登录账号(本人)收敛 —— 送水员只能看到本人送水的数据。
 * (独立「统计」Tab 已移除,统计收敛到工作台,原 getTodayStatistics/getWeekStatistics 已删除)
 */
export const getWorkbenchData = () => {
  return networkManager.query<{
    todayStatistics: TodayStatistics | null;
    weekStatistics: WeekStatistics | null;
  }>(`
    query GetWorkbenchData {
      todayStatistics {
        totalOrders
        completedOrders
        unfinishedOrders
        earningCents
      }
      weekStatistics {
        dailyStats {
          date
          orderCount
          earningCents
        }
        totalOrders
        totalEarningCents
      }
    }
  `);
};

// 获取配送员历史订单（已完成、已取消、已退款）
export const getDeliveryWorkerHistoryOrders = (
  status?: OrderStatus,
  keyword?: string,
  page = 0,
  size = 20,
) => {
  return networkManager.query<{ deliveryWorkerHistoryOrders: OrderVoPage }>(
    `
    query GetHistoryOrders($status: OrderStatus, $keyword: String, $page: Int, $size: Int) {
      deliveryWorkerHistoryOrders(status: $status, keyword: $keyword, page: $page, size: $size) {
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
            productId
            quantity
            product {
              name
              coverImageUrl
            }
          }
        }
        totalElements
        totalPages
        number
        size
      }
    }
  `,
    { status, keyword, page, size },
  );
};

// ==================== 地址和商品相关 API ====================

// 搜索所有地址 (管理员/配送员)
export const searchAllAddresses = (keyword: string = '', size = 10) => {
  return networkManager.query<{ searchAllAddresses: readonly Address[] }>(
    `
    query searchAllAddresses($keyword: String, $size: Int) {
      searchAllAddresses(keyword: $keyword, size: $size) {
        id
        receiverName
        phone
        province
        city
        district
        detailAddress
        isDefault
      }
    }
  `,
    { keyword, size },
  );
};

// 搜索上架商品（带关键字过滤）
export const searchProducts = (keyword: string, size: number = 20) => {
  return networkManager.query<{ activeProducts: ProductVoPage }>(
    `
    query SearchActiveProducts($keyword: String, $size: Int) {
      activeProducts(keyword: $keyword, size: $size) {
        list {
          id
          name
          priceCents
          stock
          coverImageUrl
          status
        }
        pageInfo {
          total
        }
      }
    }
  `,
    { keyword: keyword || '', size },
  );
};

// 代客下单商品明细项(F-ORD-012 多商品下单,items 为唯一口径 —— F-ORD-004)
export type CreateDeliveryOrderItem = {
  productId: Scalars['PrimaryId']['input'];
  quantity: number;
};

// 创建订单（配送员为用户创建订单,支持多商品 items）
export const createOrder = (
  items: CreateDeliveryOrderItem[],
  addressId: Scalars['PrimaryId']['input'],
  isSelfCollect: boolean = false,
  remark?: string,
) => {
  return networkManager.mutate<{ createDeliveryOrder: Order }>(
    `
    mutation CreateDeliveryOrder($input: CreateDeliveryOrderInput!) {
      createDeliveryOrder(input: $input) {
        id
        orderNo
        status
        amountCents
        isSelfCollect
      }
    }
  `,
    {
      input: {
        items,
        addressId,
        isSelfCollect,
        remark: remark || null,
      },
    },
  );
};

export type { Order, OrderStatus, OrderVoPage, ProductVoPage, Address };