import React, { ReactNode, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import { useAuth } from '../../hooks/useAuth'
import './index.scss'
import Taro from '@tarojs/taro'

interface PageContainerProps {
  /** 页面标题 */
  title?: string
  /** 是否需要登录检查，默认 true */
  requireAuth?: boolean
  /** 登录页路径，默认 /pages/login/index */
  loginPath?: string
  /** 子组件 */
  children: ReactNode
}

export const PageContainer: React.FC<PageContainerProps> = ({
  title,
  requireAuth = true,
  loginPath = '/pages/login/index',
  children,
}) => {
  const { workerInfo, isAuthenticated, loading: authLoading } = useAuth()

  // 设置页面标题
  useEffect(() => {
    if (title) {
      Taro.setNavigationBarTitle({ title })
    }
  }, [title])

  // 登录检查
  useEffect(() => {
    if (requireAuth && !authLoading) {
      if (!isAuthenticated || !workerInfo) {
        // 未登录，跳转到登录页
        Taro.redirectTo({ url: loginPath })
      }
    }
  }, [requireAuth, authLoading, isAuthenticated, workerInfo, loginPath])

  // 加载中状态
  if (authLoading) {
    return (
      <View className='page-container'>
        <View className='page-loading'>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  // 未登录状态（跳转前短暂显示）
  if (requireAuth && (!isAuthenticated || !workerInfo)) {
    return (
      <View className='page-container'>
        <View className='page-unauthorized'>
          <Text>请先登录</Text>
        </View>
      </View>
    )
  }

  return (
    <View className='page-container'>
      {children}
    </View>
  )
}

