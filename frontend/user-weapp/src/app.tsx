import { PropsWithChildren, useEffect } from 'react'
import { useDidShow, useDidHide } from '@tarojs/taro'
import { authService } from './utils/auth'
import { userActionTracker } from './utils/tracking'
import { testConfigurationInDevelopment } from './config/config-test'
import { connectWsPushIfLoggedIn } from './services/wsPush'

import './app.scss'

function App({ children }: PropsWithChildren) {
  // 执行静默登录
  const performSilentLogin = async () => {
    try {
      // 尝试静默登录
      const success = await authService.silentLogin()
      if (success) {
        console.log('Silent login successful')
        // 登录成功:补发登录前暂存的操作日志
        void userActionTracker.flush()
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

    // 静默登录
    initSilentLogin()
    // 已登录用户建立 WebSocket 实时推送连接(F-NTF-003;未登录时跳过,登录页/我的页会再连)
    void connectWsPushIfLoggedIn()
  }, [])

  // 对应 onShow
  useDidShow(() => {
    // 应用显示时检查登录状态
    checkLoginStatus()
    // 应用回前台时确保 WebSocket 推送连接可用
    void connectWsPushIfLoggedIn()
  })

  // 对应 onHide
  useDidHide(() => {
    // 应用切后台时立即上报缓冲队列,避免事件滞留丢失
    void userActionTracker.flush()
  })

  return (
    <>
      {/* props.children 是将要被渲染的页面 */}
      {children}
    </>
  )
}

export default App
