import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton } from 'taro-ui'
import { authService } from '../../utils/auth'

import "taro-ui/dist/style/components/button.scss"
import './index.scss'
import Taro from '@tarojs/taro'

// Sub-component interfaces and implementations
interface LoginPromptProps {
  onLoginClick?: () => void
}

const LoginPrompt: React.FC<LoginPromptProps> = ({ onLoginClick }) => {
  const defaultLoginHandler = () => {
    // Default navigation to my page for login
    const navigateToLogin = async () => {
      // const Taro = (await import('@tarojs/taro')).default
      Taro.navigateTo({
        url: '/pages/my/index'
      })
    }
    navigateToLogin()
  }

  const handleClick = onLoginClick || defaultLoginHandler

  return (
    <View className='login-prompt'>
      <View className='login-icon'>🔒</View>
      <Text className='login-title'>登录后查看收藏</Text>
      <Text className='login-subtitle'>收藏您喜欢的商品</Text>
      <AtButton
        type='primary'
        size='normal'
        onClick={handleClick}
        className='login-button'
      >
        去登录
      </AtButton>
    </View>
  )
}

interface LoadingStateProps {
  text?: string
}

const LoadingState: React.FC<LoadingStateProps> = ({ text = '加载中...' }) => {
  return (
    <View className='loading-container'>
      <Text className='loading-text'>{text}</Text>
    </View>
  )
}

interface ErrorStateProps {
  title?: string
  message: string
  onRetry?: () => void
}

const ErrorState: React.FC<ErrorStateProps> = ({ title = '加载失败', message, onRetry }) => {
  return (
    <View className='error-container'>
      <View className='error-icon'>⚠️</View>
      <Text className='error-title'>{title}</Text>
      <Text className='error-message'>{message}</Text>
      {onRetry && (
        <AtButton
          type='primary'
          size='normal'
          onClick={onRetry}
          className='error-retry-button'
        >
          重试
        </AtButton>
      )}
    </View>
  )
}

interface EmptyStateProps {
  title?: string
  subtitle?: string
  actionText?: string
  onAction?: () => void
}

const EmptyState: React.FC<EmptyStateProps> = ({
  title = '暂无数据',
  subtitle = '',
  actionText = '去逛逛',
  onAction
}) => {
  return (
    <View className='empty-container'>
      <View className='empty-icon'>❤️</View>
      <Text className='empty-title'>{title}</Text>
      {subtitle && <Text className='empty-subtitle'>{subtitle}</Text>}
      {onAction && (
        <AtButton
          type='secondary'
          size='normal'
          onClick={onAction}
          className='empty-action-button'
        >
          {actionText}
        </AtButton>
      )}
    </View>
  )
}

// Main BasePageLayout component
export interface BasePageLayoutProps {
  // Authentication
  requireAuth?: boolean
  onLoginClick?: () => void

  // Loading States
  loading?: boolean
  loadingText?: string

  // Error States
  error?: string | null
  errorTitle?: string
  onRetry?: () => void

  // Empty States
  empty?: boolean
  emptyTitle?: string
  emptySubtitle?: string
  emptyActionText?: string
  onEmptyAction?: () => void

  // Layout
  children: React.ReactNode
  className?: string
  safeArea?: boolean
}

const BasePageLayout: React.FC<BasePageLayoutProps> = ({
  requireAuth = false,
  onLoginClick,
  loading = false,
  loadingText = '加载中...',
  error = null,
  errorTitle = '加载失败',
  onRetry,
  empty = false,
  emptyTitle = '暂无数据',
  emptySubtitle = '',
  emptyActionText = '去逛逛',
  onEmptyAction,
  children,
  className = '',
  safeArea = true
}) => {
  // Authentication check
  if (requireAuth && !authService.isAuthenticated()) {
    return <LoginPrompt onLoginClick={onLoginClick} />
  }

  // Loading state
  if (loading) {
    return <LoadingState text={loadingText} />
  }

  // Error state
  if (error) {
    return <ErrorState title={errorTitle} message={error} onRetry={onRetry} />
  }

  // Empty state
  if (empty) {
    return (
      <EmptyState
        title={emptyTitle}
        subtitle={emptySubtitle}
        actionText={emptyActionText}
        onAction={onEmptyAction}
      />
    )
  }

  // Normal content
  return (
    <View className={`base-page-layout ${className}`}>
      {children}
      {safeArea && <View className='safe-bottom' />}
    </View>
  )
}

export default BasePageLayout
