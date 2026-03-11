import { useState, useCallback } from 'react'
import Taro from '@tarojs/taro'
import { deliveryAuthService, WorkerInfo, DeliveryLoginResponse } from '../services/auth'

export interface UseAuthOptions {}

export interface UseAuthReturn {
  isAuthenticated: boolean
  workerInfo: WorkerInfo | null
  loading: boolean
  needBindPhone: boolean
  login: () => Promise<DeliveryLoginResponse>
  logout: () => Promise<void>
  silentLogin: () => Promise<boolean>
  refreshWorkerInfo: () => void
}

export const useAuth = (_options: UseAuthOptions = {}): UseAuthReturn => {
  const [isAuthenticated, setIsAuthenticated] = useState(() => deliveryAuthService.isAuthenticated())
  const [workerInfo, setWorkerInfo] = useState<WorkerInfo | null>(() => deliveryAuthService.getWorkerInfo())
  const [loading, setLoading] = useState(false)
  const [needBindPhone, setNeedBindPhone] = useState(false)

  const login = useCallback(async () => {
    setLoading(true)
    try {
      const loginData = await deliveryAuthService.weChatLogin()
      console.log('loginData:', loginData)
      console.log('needBindPhone:', loginData.needBindPhone)
      if (loginData.needBindPhone) {
        console.log('redirecting to bind-phone page')
        if (loginData.openId) {
          deliveryAuthService.setOpenId(loginData.openId)
        }
        setNeedBindPhone(true)
        // 直接跳转到绑定手机页面
        const result = Taro.redirectTo({ url: '/pages/bind-phone/index' })
        console.log('redirectTo result:', result)
        return loginData
      }

      setIsAuthenticated(true)
      setWorkerInfo(loginData.workerInfo)
      setNeedBindPhone(false)
      // 登录成功，跳转到首页
      Taro.switchTab({ url: '/pages/order-list/index' })
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
      await deliveryAuthService.logout()
      setIsAuthenticated(false)
      setWorkerInfo(null)
      setNeedBindPhone(false)
      // 登出成功，跳转到登录页
      Taro.redirectTo({ url: '/pages/login/index' })
    } finally {
      setLoading(false)
    }
  }, [])

  const silentLogin = useCallback(async () => {
    setLoading(true)
    try {
      const success = await deliveryAuthService.silentLogin()
      if (success) {
        const info = deliveryAuthService.getWorkerInfo()
        setIsAuthenticated(true)
        setWorkerInfo(info)
        if (!info) {
          // 需要绑定手机
          setNeedBindPhone(true)
          Taro.redirectTo({ url: '/pages/bind-phone/index' })
        } else {
          setNeedBindPhone(false)
          Taro.switchTab({ url: '/pages/order-list/index' })
        }
      }
      return success
    } finally {
      setLoading(false)
    }
  }, [])

  const refreshWorkerInfo = useCallback(() => {
    const info = deliveryAuthService.getWorkerInfo()
    setWorkerInfo(info)
    setIsAuthenticated(!!info)
    setNeedBindPhone(false)
  }, [])

  return {
    isAuthenticated,
    workerInfo,
    loading,
    needBindPhone,
    login,
    logout,
    silentLogin,
    refreshWorkerInfo
  }
}
