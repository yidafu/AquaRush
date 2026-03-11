import Taro from '@tarojs/taro'
import NetworkManager, { AuthHandler, RequestHandler } from '@aquarush/common/utils/network'

const apiBaseUrl = import.meta.env.TARO_APP_API_BASE_URL || 'http://localhost:8080'

// Taro 请求处理器
const taroRequestHandler: RequestHandler = {
  request: async (options) => {
    return Taro.request(options) as Promise<{
      statusCode: number
      data: any
      errMsg?: string
    }>
  },
  uploadFile: async (options) => {
    return Taro.uploadFile(options) as Promise<{
      statusCode: number
      data: string
      errMsg?: string
    }>
  }
}

// AuthHandler 工厂函数
export const createAuthHandler = (getToken: () => string | null): AuthHandler => ({
  getToken,
  getTokenAsync: async () => {
    try {
      let token = Taro.getStorageSync('auth_token')
      if (token) {
        return token
      }
      // 如果没有 token，返回 null 让调用方处理
      return null
    } catch (error) {
      console.error('Failed to get auth token asynchronously:', error)
      return null
    }
  },
  clearToken: () => {
    try {
      Taro.removeStorageSync('auth_token')
    } catch (error) {
      console.error('Failed to clear auth token:', error)
    }
  },
  silentLogin: undefined // 由外部设置
})

// 初始化网络管理器
export const networkManager = NetworkManager.getInstance(
  {
    baseURL: `${apiBaseUrl}/graphql`,
    timeout: 10000,
    headers: {
      'Content-Type': 'application/json'
    }
  },
  createAuthHandler(() => null),
  taroRequestHandler
)

// 更新 authHandler 的函数
export const updateAuthHandler = (getToken: () => string | null, silentLogin?: () => Promise<boolean>) => {
  NetworkManager.getInstance(
    {
      baseURL: `${apiBaseUrl}/graphql`,
      timeout: 10000,
      headers: {
        'Content-Type': 'application/json'
      }
    },
    {
      ...createAuthHandler(getToken),
      silentLogin
    },
    taroRequestHandler
  )
}

export { NetworkManager }
