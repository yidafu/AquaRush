import React, { useState, useEffect } from 'react'
import { View, Text } from '@tarojs/components'
import { AtToast } from 'taro-ui'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { pageTheme } from '../../styles/theme.stylex'
import AvatarNicknameForm from './components/AvatarNicknameForm'
import UserSection from './components/UserSection'
import AssetsEntry from './components/AssetsEntry'
import OrderSection from './components/OrderSection'
import ServiceSection from './components/ServiceSection'
import Taro, { useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { authService, type UserInfo as AuthUserInfo } from '../../utils/auth'
import { userActionTracker } from '../../utils/tracking'
import { usePageViewTracking } from '../../hooks/usePageViewTracking'
import NotificationService from '../../services/NotificationService'
import OrderService from '../../services/OrderService'
import { connectWsPushIfLoggedIn, wsPushService } from '../../services/wsPush'
import { PENDING_ORDER_TAB_STORAGE_KEY } from '../order-list/components/constants'

// Taro UI 样式已在 app.scss 中全局引入，无需重复引入

interface OrderStats {
  pendingPayment: number
  pendingDelivery: number
  delivering: number
  completed: number
  afterSales: number
}

/**
 * 我的页(spec/design/user-weapp/05-user/my.html)
 * 布局:渐变头部 → 「我的资产」单 cell → 「我的订单」panel+五宫格
 *      → 「常用服务」weui-cells 8 行 → 退出登录(语义红)
 * 页面底 var(--weui-BG-0);区块样式在各 components/ 子组件
 *
 * 样式:StyleX(原 index.scss 迁移;750 基准 rpx,weui-* 类保留字符串引用;
 * 原 .logout-cells .logout-text 后代选择器拍平到 Text)
 */
const styles = stylex.create({
  page: {
    minHeight: '100vh',
    backgroundColor: 'var(--weui-BG-0)',
    paddingBottom: tokens.s5, // 原 var(--spacing-xl) = 24px
  },
  loadingContainer: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    minHeight: '50vh',
    color: 'var(--weui-FG-1)',
    fontSize: '26rpx',
  },
  // 退出登录(weui-cell 形态,语义红 my.html 状态一)
  logoutCells: {
    marginTop: '24rpx',
  },
  logoutText: {
    color: 'var(--weui-RED)',
  },
})

