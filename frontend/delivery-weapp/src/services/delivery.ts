import { networkManager } from '../utils/networkManager'
import type { Order, TodayStatistics, WeekStatistics, Address, ProductPage, Scalars, OrderStatus, OrderPage } from '@aquarush/common'

// 订单操作记录类型
interface OrderOperation {
  id: string
  operationType: string
  description?: string
  createdAt: string
}

// 获取配送任务列表
export const getDeliveryTasks = (workerId: string) => {
  return networkManager.get(`/orders/delivery-worker/${workerId}`)
}

// 更新配送员状态
export const updateWorkerStatus = (workerId: string, status: 'ONLINE' | 'OFFLINE') => {
  return networkManager.post(`/delivery/workers/${workerId}/status`, { status })
}

// 确认送达
export const confirmDelivery = (orderId: string, photos: string[]) => {
  return networkManager.post(`/delivery/confirm/${orderId}`, { photos })
}

// ==================== 派单相关 API ====================

// 获取待接单订单列表
export const getPendingDeliveryOrders = () => {
  return networkManager.query<{ pendingDeliveryOrders: ReadonlyArray<Order> }>(`
    query {
      pendingDeliveryOrders {
        id
        orderNo
        quantity
        amount
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
        product {
          name
        }
      }
    }
  `)
}

// 获取我的已接单订单（未开始配送）
export const getMyAssignedOrders = () => {
  return networkManager.query<{ myAssignedOrders: ReadonlyArray<Order> }>(`
    query GetAssignedOrders {
      assignedOrders {
        id
        orderNo
        quantity
        amount
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
        product {
          name
        }
      }
    }
  `, )
}

// 获取我的配送中订单
export const getMyDeliveringOrders = () => {
  return networkManager.query<{ myDeliveringOrders: ReadonlyArray<Order> }>(`
    query GetDeliveringOrders {
      deliveringOrders {
        id
        orderNo
        quantity
        amount
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
        product {
          name
        }
      }
    }
  `,)
}

// 获取订单详情
export const getOrderDetail = (orderNo: string) => {
  return networkManager.query<{ orderByNo: Order }>(`
    query GetOrderDetail($orderNo: String!) {
      orderByNo(orderNo: $orderNo) {
        id
        orderNo
        status
        quantity
        amount
        isSelfCollect
        remark
        createdAt
        deliveryStartedAt
        deliveryConfirmedAt
        paymentType
        deliveryPhotos
        product {
          id
          name
          price
          coverImageUrl
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
        }
      }
    }
  `, { orderNo })
}

// 获取订单操作记录
export const getOrderOperations = (orderId: string) => {
  return networkManager.query<{ orderOperations: OrderOperation[] }>(`
    query GetOrderOperations($orderId: PrimaryId!) {
      orderOperations(orderId: $orderId) {
        id
        operationType
        description
        createdAt
      }
    }
  `, { orderId: parseInt(orderId) })
}

// 配送员接单
export const acceptDelivery = (orderId: Scalars['PrimaryId']['input']) => {
  return networkManager.mutate<{ acceptDelivery: Order }>(`
    mutation AcceptDelivery($orderId: PrimaryId!) {
      acceptDelivery(orderId: $orderId) {
        id
        status
      }
    }
  `, { orderId })
}

// 开始配送
export const startDelivery = (orderId: Scalars['PrimaryId']['input']) => {
  return networkManager.mutate<{ startDelivery: Order }>(`
    mutation StartDelivery($orderId: PrimaryId!) {
      startDelivery(orderId: $orderId) {
        id
        status
        deliveryStartedAt
      }
    }
  `, { orderId })
}

// 完成配送
export const completeDelivery = (orderId: Scalars['PrimaryId']['input'], photos: string[], paymentType?: string, remark?: string) => {
  return networkManager.mutate<{ completeDelivery: Order }>(`
    mutation CompleteDelivery($orderId: PrimaryId!, $photos: [String!]!, $paymentType: PaymentType, $remark: String) {
      completeDelivery(orderId: $orderId, photos: $photos, paymentType: $paymentType, remark: $remark) {
        id
        status
        deliveryConfirmedAt
      }
    }
  `, { orderId, photos, paymentType, remark })
}

