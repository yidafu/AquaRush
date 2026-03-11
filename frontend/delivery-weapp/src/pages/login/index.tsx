import React, { useState, useEffect } from 'react'
import { View, Text, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { useAuth } from '../../hooks/useAuth'
import { deliveryAuthService } from '../../services/auth'
import './index.scss'

const LoginPage: React.FC = () => {
  const { login, needBindPhone } = useAuth()
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')
  const [checkingAuth, setCheckingAuth] = useState(true)

  // Check auth status on page load
  useEffect(() => {
    const checkAuth = async () => {
      Taro.showLoading({ title: '检查登录态中...' })
      try {
        const result = await deliveryAuthService.checkAuthStatus()
        if (result && result.workerInfo) {
          // Auth valid, redirect to home
          Taro.switchTab({ url: '/pages/order-list/index' })
          return
        }
      } catch (error) {
        console.log('Auth check failed, staying on login page')
      } finally {
        Taro.hideLoading()
        setCheckingAuth(false)
      }
    }

    checkAuth()
  }, [])

  // Redirect to bind phone when needed (for silentLogin from other scenarios)
  useEffect(() => {
    if (needBindPhone) {
      Taro.navigateTo({ url: '/pages/bind-phone/index' })
    }
  }, [needBindPhone])

  const handleWeChatLogin = async () => {
    try {
      setLoading(true)
      setError('')

      await login()

      // login() handles redirect internally
    } catch (err: any) {
      console.error('Login failed:', err)
      setError(err.message || '登录失败，请重试')
    } finally {
      setLoading(false)
    }
  }

  return (
    <View className='login-page'>
      <View className='login-container'>
        <View className='logo-section'>
          <Text className='app-name'>AquaRush</Text>
          <Text className='app-subtitle'>送水员管理端</Text>
        </View>

        <View className='login-content'>
          {!checkingAuth && (
            <>
              <View className='welcome-text'>
                <Text>欢迎使用</Text>
                <Text className='highlight'>送水管理系统</Text>
              </View>

              {error && (
                <View className='error-message'>
                  <Text>{error}</Text>
                </View>
              )}

              <Button
                className='wechat-login-btn'
                type='primary'
                loading={loading}
                onClick={handleWeChatLogin}
              >
                <Text>{loading ? '登录中...' : '微信授权登录'}</Text>
              </Button>

              <View className='login-tips'>
                <Text>登录即表示同意</Text>
                <Text className='link'>《用户协议》</Text>
                <Text>和</Text>
                <Text className='link'>《隐私政策》</Text>
              </View>
            </>
          )}
        </View>
      </View>
    </View>
  )
}

export default LoginPage
