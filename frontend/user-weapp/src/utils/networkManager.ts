import Taro from '@tarojs/taro'
import { NetworkManager, AuthHandler, RequestHandler } from '@aquarush/common'
import { authService } from './auth'

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
    // 小程序端 filePath 恒为本地路径字符串(UploadFilePath 联合类型在 Taro 边界收窄)
    return Taro.uploadFile({ ...options, filePath: options.filePath as string }) as Promise<{
      statusCode: number
      data: string
      errMsg?: string
    }>
  }
}

// AuthHandler 工厂函数:直接对接全局 authService。
// 关键点:silentLogin 必须接通 authService.silentLogin(),否则 401/UNAUTHORIZED 时
// 共享 networkManager 只会清 token、不会拉起登录验证,请求直接抛错(用户看到裸 401)。
export const createAuthHandler = (): AuthHandler => ({
  getToken: () => authService.getToken(),
  getTokenAsync: async () => {
    try {
      let token = Taro.getStorageSync('auth_token')
      if (!token) {
        token = authService.getToken()
      }
      return token || null
    } catch (error) {
      console.error('Failed to get auth token asynchronously:', error)
      return null
    }
  },
  clearToken: () => {
    try {
      // 同时清内存态,避免 silentLogin() 因残留的 in-memory token 直接返回 true 而不真正登录;
      // 用 clearAuthState 而非 forceClearAuth,保留进行中的静默登录,并发 401 复用同一登录流程。
      authService.clearAuthState()
    } catch (error) {
      console.error('Failed to clear auth token:', error)
    }
  },
  silentLogin: () => authService.silentLogin()
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
  createAuthHandler(),
  taroRequestHandler
)

// 更新 authHandler 的函数(运行时重新接线)
export const updateAuthHandler = (getToken?: () => string | null, silentLogin?: () => Promise<boolean>) => {
  NetworkManager.setAuthHandler({
    ...createAuthHandler(),
    ...(getToken ? { getToken } : {}),
    ...(silentLogin ? { silentLogin } : {})
  })
}

export { NetworkManager }
