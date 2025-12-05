import { Component, PropsWithChildren } from 'react'
import { View, Text } from '@tarojs/components'
import { authService } from '../../utils/auth'
import './index.scss'

interface AuthGuardProps {
  fallback?: React.ReactNode
  requireAuth?: boolean
}

interface AuthGuardState {
  isAuthenticated: boolean
  loading: boolean
}

class AuthGuard extends Component<PropsWithChildren<AuthGuardProps>, AuthGuardState> {
  constructor(props: PropsWithChildren<AuthGuardProps>) {
    super(props)
    this.state = {
      isAuthenticated: authService.isAuthenticated(),
      loading: false
    }
  }

  componentDidMount() {
    this.checkAuthStatus()
  }

  checkAuthStatus = async () => {
    this.setState({ loading: true })
    try {
      if (authService.isAuthenticated()) {
        // 验证token是否仍然有效
        const isValid = await authService.checkAndRefreshToken()
        this.setState({
          isAuthenticated: isValid,
          loading: false
        })
      } else {
        this.setState({
          isAuthenticated: false,
          loading: false
        })
      }
    } catch (error) {
      console.error('Auth check failed:', error)
      this.setState({
        isAuthenticated: false,
        loading: false
      })
    }
  }

  render() {
    const { children, fallback, requireAuth = true } = this.props
    const { isAuthenticated, loading } = this.state

    if (loading) {
      return (
        <View className='auth-guard-loading'>
          <Text>加载中...</Text>
        </View>
      )
    }

    // 如果不需要认证，直接渲染子组件
    if (!requireAuth) {
      return children
    }

    // 如果需要认证但用户未登录，显示fallback
    if (!isAuthenticated && fallback) {
      return fallback
    }

    // 如果需要认证但用户未登录且没有fallback，渲染子组件（让组件自己处理未登录状态）
    return children
  }
}

export default AuthGuard