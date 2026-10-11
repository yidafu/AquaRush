import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import type { CompiledStyles } from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'

/** 可传入 props() 的单个样式参数;rootStyles 用它接收页面级样式(含 pageTheme) */
type StyleMixin = string | CompiledStyles | boolean | null | undefined
import { authService } from '../../utils/auth'
import SharedEmptyState from '../EmptyState'

import Taro from '@tarojs/taro'

/**
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 平台变量保留字符串引用;
 * safe-bottom 仍复用 styles/common.scss 全局定义)
 */
const styles = stylex.create({
  basePageLayout: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    position: 'relative',
  },
  loginPrompt: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '60vh',
    paddingTop: '120rpx',
    paddingRight: '64rpx',
    paddingBottom: '120rpx',
    paddingLeft: '64rpx',
    backgroundColor: 'var(--weui-BG-0)',
  },
  loginIcon: {
    width: '168rpx',
    height: '168rpx',
    borderRadius: '50%',
    backgroundColor: 'var(--weui-BRAND-100)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '72rpx',
    marginBottom: '48rpx',
  },
  loginTitle: {
    fontSize: 'var(--weui-fs-cell)',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
    marginBottom: '16rpx',
  },
  loginSubtitle: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    marginBottom: '64rpx',
  },
  loginButton: {
    width: '320rpx',
  },
  loadingContainer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '60vh',
    paddingTop: 0,
    paddingBottom: 0,
    paddingLeft: '32rpx',
    paddingRight: '32rpx',
    backgroundColor: 'var(--weui-BG-0)',
  },
  loadingText: {
    color: 'var(--weui-FG-1)',
    fontSize: 'var(--weui-fs-tips)',
  },
  errorContainer: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '60vh',
    paddingTop: '128rpx',
    paddingRight: '64rpx',
    paddingBottom: '128rpx',
    paddingLeft: '64rpx',
    backgroundColor: 'var(--weui-BG-0)',
  },
  errorIcon: {
    width: '168rpx',
    height: '168rpx',
    borderRadius: '50%',
    backgroundColor: 'var(--weui-RED-100)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '72rpx',
    marginBottom: '48rpx',
  },
  errorTitle: {
    fontSize: 'var(--weui-fs-cell)',
    fontWeight: 600,
    color: 'var(--weui-FG-0)',
    marginBottom: '16rpx',
  },
  errorMessage: {
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    marginBottom: '64rpx',
    textAlign: 'center',
  },
  errorRetryButton: {
    width: '320rpx',
  },
})

// Sub-component interfaces and implementations
interface LoginPromptProps {
  onLoginClick?: () => void
}

const LoginPrompt: React.FC<LoginPromptProps> = ({ onLoginClick }) => {
  const defaultLoginHandler = () => {
    // Default navigation to my page for login
    const navigateToLogin = async () => {
      Taro.navigateTo({
        url: '/pages/my/index'
      })
    }
    navigateToLogin()
  }

  const handleClick = onLoginClick || defaultLoginHandler

  return (
    <View {...stylex.props(styles.loginPrompt)}>
      <View {...stylex.props(styles.loginIcon)}>🔒</View>
      <Text {...stylex.props(styles.loginTitle)}>登录后查看收藏</Text>
      <Text {...stylex.props(styles.loginSubtitle)}>收藏您喜欢的商品</Text>
      <View {...props('weui-btn weui-btn_primary', styles.loginButton)} onClick={handleClick}>
        去登录
      </View>
    </View>
  )
}

interface LoadingStateProps {
  text?: string
}

const LoadingState: React.FC<LoadingStateProps> = ({ text = '加载中...' }) => {
  return (
    <View {...stylex.props(styles.loadingContainer)}>
      <Text {...stylex.props(styles.loadingText)}>{text}</Text>
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
    <View {...stylex.props(styles.errorContainer)}>
      <View {...stylex.props(styles.errorIcon)}>⚠️</View>
      <Text {...stylex.props(styles.errorTitle)}>{title}</Text>
      <Text {...stylex.props(styles.errorMessage)}>{message}</Text>
      {onRetry && (
        <View
          {...props('weui-btn weui-btn_primary', styles.errorRetryButton)}
          onClick={onRetry}
        >
          重试
        </View>
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

// 共享空态实现(见 components/EmptyState);本导出保留旧 API 兼容(subtitle → description)
export const EmptyState: React.FC<EmptyStateProps> = ({
  title = '暂无数据',
  subtitle = '',
  actionText = '去逛逛',
  onAction
}) => {
  return (
    <SharedEmptyState
      icon='❤️'
      title={title}
      description={subtitle || undefined}
      actionText={actionText}
      onAction={onAction}
    />
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
  /**
   * 页面级 StyleX 样式(含 pageTheme),合并挂到根 View(规则:页面根元素挂主题):
   *   rootStyles={[pageTheme, styles.page]}
   */
  rootStyles?: StyleMixin | ReadonlyArray<StyleMixin>
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
  rootStyles,
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
    <View {...props(styles.basePageLayout, rootStyles, className)}>
      {children}
      {safeArea && <View className='safe-bottom' />}
    </View>
  )
}

export default BasePageLayout
