import React, { useState, useEffect, useMemo } from 'react'
import { View, Text, Image, Button } from '@tarojs/components'
import { AtButton, AtCard, AtList, AtListItem, AtToast } from 'taro-ui'
import { ThemeSwitcher } from '../../components/ThemeProvider'
import AvatarNicknameForm from './components/AvatarNicknameForm'
import UserSection from './components/UserSection'
import OrderSection from './components/OrderSection'
import ServiceSection from './components/ServiceSection'
import Taro, { useReady, useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { authService, type UserInfo as AuthUserInfo, type LoginResponse } from '../../utils/auth'

// Taro UI 样式已在 app.scss 中全局引入，无需重复引入
import './index.scss'
import { CONTACT_INFO } from '@/constants'

// Use AuthUserInfo from auth utility

interface OrderStats {
  pendingPayment: number
  pendingDelivery: number
  delivering: number
  completed: number
  afterSales: number
}

const MyPage: React.FC = () => {
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
    try {
      // TODO: 实际项目中这里应该调用获取订单统计的API
      // const orderStats = await getOrderStats()

      // 模拟订单统计数据
      const mockOrderStats: OrderStats = {
        pendingPayment: 2,
        pendingDelivery: 1,
        delivering: 3,
        completed: 25,
        afterSales: 1
      }

      setOrderStats(mockOrderStats)
    } catch (error) {
      console.error('获取订单统计失败:', error)
    } finally {
      setLoading(false)
    }
  }

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

  const handleRecharge = (): void => {
    Taro.navigateTo({
      url: '/pages/recharge/index'
    })
  }

  const handlePointsMall = (): void => {
    Taro.navigateTo({
      url: '/pages/points-mall/index'
    })
  }

  const handleOrderNavigation = (type: string): void => {
    Taro.navigateTo({
      url: `/pages/order-list/index?tab=${type}`
    })
  }

  const handleAddressManagement = (): void => {
    Taro.navigateTo({
      url: '/pages/address-list/index'
    })
  }

  const handleCustomerService = (): void => {
    // 联系客服
    Taro.makePhoneCall({
      phoneNumber: CONTACT_INFO.COMPLAINT_HOTLINE
    })
  }

  const handleFeedback = (): void => {
    Taro.navigateTo({
      url: '/pages/feedback/index'
    })
  }

  const handleAbout = (): void => {
    Taro.navigateTo({
      url: '/pages/about/index'
    })
  }

  const handleSettings = (): void => {
    Taro.navigateTo({
      url: '/pages/settings/index'
    })
  }

  const handleShare = (): void => {
    Taro.showShareMenu({
      withShareTicket: true
    })
  }


  const serviceItems = useMemo(() =>[
    {
      icon: '/assets/icons/service/map-pin.png',
      title: '收货地址',
      description: '管理收货地址',
      onClick: handleAddressManagement
    },
    {
      icon: '/assets/icons/service/comments.png',
      title: '客服中心',
      description: '联系在线客服',
      onClick: handleCustomerService
    },
    {
      icon: '/assets/icons/service/feedback.png',
      title: '意见反馈',
      description: '帮助我们改进',
      onClick: handleFeedback
    },
    {
      icon: '/assets/icons/service/info-circle.png',
      title: '关于我们',
      description: '了解好喝山泉',
      onClick: handleAbout
    }
  ], [])

  // Return JSX directly
  if (loading) {
    return (
      <View className='my-page'>
        <View className='loading-container'>
          <Text>加载中...</Text>
        </View>
      </View>
    )
  }

  return (
    <View className='my-page'>
      {/* 用户信息区域 */}
      <UserSection
        userInfo={userInfo}
        onProfileEdit={handleProfileEdit}
      />

      {/* 订单区域 */}
      <OrderSection
        orderStats={orderStats}
        onOrderNavigation={handleOrderNavigation}
      />

      {/* 服务区域 */}
      <ServiceSection serviceItems={serviceItems} />

      {/* 其他设置 */}
      <AtCard title='其他' className='settings-section'>
        <AtList>

          <AtListItem
            title='设置'
            arrow='right'
            iconInfo={{ value: '/assets/icons/service/settings.png', color: 'var(--theme-primary)' }}
            onClick={handleSettings}
          />
        </AtList>

      </AtCard>
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
