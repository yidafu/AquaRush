import { PropsWithChildren, useEffect } from 'react'
import { useDidShow, useDidHide } from '@tarojs/taro'
import { ThemeProvider } from './components/ThemeProvider'
import { authService } from './utils/auth'
import { testConfigurationInDevelopment } from './config/config-test'

import './app.scss'

function App({ children }: PropsWithChildren) {
  // 初始化主题
  const initAppTheme = () => {
    // 可以在这里进行主题的初始化设置
    // 例如根据系统主题、用户偏好等设置默认主题
    try {
      const hour = new Date().getHours()
      // 根据时间自动切换主题（可选）
      if (hour >= 18 || hour < 6) {
        // 晚上可以使用更柔和的颜色
        // setTheme('purple')
      }
    } catch (error) {
      console.warn('Failed to initialize app theme:', error)
    }
  }

  // 执行静默登录
  const performSilentLogin = async () => {
    try {
      // 尝试静默登录
      const success = await authService.silentLogin()
      if (success) {
        console.log('Silent login successful')
      } else {
        console.log('Silent login failed')
      }
    } catch (error) {
      console.warn('Silent login attempt failed:', error)
      // 静默登录失败，不抛出错误，让用户在需要时手动登录
    }
  }

  // 初始化静默登录
  const initSilentLogin = async () => {
    try {
      // 检查是否已经登录
      if (authService.isAuthenticated()) {
        console.log('User already authenticated, checking token validity...')
        // 验证token是否仍然有效
        const isValid = await authService.checkAndRefreshToken()
        if (!isValid) {
          console.log('Token invalid, attempting silent login...')
          await performSilentLogin()
        }
      } else {
        console.log('User not authenticated, attempting silent login...')
        await performSilentLogin()
      }
    } catch (error) {
      console.warn('Silent login failed:', error)
      // 静默登录失败时不显示错误提示，让用户手动登录
    }
  }

  // 检查登录状态
  const checkLoginStatus = async () => {
    try {
      // 检查当前登录状态
      if (authService.isAuthenticated()) {
        // 验证token有效性
        const isValid = await authService.checkAndRefreshToken()
        if (!isValid) {
          console.log('Token expired, attempting silent refresh...')
          await performSilentLogin()
        }
      }
    } catch (error) {
      console.warn('Login status check failed:', error)
    }
  }

  // 对应 componentDidMount
  useEffect(() => {
    // 验证API配置（仅在开发环境）
    testConfigurationInDevelopment()

    // 初始化主题
    initAppTheme()
    // 静默登录
    initSilentLogin()
  }, [])

  // 对应 onShow
  useDidShow(() => {
    // 应用显示时检查登录状态
    checkLoginStatus()
  })

  // 对应 onHide
  useDidHide(() => {
    // 应用隐藏时的处理逻辑（如果需要）
  })

  return (
    <ThemeProvider defaultTheme="aqua">
      {/* props.children 是将要被渲染的页面 */}
      {children}
    </ThemeProvider>
  )
}

export default App
