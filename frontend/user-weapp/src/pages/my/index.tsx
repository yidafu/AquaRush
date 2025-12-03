import { Component } from 'react'
import { View, Text, Image, Button } from '@tarojs/components'
import { AtButton, AtCard, AtList, AtListItem, AtAvatar, AtBadge, AtToast, AtDivider } from 'taro-ui'
import CustomIcon from '../../components/CustomIcon'
import { ThemeSwitcher } from '../../components/ThemeProvider'
import Taro from '@tarojs/taro'
import { authService, type UserInfo as AuthUserInfo, type LoginResponse } from '../../utils/auth'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/list.scss"
import "taro-ui/dist/style/components/avatar.scss"
import "taro-ui/dist/style/components/badge.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/divider.scss"
import './index.scss'

// Use AuthUserInfo from auth utility

interface OrderStats {
  pendingPayment: number
  pendingDelivery: number
  delivering: number
  completed: number
  afterSales: number
}

interface MyPageState {
  userInfo: AuthUserInfo | null
  orderStats: OrderStats
  loading: boolean
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
}

export default class MyPage extends Component<{}, MyPageState> {
  constructor(props) {
    super(props)
    this.state = {
      userInfo: null,
      orderStats: {
        pendingPayment: 0,
        pendingDelivery: 0,
        delivering: 0,
        completed: 0,
        afterSales: 0
      },
      loading: true,
      showToast: false,
      toastText: '',
      toastType: 'success'
    }
  }

  componentDidMount() {
    this.loadUserInfo()
    this.loadOrderStats()
  }

  componentDidShow() {
    // 页面显示时刷新用户信息
    this.loadUserInfo()
    this.loadOrderStats()

    // 检查并刷新认证状态
    this.checkAuthStatus()
  }

  checkAuthStatus = async () => {
    try {
      // 检查是否有有效的认证状态
      if (authService.isAuthenticated()) {
        const currentUser = authService.getUserInfo()
        if (currentUser && currentUser.id) {
          this.setState({ userInfo: currentUser })
        }
      } else {
        // 如果没有认证，保持游客状态
        if (this.state.userInfo?.id) {
          // 如果之前有用户信息但现在没有认证，清除状态
          this.setState({
            userInfo: {
              id: '',
              nickname: '游客',
              avatarUrl: '',
              phone: '',
              balance: 0,
              points: 0,
              level: '普通用户',
              isVip: false
            },
            orderStats: {
              pendingPayment: 0,
              pendingDelivery: 0,
              delivering: 0,
              completed: 0,
              afterSales: 0
            }
          })
        }
      }
    } catch (error) {
      console.error('检查认证状态失败:', error)
    }
  }

  onPullDownRefresh = () => {
    Promise.all([
      this.loadUserInfo(),
      this.loadOrderStats()
    ]).finally(() => {
      Taro.stopPullDownRefresh()
    })
  }

  showToast = (text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    this.setState({
      showToast: true,
      toastText: text,
      toastType: type
    })
  }

  hideToast = () => {
    this.setState({ showToast: false })
  }

  loadUserInfo = async () => {
    try {
      // 检查是否已登录
      if (authService.isAuthenticated()) {
        const storedUserInfo = authService.getUserInfo()
        if (storedUserInfo) {
          this.setState({ userInfo: storedUserInfo })
          return
        }
      }

      // 尝试从服务器获取当前用户信息
      const currentUser = await authService.getCurrentUser()
      if (currentUser) {
        this.setState({ userInfo: currentUser })
      } else {
        // 设置游客用户信息
        this.setState({
          userInfo: {
            id: '',
            nickname: '游客',
            avatarUrl: '',
            phone: '',
            balance: 0,
            points: 0,
            level: '普通用户',
            isVip: false
          }
        })
      }
    } catch (error) {
      console.error('获取用户信息失败:', error)

      // 设置默认用户信息
      this.setState({
        userInfo: {
          id: '',
          nickname: '游客',
          avatarUrl: '',
          phone: '',
          balance: 0,
          points: 0,
          level: '普通用户',
          isVip: false
        }
      })
    }
  }

