import Taro from '@tarojs/taro'
import { useState, useCallback } from 'react'

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
    phone: string
    balance: number
    points: number
    level: string
    isVip: boolean
    vipExpireTime?: string
  }
}

export interface UserInfo {
  id: string
  nickname: string
  avatarUrl: string
  phone: string
  balance: number
  points: number
  level: string
  isVip: boolean
  vipExpireTime?: string
}

const apiBaseUrl = 'http://localhost:8080';
console.log('import API_BASE_URL', apiBaseUrl)
console.log('API_BASE_URL', Taro.env)
class AuthService {
  private token: string | null = null
  private userInfo: UserInfo | null = null

  constructor() {
    this.loadStoredAuth()
  }

  private loadStoredAuth() {
    try {
      this.token = Taro.getStorageSync('auth_token')
      const storedUserInfo = Taro.getStorageSync('user_info')
      if (storedUserInfo) {
        this.userInfo = JSON.parse(storedUserInfo)
      }
    } catch (error) {
      console.error('Failed to load stored auth:', error)
    }
  }

  private storeAuth(token: string, userInfo: UserInfo) {
    try {
      this.token = token
      this.userInfo = userInfo
      Taro.setStorageSync('auth_token', token)
      Taro.setStorageSync('user_info', JSON.stringify(userInfo))
    } catch (error) {
      console.error('Failed to store auth:', error)
    }
  }

  private clearStoredAuth() {
    try {
      this.token = null
      this.userInfo = null
      Taro.removeStorageSync('auth_token')
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

  private async getWeChatCode(): Promise<WeChatAuthResponse> {
    return new Promise((resolve, reject) => {
      Taro.login({
        success: resolve,
        fail: reject
      })
    })
  }

  private async callLoginAPI(code: string): Promise<LoginResponse> {
    const response = await Taro.request({
      url: `${apiBaseUrl}/graphql`,
      method: 'POST',
      header: {
        'Content-Type': 'application/json'
      },
      data: {
        query: `mutation WechatLogin($code: String!) {
  wechatLogin(input:  {
    code: $code
  }) {
    accessToken
    refreshToken
    expiresIn
    userInfo {
      id
      wechatOpenId
      nickname
      avatarUrl
    }
  }
}`,
        variables: {
          code
        }
      }
    })

    if (response.statusCode !== 200 || response.data.errors) {
      const errorMsg = response.data.errors?.[0]?.message || '登录失败'
      throw new Error(errorMsg)
    }

    // 转换响应格式以匹配我们的接口
    const weChatLoginResult = response.data.data.weChatLogin
    return {
      token: weChatLoginResult.token,
      refreshToken: weChatLoginResult.refreshToken,
      userInfo: {
        id: weChatLoginResult.user.id,
        nickname: weChatLoginResult.user.nickname,
        avatarUrl: weChatLoginResult.user.avatarUrl,
        phone: weChatLoginResult.user.phone,
        balance: weChatLoginResult.user.balance,
        points: weChatLoginResult.user.points,
        level: weChatLoginResult.user.level,
        isVip: weChatLoginResult.user.isVip,
        vipExpireTime: weChatLoginResult.user.vipExpireTime
      }
    }
  }

  async logout(): Promise<void> {
    try {
      // 调用后端登出接口（可选）
      if (this.token) {
        await Taro.request({
          url: `${apiBaseUrl}/graphql`,
          method: 'POST',
          header: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${this.token}`
          },
          data: {
            query: `
              mutation Logout {
                logout
              }
            `
          }
        })
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

      const response = await Taro.request({
        url: `${apiBaseUrl}/graphql`,
        method: 'POST',
        header: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.token}`
        },
        data: {
          query: `
            query GetCurrentUser {
              me {
                id
                nickname
                avatarUrl
                phone
                balance
                points
                level
                isVip
                vipExpireTime
              }
            }
          `
        }
      })

      if (response.statusCode !== 200 || response.data.errors) {
        throw new Error(response.data.errors?.[0]?.message || '获取用户信息失败')
      }

      const userInfo = response.data.data.me
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

      const response = await Taro.request({
        url: `${apiBaseUrl}/graphql`,
        method: 'POST',
        header: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${this.token}`
        },
        data: {
          query: `
            mutation UpdateUserInfo($input: UserUpdateInput!) {
              updateUser(input: $input) {
                id
                nickname
                avatarUrl
                phone
                balance
                points
                level
                isVip
                vipExpireTime
              }
            }
          `,
          variables: {
            input: userInfo
          }
        }
      })

      if (response.statusCode !== 200 || response.data.errors) {
        throw new Error(response.data.errors?.[0]?.message || '更新用户信息失败')
      }

      const updatedUserInfo = response.data.data.updateUser
      this.storeAuth(this.token, updatedUserInfo)

      return updatedUserInfo
    } catch (error) {
      console.error('Failed to update user info:', error)
      throw error
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

  return {
    isAuthenticated,
    userInfo,
    loading,
    login,
    logout,
    refreshUserInfo,
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
