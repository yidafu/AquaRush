import Taro from '@tarojs/taro'
import { useState, useCallback } from 'react'
import { GraphQLError } from '@aquarush/common'
import { networkManager } from './networkManager'

export interface WeChatAuthResponse {
  code: string
  errMsg: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  userInfo: {
    id: string
    nickname: string
    avatarUrl: string
    wechatOpenId: string
  }
}

export interface UserInfo {
  id: string
  nickname: string
  avatarUrl: string
  phone: string
  wechatOpenId: string;
  // balance: number
  // points: number
  // level: string
  // isVip: boolean
  // vipExpireTime?: string
}

// 与 utils/networkManager.ts 保持一致:优先取构建期环境变量,避免真机/非本机环境下 localhost 不可达
const apiBaseUrl = import.meta.env.TARO_APP_API_BASE_URL || 'http://localhost:8080';

console.log('import API_BASE_URL', apiBaseUrl)
console.log('API_BASE_URL', Taro.env)
class AuthService {
  private token: string | null = null
  private userInfo: UserInfo | null = null
  private silentLoginPromise: Promise<boolean> | null = null

  constructor() {
    this.loadStoredAuth()
  }

  private loadStoredAuth() {
    try {
      // 直接从本地存储读取 token(共享 NetworkManager 的 AuthHandler 亦从本存储读取,保持一致)
      this.token = Taro.getStorageSync('auth_token') || null

      const storedUserInfo = Taro.getStorageSync('user_info')
      if (storedUserInfo) {
        this.userInfo = JSON.parse(storedUserInfo)
      }
    } catch (error) {
      console.error('Failed to load stored auth:', error)
    }
  }

  private storeAuth(token: string, userInfo: UserInfo) {
    if (!token) { return}
    try {
      this.token = token
      this.userInfo = userInfo

      // 共享 NetworkManager 的 setAuthToken 为 no-op(由 AuthHandler 负责持久化),这里直接写存储
      Taro.setStorageSync('auth_token', token)

      // 继续存储用户信息到本地
      Taro.setStorageSync('user_info', JSON.stringify(userInfo))
    } catch (error) {
      console.error('Failed to store auth:', error)
    }
  }

  private clearStoredAuth() {
    try {
      this.token = null
      this.userInfo = null

      // 清除进行中的静默登录 Promise
      this.silentLoginPromise = null

      // 直接清除本地存储
      Taro.removeStorageSync('auth_token')

      // 继续清除本地用户信息
      Taro.removeStorageSync('user_info')
    } catch (error) {
      console.error('Failed to clear stored auth:', error)
    }
  }

  isAuthenticated(): boolean {
    return !!this.token && !!this.userInfo
  }

  getToken(): string | null {
    return this.token
  }

  getUserInfo(): UserInfo | null {
    return this.userInfo
  }


  async weChatLogin(): Promise<LoginResponse> {
    try {
      // 第一步：获取微信登录凭证
      const loginRes = await this.getWeChatCode()
      if (loginRes.errMsg !== 'login:ok') {
        throw new Error(`微信登录失败: ${loginRes.errMsg}`)
      }

      // 第二步：调用后端登录接口
      const loginData = await this.callLoginAPI(loginRes.code)

      // 第三步：存储认证信息
      this.storeAuth(loginData.token, loginData.userInfo)

      return loginData
    } catch (error) {
      console.error('WeChat login failed:', error)
      throw error
    }
  }

  // 静默登录方法，用于app启动时自动登录
  async silentLogin(): Promise<boolean> {
    // 如果已经有进行中的静默登录请求，返回同一个 Promise
    if (this.silentLoginPromise) {
      console.log('🔄 静默登录正在进行中，等待现有请求完成...')
      return this.silentLoginPromise
    }

    // 检查是否已经有有效的 token
    if (this.token && this.userInfo) {
      return true
    }

    // 创建新的静默登录 Promise
    this.silentLoginPromise = (async () => {
      try {
        console.log('🔐 开始静默登录...')
        await this.weChatLogin()
        console.log('✅ 静默登录成功')
        return true
      } catch (error) {
        console.warn('⚠️ 静默登录失败:', error)
        return false
      } finally {
        // 无论成功失败，都清除 Promise 引用
        this.silentLoginPromise = null
      }
    })()

    return this.silentLoginPromise
  }

