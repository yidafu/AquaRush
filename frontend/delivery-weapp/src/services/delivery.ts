import Taro from '@tarojs/taro'

const API_BASE_URL = 'http://localhost:9090'

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

// ==================== 地址和商品相关 API ====================

// 获取用户地址列表
export const getAddresses = () => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
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
      `
    }
  })
}

// 搜索用户地址
export const searchAddresses = (keyword: string, size = 10) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
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
      `,
      variables: { keyword, size }
    }
  })
}

// 搜索所有地址 (管理员/配送员)
export const searchAllAddresses = (keyword: string = '', size = 10) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
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
      variables: { keyword, size }
    }
  })
}

// 获取商品列表
export const getProducts = () => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        query {
          products {
            id
            name
            price
            image
            stock
          }
        }
      `
    }
  })
}

// 搜索上架商品（带关键字过滤）- 使用admin GraphQL端点
export const searchProducts = (keyword: string, size: number = 20) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
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
      `,
      variables: {
        keyword: keyword || '',
        size
      }
    }
  })
}

// 创建订单（配送员为用户创建订单）
export const createOrder = (productId: string, addressId: string, quantity: number, isSelfCollect: boolean = false, remark?: string) => {
  return doRequest({
    url: '/graphql',
    method: 'POST',
    data: {
      query: `
        mutation CreateDeliveryOrder($input: CreateDeliveryOrderInput!) {
          createDeliveryOrder(input: $input) {
            id
            orderNumber
            status
            quantity
            amount
            isSelfCollect
          }
        }
      `,
      variables: {
        input: {
          productId,
          addressId,
          quantity,
          isSelfCollect,
          remark: remark || null
        }
      }
    }
  })
}
