import Taro from '@tarojs/taro'
import { networkManager } from '../utils/networkManager'

export interface WorkerInfo {
  id: number
  name: string
  phone: string
  avatarUrl: string
  wechatOpenId: string
}

export interface DeliveryLoginResponse {
  token: string
  refreshToken: string
  needBindPhone: boolean
  workerInfo: WorkerInfo | null
  message: string
}

const apiBaseUrl = process.env.TARO_APP_API_BASE_URL ?? 'http://localhost:9090'

class DeliveryAuthService {
  private token: string | null = null
  private workerInfo: WorkerInfo | null = null
  private openId: string | null = null
  private loginPromise: Promise<boolean> | null = null

  constructor() {
    this.loadStoredAuth()
  }

  private loadStoredAuth() {
    try {
      this.token = Taro.getStorageSync('auth_token')
      const storedWorkerInfo = Taro.getStorageSync('worker_info')
      if (storedWorkerInfo) {
        this.workerInfo = JSON.parse(storedWorkerInfo)
      }
      this.openId = Taro.getStorageSync('worker_openid')
    } catch (error) {
      console.error('Failed to load stored auth:', error)
    }
  }

  private storeAuth(token: string, workerInfo: WorkerInfo, openId: string) {
    try {
      this.token = token
      this.workerInfo = workerInfo
      this.openId = openId

      Taro.setStorageSync('auth_token', token)
      Taro.setStorageSync('worker_info', JSON.stringify(workerInfo))
      Taro.setStorageSync('worker_openid', openId)
    } catch (error) {
      console.error('Failed to store auth:', error)
    }
  }

  private clearStoredAuth() {
    try {
      this.token = null
      this.workerInfo = null
      this.openId = null

      Taro.removeStorageSync('auth_token')
      Taro.removeStorageSync('worker_info')
      Taro.removeStorageSync('worker_openid')
    } catch (error) {
      console.error('Failed to clear auth:', error)
    }
  }

  isAuthenticated(): boolean {
    return !!this.token && !!this.workerInfo
  }

  getToken(): string | null {
    return this.token
  }

  getWorkerInfo(): WorkerInfo | null {
    return this.workerInfo
  }

  getOpenId(): string | null {
    return this.openId
  }

  /**
   * WeChat login for delivery worker
   */
  async weChatLogin(): Promise<DeliveryLoginResponse> {
    try {
      // Step 1: Get WeChat code
      const loginRes = await this.getWeChatCode()
      if (loginRes.errMsg !== 'login:ok') {
        throw new Error(`微信登录失败: ${loginRes.errMsg}`)
      }

      // Step 2: Call backend API

      // Call backend API
      const loginData = await this.callLoginAPI(loginRes.code)

      // Step 3: If already bound, store auth
      if (loginData.workerInfo && loginData.token) {
        this.storeAuth(loginData.token, loginData.workerInfo, loginData.openId || '')
      }

      return loginData
    } catch (error) {
      console.error('WeChat login failed:', error)
      throw error
    }
  }

  /**
   * Silent login - used on app start
   */
  async silentLogin(): Promise<boolean> {
    if (this.loginPromise) {
      console.log('Silent login in progress, waiting...')
      return this.loginPromise
    }

    // If already authenticated, return true
    if (this.token && this.workerInfo) {
      return true
    }

    this.loginPromise = (async () => {
      try {
        console.log('Starting silent login...')
        await this.weChatLogin()
        console.log('Silent login successful')
        return true
      } catch (error) {
        console.warn('Silent login failed:', error)
        return false
      } finally {
        this.loginPromise = null
      }
    })()

    return this.loginPromise
  }

  private getWeChatCode(): Promise<{ code: string; errMsg: string }> {
    return new Promise((resolve, reject) => {
      Taro.login({
        success: (res) => resolve(res as any),
        fail: reject
      })
    })
  }