  private async getWeChatCode(): Promise<WeChatAuthResponse> {
    return new Promise((resolve, reject) => {
      Taro.login({
        success: resolve,
        fail: reject
      })
    })
  }

  private async callLoginAPI(code: string): Promise<LoginResponse> {
    try {
      const response = await Taro.request({
        url: `${apiBaseUrl}/api/auth/wechat/login`,
        method: 'POST',
        data: { code },
        header: {
          'Content-Type': 'application/json'
        }
      });

      if (response.statusCode !== 200) {
        throw new Error(`HTTP ${response.statusCode}: ${response.data?.message || '登录失败'}`);
      }

      const apiResponse = response.data;

      if (!apiResponse.success) {
        throw new Error(apiResponse.message || '登录失败');
      }

      const loginResult = apiResponse.data;

      return {
        token: loginResult.accessToken,
        refreshToken: loginResult.refreshToken,
        userInfo: {
          id: loginResult.userInfo.id.toString(),
          nickname: loginResult.userInfo.nickname || '微信用户',
          avatarUrl: loginResult.userInfo.avatar || '',
          wechatOpenId: loginResult.userInfo.wechatOpenId
        }
      };
    } catch (error) {
      console.error('Login API call failed:', error);
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('网络连接失败，请检查网络设置');
    }
  }

