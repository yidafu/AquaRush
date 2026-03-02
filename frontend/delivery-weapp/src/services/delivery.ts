import Taro from '@tarojs/taro'

const API_BASE_URL = process.env.TARO_APP_API_BASE_URL || 'http://localhost:9090'

// 通用请求方法
const doRequest = async (options: {
  url: string
  method?: 'GET' | 'POST'
  data?: any
}) => {
  const token = Taro.getStorageSync('worker_token') || Taro.getStorageSync('token')

  const response = await Taro.request({
    url: `${API_BASE_URL}${options.url}`,
    method: options.method || 'GET',
    data: options.data,
    header: {
      'Content-Type': 'application/json',
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }
  })

  if (response.statusCode === 401) {
    // Unauthorized - logout
    Taro.removeStorageSync('worker_token')
    Taro.removeStorageSync('worker_info')
    Taro.reLaunch({ url: '/pages/login/index' })
    throw new Error('登录已过期，请重新登录')
  }

  if (response.statusCode !== 200) {
    Taro.showToast({
      title: response.data?.message || '请求失败',
      icon: 'none'
    })
    throw new Error(response.data?.message || '请求失败')
  }

  return response.data
}

// Request object with get/post methods
const request = {
  get: (url: string) => doRequest({ url, method: 'GET' }),
  post: (url: string, data?: any) => doRequest({ url, method: 'POST', data }),
}

// 获取配送任务列表
export const getDeliveryTasks = (workerId: string) => {
  return request.get(`/orders/delivery-worker/${workerId}`)
}

// 更新配送员状态
export const updateWorkerStatus = (workerId: string, status: 'ONLINE' | 'OFFLINE') => {
  return request.post(`/delivery/workers/${workerId}/status`, { status })
}

// 确认送达
export const confirmDelivery = (orderId: string, photos: string[]) => {
  return request.post(`/delivery/confirm/${orderId}`, { photos })
}

// ==================== 派单相关 API ====================

// 获取待接单订单列表
export const getPendingDeliveryOrders = () => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        query {
          pendingDeliveryOrders {
            id
            orderNumber
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
      `
    }
  })
}

// 获取我的已接单订单（未开始配送）
export const getMyAssignedOrders = (workerId: number) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        query GetAssignedOrders($workerId: Long!) {
          assignedOrders(workerId: $workerId) {
            id
            orderNumber
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
      `,
      variables: { workerId }
    }
  })
}

// 获取我的配送中订单
export const getMyDeliveringOrders = (workerId: number) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        query GetDeliveringOrders($workerId: Long!) {
          deliveringOrders(workerId: $workerId) {
            id
            orderNumber
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
      `,
      variables: { workerId }
    }
  })
}

// 配送员接单
export const acceptDelivery = (orderId: number, workerId: number) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        mutation AcceptDelivery($orderId: Long!, $workerId: Long!) {
          acceptDelivery(orderId: $orderId, workerId: $workerId) {
            id
            status
          }
        }
      `,
      variables: { orderId, workerId }
    }
  })
}

// 开始配送
export const startDelivery = (orderId: number) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        mutation StartDelivery($orderId: Long!) {
          startDelivery(orderId: $orderId) {
            id
            status
            deliveryStartedAt
          }
        }
      `,
      variables: { orderId }
    }
  })
}

// 完成配送
export const completeDelivery = (orderId: number, photos: string[], paymentType?: string) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        mutation CompleteDelivery($orderId: Long!, $photos: [String!]!, $paymentType: PaymentType) {
          completeDelivery(orderId: $orderId, photos: $photos, paymentType: $paymentType) {
            id
            status
            deliveryConfirmedAt
          }
        }
      `,
      variables: { orderId, photos, paymentType }
    }
  })
}

// 获取当日统计数据
export const getTodayStatistics = (workerId?: number) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        query GetTodayStatistics($workerId: Long) {
          todayStatistics(workerId: $workerId) {
            totalOrders
            completedOrders
            pendingOrders
            earningCents
          }
        }
      `,
      variables: workerId ? { workerId } : {}
    }
  })
}