  private async callLoginAPI(code: string): Promise<DeliveryLoginResponse & { openId?: string }> {

    try {
      const response = await Taro.request({
        url: `${apiBaseUrl}/api/auth/delivery/login`,
        method: 'POST',
        header: {
          'Content-Type': 'application/json',
        },
        data: { code },
      })

      if (response.statusCode !== 200) {
        throw new Error(response.data?.message || '登录失败')
      }

      const result = response.data
      if (!result.success) {
        throw new Error(result.message || '登录失败')
      }

      const loginResult = result.data

      if (!loginResult.needBindPhone) {
        return {
          token: loginResult.token,
          refreshToken: loginResult.refreshToken,
          needBindPhone: loginResult.needBindPhone,
          workerInfo: loginResult.workerInfo,
          message: loginResult.message,
          openId: '',
        }
      }

      return {
        needBindPhone: loginResult.needBindPhone,
        message: loginResult.message,
        openId: loginResult.openId,
        token: '',
        refreshToken: '',
        workerInfo: null,
      }
    } catch (error) {
      console.error('Login REST call failed:', error)
      if (error instanceof Error) {
        throw error
      }
      throw new Error('网络连接失败，请检查网络设置')
    }
  }

  /**
   * Bind phone number to delivery worker
   */
  async bindPhone(phoneNumber: string): Promise<DeliveryLoginResponse> {
    if (!this.openId) {
      throw new Error('OpenID不存在，请重新登录')
    }

    try {
      const mutation = `
        mutation BindDeliveryPhone($input: BindDeliveryPhoneInput!) {
          bindDeliveryPhone(input: $input) {
            token
            refreshToken
            needBindPhone
            workerInfo {
              id
              name
              phone
              avatarUrl
              wechatOpenId
            }
            message
            openId
          }
        }
      `

      const response = await networkManager.mutate<{ bindDeliveryPhone: DeliveryLoginResponse }>(
        mutation,
        { input: { openId: this.openId, phoneNumber } }
      )

      const bindResult = response.bindDeliveryPhone

      // Store auth after successful binding
      if (bindResult.workerInfo && bindResult.token) {
        this.storeAuth(bindResult.token, bindResult.workerInfo, this.openId)
      }

      return bindResult
    } catch (error) {
      console.error('Bind phone failed:', error)
      if (error instanceof Error) {
        throw error
      }
      throw new Error('网络连接失败，请检查网络设置')
    }
  }

  /**
   * Set openId after phone binding is required
   */
  setOpenId(openId: string) {
    this.openId = openId
    Taro.setStorageSync('worker_openid', openId)
  }

  /**
   * Logout
   */
  async logout(): Promise<void> {
    this.clearStoredAuth()
    // Redirect is handled by useAuth.logout()
  }

  /**
   * Check authentication status with stored token
   */
  async checkAuthStatus(): Promise<DeliveryLoginResponse | null> {
    const token = this.getToken()
    if (!token) {
      return null
    }

    try {
      const response = await networkManager.get<{ success: boolean; data: DeliveryLoginResponse }>(
        '/api/auth/delivery/check',
        { skipAuth: true, headers: { 'Authorization': 'Bearer ' + token } }
      )

      if (response.success && response.data) {
        const data = response.data
        // Update stored worker info if returned
        if (data.workerInfo) {
          this.workerInfo = data.workerInfo
          Taro.setStorageSync('worker_info', JSON.stringify(data.workerInfo))
        }
        return {
          token: data.token,
          refreshToken: data.refreshToken,
          needBindPhone: data.needBindPhone,
          workerInfo: data.workerInfo,
          message: data.message,
          openId: data.openId,
        }
      }
      return null
    } catch (error: any) {
      if (error.statusCode === 401) {
        // Token invalid, clear auth
        this.clearStoredAuth()
        return null
      }
      console.error('Check auth status failed:', error)
      return null
    }
  }
}

export const deliveryAuthService = new DeliveryAuthService()
