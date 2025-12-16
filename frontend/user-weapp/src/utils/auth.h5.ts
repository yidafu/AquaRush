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
    wechatOpenId: string
  }
}

export interface UserInfo {
  id: string
  nickname: string
  avatarUrl: string
  phone: string
  wechatOpenId: string;
}

// Mock 数据
const MOCK_USER: UserInfo = {
  id: 'mock_h5_user_1',
  nickname: 'H5测试用户',
  avatarUrl: 'https://via.placeholder.com/100x100?text=H5用户',
  phone: '138****8888',
  wechatOpenId: 'mock_h5_openid_12345'
}

const MOCK_TOKEN = 'mock_h5_token_12345'
const MOCK_REFRESH_TOKEN = 'mock_h5_refresh_token_12345'

console.log('🌐 [H5] 使用Mock认证实现')

class AuthService {
  private token: string | null = MOCK_TOKEN
  private userInfo: UserInfo | null = MOCK_USER

  constructor() {
    // 直接设置为已登录状态
  }

  private storeAuth(token: string, userInfo: UserInfo) {
    this.token = token
    this.userInfo = userInfo
  }

  private clearStoredAuth() {
    this.token = null
    this.userInfo = null
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
    console.log('🔐 [H5] Mock微信登录 - 直接返回成功')

    const mockResponse: LoginResponse = {
      token: MOCK_TOKEN,
      refreshToken: MOCK_REFRESH_TOKEN,
      userInfo: MOCK_USER
    }

    this.storeAuth(mockResponse.token, MOCK_USER)
    return mockResponse
  }

  async silentLogin(): Promise<boolean> {
    console.log('🔐 [H5] Mock静默登录 - 直接返回成功')
    return true
  }

  async getWeChatCode(): Promise<WeChatAuthResponse> {
    return {
      code: 'mock_code_12345',
      errMsg: 'login:ok'
    }
  }

  async callLoginAPI(code: string): Promise<LoginResponse> {
    console.log('🌐 [H5] Mock登录API调用，code:', code)
    return {
      token: MOCK_TOKEN,
      refreshToken: MOCK_REFRESH_TOKEN,
      userInfo: MOCK_USER
    }
  }

  async logout(): Promise<void> {
    console.log('🚪 [H5] Mock登出')
    this.clearStoredAuth()
  }

  async getCurrentUser(): Promise<UserInfo | null> {
    console.log('👤 [H5] Mock获取当前用户')
    return this.userInfo
  }

  async updateUserInfo(userInfo: Partial<UserInfo>): Promise<UserInfo> {
    console.log('✏️ [H5] Mock更新用户信息:', userInfo)
    const updatedUserInfo: UserInfo = {
      ...this.userInfo!,
      ...userInfo
    }
    this.storeAuth(this.token!, updatedUserInfo)
    return updatedUserInfo
  }

  async checkAndRefreshToken(): Promise<boolean> {
    console.log('🔄 [H5] Mock检查Token')
    return this.isAuthenticated()
  }

  forceClearAuth(): void {
    console.log('🧹 [H5] Mock清除认证数据')
    this.clearStoredAuth()
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
export const withAuth = (Component: any) => {
  return class AuthenticatedComponent extends Component {
    constructor(props: any) {
      super(props)
      this.state = {
        ...this.state,
        isAuthenticated: authService.isAuthenticated(),
        authLoading: false,
        userInfo: authService.getUserInfo()
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
        userInfo: this.state.userInfo,
        handleWeChatLogin: this.handleWeChatLogin,
        handleLogout: this.handleLogout,
        refreshAuth: this.refreshAuth
      }

      return React.createElement(Component, { ...this.props, ...authProps })
    }
  }
}