// 获取当日统计数据
export const getTodayStatistics = () => {
  return networkManager.query<{ todayStatistics: TodayStatistics }>(`
    query GetTodayStatistics {
      todayStatistics {
        totalOrders
        completedOrders
        unfinishedOrders
        earningCents
      }
    }
  `, {})
}

// 获取一周统计数据
export const getWeekStatistics = () => {
  return networkManager.query<{ weekStatistics: WeekStatistics }>(`
    query GetWeekStatistics {
      weekStatistics {
        dailyStats {
          date
          orderCount
          earning
        }
        totalOrders
        totalEarning
      }
    }
  `, {})
}

// 获取配送员历史订单（已完成、已取消、已退款）
export const getDeliveryWorkerHistoryOrders = (
  status?: OrderStatus,
  keyword?: string,
  page = 0,
  size = 20
) => {
  return networkManager.query<{ deliveryWorkerHistoryOrders: OrderPage }>(`
    query GetHistoryOrders($status: OrderStatus, $keyword: String, $page: Int, $size: Int) {
      deliveryWorkerHistoryOrders(status: $status, keyword: $keyword, page: $page, size: $size) {
        content {
          id
          orderNo
          quantity
          amount
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
          product {
            name
          }
        }
        totalElements
        totalPages
        number
        size
      }
    }
  `, { status, keyword, page, size })
}

// ==================== 地址和商品相关 API ====================

// 获取用户地址列表
export const getAddresses = () => {
  return networkManager.query<{ addresses: ReadonlyArray<Address> }>(`
    query {
      addresses {
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
  `)
}

// 搜索用户地址
export const searchAddresses = (keyword: string, size = 10) => {
  return networkManager.query<{ searchUserAddresses: ReadonlyArray<Address> }>(`
    query searchUserAddresses($keyword: String!, $size: Int) {
      searchUserAddresses(keyword: $keyword, size: $size) {
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
  `, { keyword, size })
}

// 搜索所有地址 (管理员/配送员)
export const searchAllAddresses = (keyword: string = '', size = 10) => {
  return networkManager.query<{ searchAllAddresses: ReadonlyArray<Address> }>(`
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
  `, { keyword, size })
}

// 获取商品列表
export const getProducts = () => {
  return networkManager.query<{ products: ProductPage }>(`
    query {
      products {
        list {
          id
          name
          price
          image
          stock
        }
        pageInfo {
          total
        }
      }
    }
  `)
}

// 搜索上架商品（带关键字过滤）- 使用admin GraphQL端点
export const searchProducts = (keyword: string, size: number = 20) => {
  return networkManager.query<{ activeProducts: ProductPage }>(`
    query SearchActiveProducts($keyword: String, $size: Int) {
      activeProducts(keyword: $keyword, size: $size) {
        list {
          id
          name
          price
          stock
          coverImageUrl
          status
        }
        pageInfo {
          total
        }
      }
    }
  `, { keyword: keyword || '', size })
}

// 创建订单（配送员为用户创建订单）
export const createOrder = (
  productId: Scalars['PrimaryId']['input'],
  addressId: Scalars['PrimaryId']['input'],
  quantity: number,
  isSelfCollect: boolean = false,
  remark?: string
) => {
  return networkManager.mutate<{ createDeliveryOrder: Order }>(`
    mutation CreateDeliveryOrder($input: CreateDeliveryOrderInput!) {
      createDeliveryOrder(input: $input) {
        id
        orderNo
        status
        quantity
        amount
        isSelfCollect
      }
    }
  `, {
    input: {
      productId,
      addressId,
      quantity,
      isSelfCollect,
      remark: remark || null
    }
  })
}