const MyPage: React.FC = () => {
  usePageViewTracking()

  // State management
  const [userInfo, setUserInfo] = useState<AuthUserInfo | null>(null)
  const [orderStats, setOrderStats] = useState<OrderStats>({
    pendingPayment: 0,
    pendingDelivery: 0,
    delivering: 0,
    completed: 0,
    afterSales: 0
  })
  const [loading, setLoading] = useState<boolean>(true)
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  const [showAvatarNicknameForm, setShowAvatarNicknameForm] = useState<boolean>(false)
  const [unreadCount, setUnreadCount] = useState<number>(0)

  // Lifecycle hooks
  useEffect(() => {
    // 对应 componentDidMount
    loadUserInfo()
    loadOrderStats()
  }, [])

  useDidShow(() => {
    // 页面显示时刷新用户信息
    loadUserInfo()
    loadOrderStats()

    // 检查并刷新认证状态
    checkAuthStatus()

    // 消息中心未读角标(F-NTF-002)
    loadUnreadCount()

    // 登录态下建立 WebSocket 实时推送连接(F-NTF-003)
    if (authService.isAuthenticated()) {
      connectWsPushIfLoggedIn()
    }
  })

  usePullDownRefresh(() => {
    // 下拉刷新
    Promise.all([
      loadUserInfo(),
      loadOrderStats()
    ]).finally(() => {
      Taro.stopPullDownRefresh()
    })
  })

  // Toast functions
  const showToastMessage = (text: string, type: 'success' | 'error' | 'loading' = 'success'): void => {
    setShowToast(true)
    setToastText(text)
    setToastType(type)
  }

  const hideToast = (): void => {
    setShowToast(false)
  }

  const checkAuthStatus = async (): Promise<void> => {
    try {
      // 检查是否有有效的认证状态
      if (authService.isAuthenticated()) {
        const currentUser = authService.getUserInfo()
        if (currentUser && currentUser.id) {
          setUserInfo(currentUser)
        }
      } else {
        // 如果没有认证，保持游客状态
        if (userInfo?.id) {
          // 如果之前有用户信息但现在没有认证，清除状态
          setUserInfo({
            id: '',
            nickname: '游客',
            avatarUrl: '',
            phone: '',
            wechatOpenId: ''
          })
          setOrderStats({
            pendingPayment: 0,
            pendingDelivery: 0,
            delivering: 0,
            completed: 0,
            afterSales: 0
          })
        }
      }
    } catch (error) {
      console.error('检查认证状态失败:', error)
    }
  }

  const loadUserInfo = async (): Promise<void> => {
    try {
      // 检查是否已登录
      if (authService.isAuthenticated()) {
        const storedUserInfo = authService.getUserInfo()
        if (storedUserInfo) {
          setUserInfo(storedUserInfo)
          return
        }
      }

      // 尝试从服务器获取当前用户信息
      const currentUser = await authService.getCurrentUser()
      if (currentUser) {
        setUserInfo(currentUser)
      } else {
        // 设置游客用户信息
        setUserInfo({
          id: '',
          nickname: '游客',
          avatarUrl: '',
          phone: '',
          wechatOpenId: ''
        })
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)

      // 设置默认用户信息
      setUserInfo({
        id: '',
        nickname: '游客',
        avatarUrl: '',
        phone: '',
        wechatOpenId: ''
      })
    }
  }

  const loadOrderStats = async (): Promise<void> => {
    // 游客态:无真实订单统计,角标全 0 且不发起需登录的请求(F-USR-002,AC-USR-002-02)
    if (!authService.isAuthenticated()) {
      setOrderStats({
        pendingPayment: 0,
        pendingDelivery: 0,
        delivering: 0,
        completed: 0,
        afterSales: 0
      })
      setLoading(false)
      return
    }
    try {
      // 真实订单统计(G31):myOrderStatistics 单请求返回当前用户各状态订单数;
      // 待配送口径 = 待派单 + 待配送(与 order-list「待配送」Tab 一致);售后 = 已退款
      const result = await OrderService.getInstance().getMyOrderStatistics()
      if (!result.success || !result.data) {
        throw new Error(result.error?.message || '获取订单统计失败')
      }
      const stats = result.data
      setOrderStats({
        pendingPayment: stats.pendingPayment,
        pendingDelivery: stats.pendingDispatch + stats.pendingDelivery,
        delivering: stats.delivering,
        completed: stats.completed,
        afterSales: stats.refunded
      })
    } catch (error) {
      console.error('获取订单统计失败:', error)
      setOrderStats({
        pendingPayment: 0,
        pendingDelivery: 0,
        delivering: 0,
        completed: 0,
        afterSales: 0
      })
    } finally {
      setLoading(false)
    }
  }

  const loadUnreadCount = async (): Promise<void> => {
    if (!authService.isAuthenticated()) {
      setUnreadCount(0)
      return
    }
    try {
      const count = await NotificationService.getUnreadCount()
      setUnreadCount(count)
    } catch (e) {
      console.error('获取未读消息数失败:', e)
    }
  }

  // WebSocket 推送实时更新未读角标
  useEffect(() => {
    const unsubscribe = wsPushService.onMessage((message) => {
      if (typeof message.unreadCount === 'number') {
        setUnreadCount(message.unreadCount)
      } else {
        loadUnreadCount()
      }
    })
    return unsubscribe
  }, [])

  const handleProfileEdit = (): void => {
    // 如果用户未登录，触发微信登录
    if (!authService.isAuthenticated()) {
      handleWeChatLogin()
    } else {
      // 显示头像昵称填写表单
      setShowAvatarNicknameForm(true)
    }
  }

  // 关闭头像昵称表单
  const handleCloseAvatarNicknameForm = (): void => {
    setShowAvatarNicknameForm(false)
  }

  // 提交头像昵称更新
  const handleSubmitAvatarNickname = async (data: { avatarUrl: string; nickname: string }): Promise<void> => {
    try {
      // 使用 authService 的 updateUserInfo 方法
      const updatedUserInfo = await authService.updateUserInfo({
        avatarUrl: data.avatarUrl,
        nickname: data.nickname
      })

      // 更新页面状态
      setUserInfo(updatedUserInfo)

      showToastMessage('个人资料更新成功', 'success')
    } catch (error) {
      console.error('更新个人资料失败:', error)
      const errorMsg = error instanceof Error ? error.message : '更新失败'
      throw new Error(errorMsg)
    }
  }

  const handleWeChatLogin = async (): Promise<void> => {
    try {
      showToastMessage('正在登录...', 'loading')

      const loginData = await authService.weChatLogin()

      setUserInfo({
        ...loginData.userInfo,
        phone: ''
      })
      showToastMessage('登录成功', 'success')

      // 重新加载订单统计
      loadOrderStats()
    } catch (error) {
      console.error('微信登录失败:', error)
      const errorMsg = error instanceof Error ? error.message : '登录失败'
      showToastMessage(errorMsg, 'error')
    }
  }

  // 订单宫格入口 → order-list。order-list 是 tabBar 页,switchTab 不能携带
  // 查询参数,目标 tab 经 storage key 接力(order-list 读后即清)。
  const handleOrderNavigation = (type: string): void => {
    userActionTracker.track('CLICK', 'my-order-grid', {
      elementType: 'grid',
      elementText: type,
      properties: { orderTab: type }
    })
    Taro.setStorageSync(PENDING_ORDER_TAB_STORAGE_KEY, type)
    Taro.switchTab({
      url: '/pages/order-list/index'
    })
  }

  const handleLogout = (): void => {
    Taro.showModal({
      title: '退出登录',
      content: '确定要退出登录吗？',
      confirmText: '退出',
      confirmColor: '#FF4D4F',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await authService.logout()
          setUserInfo({
            id: '',
            nickname: '游客',
            avatarUrl: '',
            phone: '',
            wechatOpenId: ''
          })
          setOrderStats({
            pendingPayment: 0,
            pendingDelivery: 0,
            delivering: 0,
            completed: 0,
            afterSales: 0
          })
          showToastMessage('已退出登录', 'success')
        } catch (error) {
          console.error('退出登录失败:', error)
          showToastMessage('退出登录失败', 'error')
        }
      }
    })
  }

  // Return JSX directly
  if (loading) {
    return (
      <View {...stylex.props(pageTheme, styles.page)}>
        <View {...stylex.props(styles.loadingContainer)}>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View {...stylex.props(pageTheme, styles.page)}>
      {/* 渐变头部(头像/昵称/手机号,点击弹头像昵称表单或触发登录) */}
      <UserSection
        userInfo={userInfo}
        onProfileEdit={handleProfileEdit}
      />

      {/* 「我的资产」聚合入口(单 cell → assets 聚合页) */}
      <AssetsEntry />

      {/* 「我的订单」(.weui-panel + .weui-grids 五列) */}
      <OrderSection
        orderStats={orderStats}
        onOrderNavigation={handleOrderNavigation}
      />

      {/* 「常用服务」(8 行 weui-cells) */}
      <ServiceSection unreadCount={unreadCount} />

      {/* 退出登录(语义红,二次确认) */}
      {authService.isAuthenticated() && (
        <View {...props('weui-cells', styles.logoutCells)}>
          <View
            className='weui-cell'
            trackTarget='my-logout'
            trackElementType='cell'
            trackElementText='退出登录'
            onClick={handleLogout}
          >
            <View className='weui-cell__bd'>
              <Text {...stylex.props(styles.logoutText)}>退出登录</Text>
            </View>
            <View className='weui-cell__ft weui-cell__ft_in-access' />
          </View>
        </View>
      )}

      {/* 底部安全区域 */}
      <View className='safe-bottom' />

      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={hideToast}
      />

      {/* 头像昵称填写表单 */}
      <AvatarNicknameForm
        visible={showAvatarNicknameForm}
        onClose={handleCloseAvatarNicknameForm}
        onSubmit={handleSubmitAvatarNickname}
        initialData={{
          avatarUrl: userInfo?.avatarUrl,
          nickname: userInfo?.nickname
        }}
      />
    </View>
  )
}

export default MyPage