  loadOrderStats = async () => {
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

      this.setState({ orderStats: mockOrderStats })
    } catch (error) {
      console.error('获取订单统计失败:', error)
    } finally {
      this.setState({ loading: false })
    }
  }

  handleProfileEdit = () => {
    // 如果用户未登录，触发微信登录
    if (!authService.isAuthenticated()) {
      this.handleWeChatLogin()
    } else {
      Taro.navigateTo({
        url: '/pages/profile-edit/index'
      })
    }
  }

  handleWeChatLogin = async () => {
    try {
      this.showToast('正在登录...', 'loading')

      const loginData = await authService.weChatLogin()

      this.setState({ userInfo: loginData.userInfo })
      this.showToast('登录成功', 'success')

      // 重新加载订单统计
      this.loadOrderStats()
    } catch (error) {
      console.error('微信登录失败:', error)
      const errorMsg = error instanceof Error ? error.message : '登录失败'
      this.showToast(errorMsg, 'error')
    }
  }

  handleRecharge = () => {
    Taro.navigateTo({
      url: '/pages/recharge/index'
    })
  }

  handlePointsMall = () => {
    Taro.navigateTo({
      url: '/pages/points-mall/index'
    })
  }

  handleOrderNavigation = (type: string) => {
    Taro.navigateTo({
      url: `/pages/order-list/index?tab=${type}`
    })
  }

  handleAddressManagement = () => {
    Taro.navigateTo({
      url: '/pages/address-list/index'
    })
  }

  handleCouponCenter = () => {
    Taro.navigateTo({
      url: '/pages/coupon-center/index'
    })
  }

  handleCustomerService = () => {
    // 联系客服
    Taro.makePhoneCall({
      phoneNumber: '400-888-8888'
    })
  }

  handleFeedback = () => {
    Taro.navigateTo({
      url: '/pages/feedback/index'
    })
  }

  handleAbout = () => {
    Taro.navigateTo({
      url: '/pages/about/index'
    })
  }

  handleSettings = () => {
    Taro.navigateTo({
      url: '/pages/settings/index'
    })
  }

  handleShare = () => {
    Taro.showShareMenu({
      withShareTicket: true
    })
  }

  handleLogout = async () => {
    // 微信小程序不需要手动登出，这个方法仅用于程序内部处理认证过期
    try {
      await authService.logout()

      // 清除用户信息
      this.setState({
        userInfo: {
          id: '',
          nickname: '游客',
          avatarUrl: '',
          phone: '',
          balance: 0,
          points: 0,
          level: '普通用户',
          isVip: false
        }
      })

      // 清除订单统计
      this.setState({
        orderStats: {
          pendingPayment: 0,
          pendingDelivery: 0,
          delivering: 0,
          completed: 0,
          afterSales: 0
        }
      })
    } catch (error) {
      console.error('清理认证状态失败:', error)
    }
  }

  renderUserSection = () => {
    const { userInfo } = this.state

    if (!userInfo) {
      return (
        <View className='user-section'>
          <Text>加载中...</Text>
        </View>
      )
    }

    const isLoggedIn = authService.isAuthenticated()

    return (
      <View className='user-section'>
        <View
          className={`user-info ${!isLoggedIn ? 'not-logged-in' : ''}`}
          onClick={this.handleProfileEdit}
        >
          <View className='avatar-section'>
            {userInfo.avatarUrl ? (
              <AtAvatar
                image={userInfo.avatarUrl}
                className='user-avatar'
                circle
              />
            ) : (
              <AtAvatar
                size='large'
                circle
                text={userInfo.nickname ?? '游客'}
                className='default-avatar'
              />
            )}

          </View>

          <View className='user-details'>
            <View className='user-name-section'>
              <Text className='user-name'>{userInfo.nickname}</Text>
              <View className='user-level'>
                <CustomIcon value='bookmark' size={12} color='#ff6b35' />
                <Text className='level-text'>{userInfo.level}</Text>
              </View>
            </View>
            {userInfo.phone && (
              <Text className='user-phone'>{userInfo.phone}</Text>
            )}
            {!isLoggedIn && (
              <Text className='login-hint'>点击头像登录</Text>
            )}
          </View>

          <CustomIcon value='chevron-right' size={16} color='#999' />
        </View>


      </View>
    )
  }

  renderOrderSection = () => {
    const { orderStats } = this.state

    const orderItems = [
      {
        icon: '/assets/icons/order/all.png',
        label: '全部',
        count: 0,
        type: 'pending_payment',
        color: '#ff6b35'
      },
      {
        icon: '/assets/icons/order/credit-card.png',
        label: '待付款',
        count: orderStats.pendingPayment,
        type: 'pending_payment',
        color: '#ff6b35'
      },
      {
        icon: '/assets/icons/order/shopping-bag.png',
        label: '待配送',
        count: orderStats.pendingDelivery,
        type: 'pending_delivery',
        color: '#667eea'
      },
      {
        icon: '/assets/icons/order/truck.png',
        label: '配送中',
        count: orderStats.delivering,
        type: 'delivering',
        color: '#19be6b'
      },
      // {
      //   icon: '/assets/icons/order/check-circle.png',
      //   label: '已完成',
      //   count: orderStats.completed,
      //   type: 'completed',
      //   color: '#999'
      // },
      // {
      //   icon: '/assets/icons/order/message.png',
      //   label: '售后',
      //   count: orderStats.afterSales,
      //   type: 'after_sales',
      //   color: '#f39c12'
      // }
    ]

    return (
      <AtCard title='我的订单' className='order-section'>
        <View className='order-grid'>
          {orderItems.map((item, index) => (
            <View
              key={index}
              className='order-item'
              onClick={() => this.handleOrderNavigation(item.type)}
            >
              <AtBadge value={item.count} maxValue={99} className='order-badge'>
                <View className='order-icon' style={{ color: item.color }}>
                  <CustomIcon value={item.icon} size={20} />
                </View>
                <Text className='order-label'>{item.label}</Text>
              </AtBadge>
            </View>
          ))}
        </View>

        <AtDivider />
      </AtCard>
    )
  }

  renderServiceSection = () => {
    const serviceItems = [
      {
        icon: '/assets/icons/service/map-pin.png',
        title: '收货地址',
        description: '管理收货地址',
        onClick: this.handleAddressManagement
      },
      {
        icon: '/assets/icons/service/comments.png',
        title: '客服中心',
        description: '联系在线客服',
        onClick: this.handleCustomerService
      },
      {
        icon: '/assets/icons/service/feedback.png',
        title: '意见反馈',
        description: '帮助我们改进',
        onClick: this.handleFeedback
      },
      {
        icon: '/assets/icons/service/info-circle.png',
        title: '关于我们',
        description: '了解 AquaRush',
        onClick: this.handleAbout
      }
    ]

    return (
      <AtCard title='常用服务' className='service-section'>
        <View className='service-grid'>
          {serviceItems.map((item, index) => (
            <View
              key={index}
              className='service-item'
              onClick={item.onClick}
            >
              <View className='service-icon'>
                <CustomIcon value={item.icon} size={24} color='#667eea' />
              </View>
              <Text className='service-title'>{item.title}</Text>
              <Text className='service-desc'>{item.description}</Text>
            </View>
          ))}
        </View>
      </AtCard>
    )
  }

  render() {
    const { loading, showToast, toastText, toastType, userInfo } = this.state

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
        {this.renderUserSection()}

        {/* 订单区域 */}
        {this.renderOrderSection()}

        {/* 服务区域 */}
        {this.renderServiceSection()}

        {/* 其他设置 */}
        <AtCard className='settings-section'>
          <AtList>
            <AtListItem
              title='主题设置'
              note='选择应用主题颜色'
              arrow='right'
              iconInfo={{ value: '/assets/icons/service/settings.png', color: '#667eea' }}
              onClick={() => Taro.navigateTo({ url: '/pages/theme-settings/index' })}
            />
            <AtListItem
              title='设置'
              arrow='right'
              iconInfo={{ value: '/assets/icons/service/settings.png', color: '#667eea' }}
              onClick={this.handleSettings}
            />
          </AtList>

          <View style={{ padding: '8px 16px', display: 'flex', justifyContent: 'center', alignItems: 'center' }}>
            <Text style={{ fontSize: '12px', color: '#666', marginRight: '8px' }}>快速切换主题：</Text>
            <ThemeSwitcher showLabel={true} />
          </View>
        </AtCard>

        {/* 底部安全区域 */}
        <View className='safe-bottom' />

        {/* Toast 提示 */}
        <AtToast
          isOpened={showToast}
          text={toastText}
          status={toastType}
          onClose={this.hideToast}
        />
      </View>
    )
  }
}