  async logout(): Promise<void> {
    try {
      // 调用后端登出接口（可选）
      if (this.token) {
        await Taro.request({
          url: `${apiBaseUrl}/api/auth/logout`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.token}`
          }
        });
      }
    } catch (error) {
      console.error('Logout API call failed:', error)
      // 即使后端登出失败，也要清除本地存储
    } finally {
      this.clearStoredAuth()
    }
  }

  async getCurrentUser(): Promise<UserInfo | null> {
    try {
      if (!this.token) {
        return null
      }

      const query = `
        query GetCurrentUser {
          me {
            id
            nickname
            avatarUrl
            phone
            wechatOpenId
          }
        }
      `

      const response = await networkManager.query<any>(query, {}, {})
      const userInfo = response.me
      this.storeAuth(this.token, userInfo)

      return userInfo
    } catch (error) {
      console.error('Failed to get current user:', error)
      // 如果获取用户信息失败，可能token已过期，清除认证状态
      this.clearStoredAuth()
      return null
    }
  }

  async updateUserInfo(userInfo: Partial<UserInfo>): Promise<UserInfo> {
    try {
      if (!this.token) {
        throw new Error('未登录')
      }

      const mutation = `
        mutation UpdateUserInfo($input: UpdateProfileInput!) {
          updateProfile(input: $input) {
            id
            nickname
            avatarUrl
            phone
            wechatOpenId
          }
        }
      `

      // 将前端的 avatarUrl 字段映射为 GraphQL 的 avatar 字段
      const graphqlInput = {
        ...userInfo,
        avatar: userInfo.avatarUrl
      }
      delete (graphqlInput as any).avatarUrl

      const response = await networkManager.mutate<any>(mutation, { input: graphqlInput }, {})
      const updatedUserInfo = response.updateProfile
      this.storeAuth(this.token, updatedUserInfo)

      return updatedUserInfo
    } catch (error) {
      console.error('Failed to update user info:', error)
      if (error instanceof GraphQLError) {
        throw new Error(error.errors?.[0]?.message || '更新用户信息失败')
      }
      if (error instanceof Error) {
        throw error;
      }
      throw new Error('网络连接失败，请检查网络设置')
    }
  }

  // 检查并刷新token
  async checkAndRefreshToken(): Promise<boolean> {
    try {
      if (!this.token) {
        return false
      }

      // 这里可以实现token刷新逻辑
      // 简化实现：尝试获取当前用户信息来验证token是否有效
      const currentUser = await this.getCurrentUser()
      return currentUser !== null
    } catch (error) {
      console.error('Token validation failed:', error)
      this.clearStoredAuth()
      return false
    }
  }

  // 强制清除所有认证数据
  forceClearAuth(): void {
    this.clearStoredAuth()

    // 清除进行中的静默登录 Promise
    this.silentLoginPromise = null

    // 额外清除其他可能的认证数据
    try {
      Taro.removeStorageSync('refresh_token')
      Taro.removeStorageSync('login_time')
      console.log('✅ 所有认证数据已强制清除')
    } catch (error) {
      console.error('Failed to force clear auth:', error)
    }
  }

  /**
   * 清除内存态与本地存储的认证数据(供网络层 401 处理调用)。
   * 与 forceClearAuth 的区别:不打断进行中的静默登录(silentLoginPromise 保留),
   * 这样并发的多个 401 会复用同一个 wx.login 流程,避免重复登录、且能接上启动时的静默登录。
   */
  clearAuthState(): void {
    try {
      this.token = null
      this.userInfo = null
      Taro.removeStorageSync('auth_token')
      Taro.removeStorageSync('user_info')
    } catch (error) {
      console.error('Failed to clear auth state:', error)
    }
  }
}

export const authService = new AuthService()

// 全局认证状态管理
export const useAuth = () => {
  const [isAuthenticated, setIsAuthenticated] = useState(() => authService.isAuthenticated())
  const [userInfo, setUserInfo] = useState(() => authService.getUserInfo())
  const [loading, setLoading] = useState(false)

  const login = useCallback(async () => {
    setLoading(true)
    try {
      const loginData = await authService.weChatLogin()
      setIsAuthenticated(true)
      setUserInfo(loginData.userInfo)
      return loginData
    } catch (error) {
      console.error('Login failed:', error)
      throw error
    } finally {
      setLoading(false)
    }
  }, [])

  const logout = useCallback(async () => {
    setLoading(true)
    try {
      await authService.logout()
      setIsAuthenticated(false)
      setUserInfo(null)
    } catch (error) {
      console.error('Logout failed:', error)
      throw error
    } finally {
      setLoading(false)
    }
  }, [])

  const refreshUserInfo = useCallback(async () => {
    setLoading(true)
    try {
      const refreshedUserInfo = await authService.getCurrentUser()
      if (refreshedUserInfo) {
        setUserInfo(refreshedUserInfo)
        setIsAuthenticated(true)
      } else {
        setUserInfo(null)
        setIsAuthenticated(false)
      }
      return refreshedUserInfo
    } catch (error) {
      console.error('Refresh user info failed:', error)
      throw error
    } finally {
      setLoading(false)
    }
  }, [])

  const clearAuth = useCallback(() => {
    authService.forceClearAuth()
    setIsAuthenticated(false)
    setUserInfo(null)
    Taro.showToast({
      title: '已清除登录信息',
      icon: 'success',
      duration: 2000
    })
  }, [])

  return {
    isAuthenticated,
    userInfo,
    loading,
    login,
    logout,
    refreshUserInfo,
    clearAuth,
    getToken: () => authService.getToken()
  }
}

// Class component auth hook wrapper
export const withAuth = (Component) => {
  return class AuthenticatedComponent extends Component {
    constructor(props) {
      super(props)
      this.state = {
        ...this.state,
        isAuthenticated: authService.isAuthenticated(),
        authLoading: false
      }
    }

    handleWeChatLogin = async () => {
      this.setState({ authLoading: true })
      try {
        const loginData = await authService.weChatLogin()
        this.setState({
          isAuthenticated: true,
          userInfo: loginData.userInfo
        })
        return loginData
      } catch (error) {
        console.error('WeChat login failed:', error)
        throw error
      } finally {
        this.setState({ authLoading: false })
      }
    }

    handleLogout = async () => {
      this.setState({ authLoading: true })
      try {
        await authService.logout()
        this.setState({
          isAuthenticated: false,
          userInfo: null
        })
      } catch (error) {
        console.error('Logout failed:', error)
        throw error
      } finally {
        this.setState({ authLoading: false })
      }
    }

    refreshAuth = async () => {
      this.setState({ authLoading: true })
      try {
        const isValid = await authService.checkAndRefreshToken()
        const currentUser = authService.getUserInfo()
        this.setState({
          isAuthenticated: isValid,
          userInfo: currentUser
        })
        return { isValid, currentUser }
      } catch (error) {
        console.error('Auth refresh failed:', error)
        this.setState({
          isAuthenticated: false,
          userInfo: null
        })
        throw error
      } finally {
        this.setState({ authLoading: false })
      }
    }

    render() {
      const authProps = {
        authService,
        isAuthenticated: this.state.isAuthenticated,
        authLoading: this.state.authLoading,
        handleWeChatLogin: this.handleWeChatLogin,
        handleLogout: this.handleLogout,
        refreshAuth: this.refreshAuth
      }

      return React.createElement(Component, { ...this.props, ...authProps })
    }
  }
}
