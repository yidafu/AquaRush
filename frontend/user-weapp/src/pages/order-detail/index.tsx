import { Component } from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtCard, AtIcon, AtButton, AtToast, AtTag, AtTimeline, AtProgress } from 'taro-ui'
import Taro from '@tarojs/taro'
import { Money, OrderReviewCheckResponse, normalizeCents } from '@aquarush/common'
import OrderService from '../../services/OrderService'
import { userActionTracker } from '../../utils/tracking'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/tag.scss"
import "taro-ui/dist/style/components/timeline.scss"
import "taro-ui/dist/style/components/progress.scss"
import './index.scss'

interface OrderItem {
  id: string
  name: string
  image: string
  // 成交单价快照(分,R-MKT-02 优惠价;无营销价=目录价)
  priceCents: number
  // 目录价(分,划线价展示依据)
  originalPriceCents?: number
  // 成交单价来源快照(CATALOG_PRICE=无营销价)
  priceSource?: string | null
  quantity: number
  specifications?: string[]
}

// 评价信息(F-ORD-007):服务端返回结构(OrderReviewCheckResponse),含已评价内容与是否可评
type ReviewCheckState = OrderReviewCheckResponse

interface DeliveryInfo {
  deliveryWorkerId?: string
  deliveryWorkerName?: string
  deliveryWorkerPhone?: string
  deliveryWorkerAvatar?: string
  estimatedDeliveryTime?: string
  currentLocation?: string
  deliveryDistance?: string
}

interface OrderTimeline {
  time: string
  title: string
  content?: string
  icon?: string
  color?: string
}

interface OrderDetailState {
  order: {
    id: string
    orderNo: string
    status: 'pending_payment' | 'pending_delivery' | 'delivering' | 'completed' | 'cancelled'
    items: OrderItem[]
    totalAmount: number
    deliveryFee: number
    finalAmount: number
    address: {
      receiverName: string
      phone: string
      province: string
      city: string
      district: string
      detailAddress: string
    }
    paymentMethod: string
    deliveryTime: string
    // 配送时段(AC-ORD-001-05,随单落库展示;IMMEDIATE/MORNING/AFTERNOON/EVENING)
    deliveryTimeSlot?: string
    createTime: string
    paymentTime?: string
    acceptTime?: string
    completedTime?: string
    cancelledTime?: string
    remark?: string
    deliveryInfo?: DeliveryInfo
    timeline: OrderTimeline[]
  } | null
  loading: boolean
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
  refreshing: boolean
  // 评价信息(F-ORD-007):已完成订单的评价入口/已评内容
  reviewInfo: ReviewCheckState | null
}

const STATUS_MAP = {
  pending_payment: { text: '待付款', color: 'var(--theme-secondary)', step: 1 },
  pending_delivery: { text: '待配送', color: 'var(--theme-primary)', step: 2 },
  delivering: { text: '配送中', color: 'var(--theme-success)', step: 3 },
  completed: { text: '已完成', color: 'var(--theme-text-tertiary)', step: 4 },
  cancelled: { text: '已取消', color: 'var(--theme-text-tertiary)', step: 0 }
}

const PAYMENT_METHOD_MAP = {
  wechat: '微信支付',
  balance: '余额支付'
}

// 配送时段文案(AC-ORD-001-05,与 DeliveryTimeSelector/后端 DeliveryTimeSlot 枚举对应)
const DELIVERY_TIME_SLOT_MAP: Record<string, string> = {
  IMMEDIATE: '立即配送（30分钟内）',
  MORNING: '上午配送（9:00-12:00）',
  AFTERNOON: '下午配送（14:00-18:00）',
  EVENING: '晚上配送（18:00-21:00）'
}

export default class OrderDetail extends Component<{}, OrderDetailState> {
  orderId: string = ''

  constructor(props) {
    super(props)
    this.state = {
      order: null,
      loading: true,
      showToast: false,
      toastText: '',
      toastType: 'success',
      refreshing: false,
      reviewInfo: null
    }
  }

  componentDidMount() {
    // 获取页面参数
    const instance = Taro.getCurrentInstance()
    const { orderId } = instance.router?.params || {}

    if (orderId) {
      this.orderId = orderId
      this.loadOrderDetail()
    } else {
      this.showToast('订单ID缺失', 'error')
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    }
  }

  componentDidShow() {
    userActionTracker.trackPageView()
    // 页面显示时刷新订单详情
    if (this.orderId && !this.state.loading) {
      this.loadOrderDetail()
    }
  }

  onPullDownRefresh = () => {
    this.setState({ refreshing: true }, () => {
      this.loadOrderDetail().finally(() => {
        Taro.stopPullDownRefresh()
      })
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

  loadOrderDetail = async () => {
    try {
      this.setState({ loading: true })

      // 优先加载真实订单数据(F-ORD-003);失败回退模拟数据(离线/演示场景)
      const result = await OrderService.getInstance().getOrder(this.orderId)
      if (result.success && result.data) {
        const order = this.mapRealOrder(result.data)
        this.setState({ order })
        // 已完成订单:加载评价信息(F-ORD-007)
        if (order.status === 'completed') {
          this.loadReviewCheck()
        }
        return
      }

      // 模拟订单详情数据
      const mockOrder = {
        id: this.orderId,
        orderNo: 'ORD202412020001',
        status: 'delivering' as const,
        items: [
          {
            id: '1',
            name: '农夫山泉 天然矿泉水 550ml',
            image: '/assets/product-water.jpg',
            priceCents: 200,
            quantity: 2,
            specifications: ['24瓶装']
          },
          {
            id: '2',
            name: '怡宝 纯净水 380ml',
            image: '/assets/product-water.jpg',
            priceCents: 150,
            quantity: 3,
            specifications: ['36瓶装']
          }
        ],
        totalAmount: 8700,
        deliveryFee: 0,
        finalAmount: 8700,
        address: {
          receiverName: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detailAddress: '科技园南区深南大道9988号'
        },
        paymentMethod: 'wechat',
        deliveryTime: 'immediate',
        createTime: '2024-12-02 10:30:00',
        paymentTime: '2024-12-02 10:35:00',
        acceptTime: '2024-12-02 10:45:00',
        actualDeliveryTime: '2024-12-02 10:50:00',
        remark: '请送到前台',
        deliveryInfo: {
          deliveryWorkerId: 'DW001',
          deliveryWorkerName: '配送员小李',
          deliveryWorkerPhone: '13600136000',
          deliveryWorkerAvatar: '/assets/delivery-worker.jpg',
          estimatedDeliveryTime: '2024-12-02 11:15:00',
          currentLocation: '科技园北区',
          deliveryDistance: '1.2km'
        },
        timeline: [
          {
            time: '2024-12-02 10:30:00',
            title: '订单创建',
            content: '您的订单已创建',
            icon: 'bookmark',
            color: 'var(--theme-primary)'
          },
          {
            time: '2024-12-02 10:35:00',
            title: '支付成功',
            content: '使用微信支付成功',
            icon: 'credit-card',
            color: 'var(--theme-secondary)'
          },
          {
            time: '2024-12-02 10:45:00',
            title: '商家接单',
            content: '商家已接单，正在准备商品',
            icon: 'shopping-bag',
            color: 'var(--theme-primary)'
          },
          {
            time: '2024-12-02 10:50:00',
            title: '配送员取货',
            content: '配送员小李已取货，正在配送中',
            icon: 'truck',
            color: 'var(--theme-success)'
          }
        ]
      }

      this.setState({ order: mockOrder })
    } catch (error) {
      console.error('获取订单详情失败:', error)
      this.showToast('获取订单详情失败', 'error')
    } finally {
      this.setState({
        loading: false,
        refreshing: false
      })
    }
  }

  /**
   * GraphQL OrderStatus → 页面状态键
   */
  mapStatus = (status: string): 'pending_payment' | 'pending_delivery' | 'delivering' | 'completed' | 'cancelled' => {
    const map: Record<string, 'pending_payment' | 'pending_delivery' | 'delivering' | 'completed' | 'cancelled'> = {
      PENDING_PAYMENT: 'pending_payment',
      PENDING_DELIVERY: 'pending_delivery',
      PENDING_DISPATCH: 'pending_delivery',
      DELIVERING: 'delivering',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled',
      REFUNDED: 'cancelled'
    }
    return map[status] || 'pending_payment'
  }

  /**
   * 真实订单 → 页面订单结构(F-ORD-004 多商品:商品信息来自 order.items 随单快照,金额均为分)
   */
  mapRealOrder = (order: any) => {
    const rawItems: any[] = Array.isArray(order.items) && order.items.length > 0 ? order.items : []
    const items: OrderItem[] = rawItems.map((item: any) => ({
      id: item.productId,
      name: item.product?.name || '商品',
      image: item.product?.coverImageUrl || '/assets/default-product.png',
      // 优惠价=明细成交单价快照;无营销价时=目录价快照(AC-ORD-003-09 —— R-MKT-01/02)
      priceCents: normalizeCents(item.unitPriceCents) ?? normalizeCents(item.product?.priceCents) ?? 0,
      originalPriceCents: item.unitPriceCents != null && normalizeCents(item.unitPriceCents) !== normalizeCents(item.product?.priceCents)
        ? normalizeCents(item.product?.priceCents) ?? undefined
        : undefined,
      priceSource: 'CATALOG_PRICE',
      quantity: item.quantity,
      specifications: item.product?.specification ? [item.product.specification] : []
    }))

    const status = this.mapStatus(order.status)
    const timeline: OrderTimeline[] = []
    if (order.createdAt) {
      timeline.push({ time: order.createdAt, title: '订单创建', content: '您的订单已创建', icon: 'bookmark', color: 'var(--theme-primary)' })
    }
    if (order.paymentTime) {
      timeline.push({ time: order.paymentTime, title: '支付成功', content: '使用微信支付成功', icon: 'credit-card', color: 'var(--theme-secondary)' })
    }
    if (order.deliveryStartedAt) {
      timeline.push({ time: order.deliveryStartedAt, title: '开始配送', content: '配送员正在配送中', icon: 'truck', color: 'var(--theme-success)' })
    }
    if (order.completedAt) {
      timeline.push({ time: order.completedAt, title: '订单完成', content: '订单已完成', icon: 'check-circle', color: 'var(--theme-success)' })
    }

    return {
      id: order.id,
      orderNo: order.orderNo,
      status,
      items,
      totalAmount: order.amountCents,
      deliveryFee: order.deliveryFeeCents,
      finalAmount: order.payAmountCents,
      address: order.address ? {
        receiverName: order.address.receiverName,
        phone: order.address.phone,
        province: order.address.province,
        city: order.address.city,
        district: order.address.district,
        detailAddress: order.address.detailAddress
      } : { receiverName: '', phone: '', province: '', city: '', district: '', detailAddress: '' },
      paymentMethod: order.paymentMethod === 'WECHAT_PAY' ? 'wechat' : (order.paymentMethod || 'wechat'),
      deliveryTime: 'immediate',
      // 配送时段(AC-ORD-001-05):随单快照,缺省=立即配送
      deliveryTimeSlot: order.deliveryTimeSlot || 'IMMEDIATE',
      createTime: order.createdAt || '',
      paymentTime: order.paymentTime || undefined,
      remark: order.remark || undefined,
      timeline
    }
  }

  /**
   * 立即支付(F-ORD-005):测试环境假支付自动完成,真实环境拉起微信支付
   */
  handlePayment = async () => {
    const { order } = this.state
    if (!order) return

    this.showToast('正在发起支付...', 'loading')

    const payResult = await OrderService.getInstance().createOrderPayment(order.id)
    if (!payResult.success || !payResult.data) {
      this.showToast(payResult.error?.message || '支付发起失败', 'error')
      return
    }
    const { autoCompleted, requiresSimulation, paymentParams } = payResult.data

    // 测试环境假支付:服务端已自动完成,直接刷新订单状态
    if (autoCompleted) {
      this.showToast('支付成功（模拟支付自动完成）', 'success')
      setTimeout(() => this.loadOrderDetail(), 800)
      return
    }
    if (requiresSimulation || !paymentParams) {
      this.showToast('支付功能待接入', 'error')
      return
    }

    try {
      await Taro.requestPayment({
        timeStamp: paymentParams.timeStamp as string,
        nonceStr: paymentParams.nonceStr as string,
        package: paymentParams.package as string,
        signType: paymentParams.signType as 'MD5' | 'HMAC-SHA256' | 'RSA',
        paySign: paymentParams.paySign as string
      })
      this.showToast('支付成功', 'success')
      setTimeout(() => this.loadOrderDetail(), 800)
    } catch (e) {
      console.error('微信支付未完成:', e)
      this.showToast('支付未完成，请重试', 'error')
    }
  }

  handleCancelOrder = () => {
    const { order } = this.state
    if (!order) return

    Taro.showModal({
      title: '确认取消',
      content: '确定要取消这个订单吗？取消后无法恢复。',
      success: async (res) => {
        if (res.confirm) {
          try {
            // 真实取消接口(F-ORD-006,G02 接线):未支付即时取消恢复库存;已支付自动退款
            const result = await OrderService.getInstance().cancelOrder(order.id)
            if (!result.success) {
              throw new Error(result.error?.message || '取消订单失败')
            }
            this.showToast('订单已取消', 'success')

            // 刷新订单详情
            this.loadOrderDetail()
          } catch (error) {
            console.error('取消订单失败:', error)
            this.showToast(error instanceof Error ? error.message : '取消订单失败', 'error')
          }
        }
      }
    })
  }

  /**
   * 评价信息加载(F-ORD-007):一单一评,已评价展示内容,未评价展示入口
   */
  loadReviewCheck = async () => {
    try {
      const result = await OrderService.getInstance().getReviewCheck(this.orderId)
      if (result.success && result.data) {
        this.setState({ reviewInfo: result.data })
      }
    } catch (error) {
      console.error('获取评价信息失败:', error)
    }
  }

  /**
   * 进入评价页(F-ORD-007)
   */
  handleReview = () => {
    Taro.navigateTo({
      url: `/pages/order-review/index?orderId=${this.orderId}`
    })
  }

  handleContactDelivery = () => {
    const { order } = this.state
    if (!order || !order.deliveryInfo?.deliveryWorkerPhone) return

    Taro.makePhoneCall({
      phoneNumber: order.deliveryInfo.deliveryWorkerPhone
    })
  }

  handleReorder = () => {
    const { order } = this.state
    if (!order) return

    // 检查order.items是否为数组
    if (!Array.isArray(order.items)) {
      console.warn('handleReorder: order.items is not an array', order.items)
      this.showToast('订单商品信息异常', 'error')
      return
    }

    // 将订单商品添加到订单确认页面
    const orderData = {
      products: order.items.map(item => ({
        id: item.id,
        name: item.name || '商品名称',
        price: item.priceCents || 0,
        quantity: item.quantity || 0,
        image: item.image || '/assets/default-product.png',
        specifications: Array.isArray(item.specifications) ? item.specifications : []
      })),
      totalAmount: order.totalAmount || 0
    }

    Taro.setStorageSync('pendingOrder', orderData)

    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }

  handleCopyOrderNo = () => {
    const { order } = this.state
    if (!order) return

    Taro.setClipboardData({
      data: order.orderNo,
      success: () => {
        this.showToast('订单号已复制', 'success')
      }
    })
  }

  handleTrackDelivery = () => {
    const { order } = this.state
    if (!order) return

    // 跳转到配送跟踪页面
    Taro.navigateTo({
      url: `/pages/delivery-tracking/index?orderId=${order.id}`
    })
  }

  getTimelineColor = (color?: string): 'blue' | 'green' | 'red' | 'yellow' => {
    if (!color) return 'blue'

    const colorMap: Record<string, 'blue' | 'green' | 'red' | 'yellow'> = {
      'var(--theme-primary)': 'blue',
      'var(--theme-success)': 'green',
      'var(--theme-secondary)': 'red',
      'var(--theme-warning)': 'yellow',
      // Legacy color mappings for backward compatibility
      '#667eea': 'blue',
      '#19be6b': 'green',
      '#ff6b35': 'red',
      '#ffc107': 'yellow'
    }

    return colorMap[color] || 'blue'
  }

  renderOrderItems = (items: OrderItem[] = []) => {
    if (!Array.isArray(items)) {
      console.warn('renderOrderItems: items is not an array', items)
      return <View className='order-items'><Text>暂无商品信息</Text></View>
    }

    return (
      <View className='order-items'>
        {items.map((item, index) => (
          <View key={`${item.id}-${index}`} className='order-item'>
            <Image
              src={item.image || '/assets/default-product.png'}
              mode='aspectFill'
              className='item-image'
            />
            <View className='item-info'>
              <Text className='item-name'>{item.name || '商品名称'}</Text>
              {item.specifications && Array.isArray(item.specifications) && item.specifications.length > 0 && (
                <Text className='item-spec'>
                  规格：{item.specifications.join(', ')}
                </Text>
              )}
              <View className='item-bottom'>
                <View className='item-price-row'>
                  <Text className='item-price'><Money cents={item.priceCents || 0} /></Text>
                  {/* 有营销价时划线目录价(AC-ORD-003-09 —— R-MKT-01/02) */}
                  {item.priceSource && item.priceSource !== 'CATALOG_PRICE' && item.originalPriceCents != null && (
                    <Text className='item-original-price'><Money cents={item.originalPriceCents} /></Text>
                  )}
                </View>
                <Text className='item-quantity'>x{item.quantity || 0}</Text>
              </View>
            </View>
          </View>
        ))}
      </View>
    )
  }

  render() {
    const { order, loading, showToast, toastText, toastType, reviewInfo } = this.state

    if (loading) {
      return (
        <View className='order-detail-page'>
          <View className='loading-container'>
            <Text>加载中...</Text>
          </View>
        </View>
      )
    }

    if (!order) {
      return (
        <View className='order-detail-page'>
          <View className='error-container'>
            <Text>订单不存在</Text>
            <AtButton
              type='primary'
              size='small'
              onClick={() => Taro.navigateBack()}
            >
              返回
            </AtButton>
          </View>
        </View>
      )
    }

    const status = STATUS_MAP[order.status] || STATUS_MAP.cancelled

    return (
      <View className='order-detail-page'>
        {/* 订单状态头部 */}
        <View className='status-header' style={{ background: `linear-gradient(135deg, ${status.color}22, ${status.color}11)` }}>
          <View className='status-info'>
            <AtTag
              type='primary'
              size='large'
              circle
              customStyle={{
                background: status.color,
                color: 'white',
                border: 'none'
              }}
            >
              {status.text}
            </AtTag>
            <Text className='order-no'>
              订单号：{order.orderNo}
              <AtIcon
                value='copy'
                size='14'
                color='#667eea'
                onClick={this.handleCopyOrderNo}
              />
            </Text>
          </View>

          {/* 配送进度 */}
          {order.status !== 'cancelled' && order.status !== 'pending_payment' && (
            <View className='delivery-progress'>
              <AtProgress
                percent={status.step * 25}
                strokeWidth={4}
                color={status.color}
                isShowPercent={false}
              />
              <View className='progress-steps'>
                <View className={`step-item ${status.step >= 1 ? 'active' : ''}`}>
                  <View className='step-dot' />
                  <Text className='step-text'>下单</Text>
                </View>
                <View className={`step-item ${status.step >= 2 ? 'active' : ''}`}>
                  <View className='step-dot' />
                  <Text className='step-text'>付款</Text>
                </View>
                <View className={`step-item ${status.step >= 3 ? 'active' : ''}`}>
                  <View className='step-dot' />
                  <Text className='step-text'>配送</Text>
                </View>
                <View className={`step-item ${status.step >= 4 ? 'active' : ''}`}>
                  <View className='step-dot' />
                  <Text className='step-text'>完成</Text>
                </View>
              </View>
            </View>
          )}
        </View>

        {/* 配送员信息 */}
        {order.status === 'delivering' && order.deliveryInfo && (
          <AtCard
            title='配送员信息'
            className='delivery-info-card shadow-default'
          >
            <View className='delivery-worker'>
              <View className='worker-avatar'>
                <Image
                  src={order.deliveryInfo.deliveryWorkerAvatar || '/assets/default-avatar.png'}
                  mode='aspectFill'
                  className='avatar-image'
                />
              </View>
              <View className='worker-info'>
                <Text className='worker-name'>{order.deliveryInfo.deliveryWorkerName}</Text>
                <Text className='worker-phone'>{order.deliveryInfo.deliveryWorkerPhone}</Text>
                <Text className='delivery-location'>
                  <AtIcon value='map-pin' size='12' color='#999' />
                  {order.deliveryInfo.currentLocation} · {order.deliveryInfo.deliveryDistance}
                </Text>
              </View>
              <View className='worker-actions'>
                <AtButton
                  type='primary'
                  size='small'
                  circle
                  onClick={this.handleContactDelivery}
                >
                  <AtIcon value='phone' size='14' color='white' />
                </AtButton>
                <AtButton
                  type='secondary'
                  size='small'
                  circle
                  onClick={this.handleTrackDelivery}
                >
                  <AtIcon value='navigation' size='14' color='#666' />
                </AtButton>
              </View>
            </View>
          </AtCard>
        )}

        {/* 订单时间轴 */}
        {Array.isArray(order.timeline) && order.timeline.length > 0 && (
          <AtCard
            title='订单跟踪'
            className='timeline-card shadow-default'
          >
            <AtTimeline
              items={order.timeline.map(item => ({
                title: item.title || '状态更新',
                content: Array.isArray(item.content) ? item.content : [item.content || ''],
                icon: item.icon || 'clock',
                color: this.getTimelineColor(item.color)
              }))}
            />
          </AtCard>
        )}

        {/* 商品信息 */}
        <AtCard
          title='商品信息'
          className='items-card shadow-default'
        >
          {this.renderOrderItems(order.items)}
          <View className='order-summary'>
            <View className='summary-item'>
              <Text className='summary-label'>商品总价</Text>
              <Text className='summary-value'><Money cents={order.totalAmount} /></Text>
            </View>
            <View className='summary-item'>
              <Text className='summary-label'>配送费</Text>
              <Text className='summary-value'>
                {order.deliveryFee === 0 ? '免配送费' : <Money cents={order.deliveryFee} />}
              </Text>
            </View>
            <View className='summary-item total'>
              <Text className='summary-label'>实付金额</Text>
              <Text className='summary-value total-amount'><Money cents={order.finalAmount} /></Text>
            </View>
          </View>
        </AtCard>

        {/* 订单评价(F-ORD-007):已评价展示内容 */}
        {order.status === 'completed' && reviewInfo?.hasReviewed && reviewInfo.review && (
          <AtCard
            title='我的评价'
            className='review-card shadow-default'
          >
            <View className='review-content'>
              <View className='review-stars'>
                {[1, 2, 3, 4, 5].map(star => (
                  <Text
                    key={star}
                    className={`review-star ${star <= reviewInfo.review!.rating ? 'active' : ''}`}
                  >
                    ★
                  </Text>
                ))}
              </View>
              {reviewInfo.review.comment && (
                <Text className='review-comment'>{reviewInfo.review.comment}</Text>
              )}
              <Text className='review-time'>
                {reviewInfo.review.createdAt}
                {reviewInfo.review.isAnonymous ? ' · 匿名评价' : ''}
              </Text>
            </View>
          </AtCard>
        )}

        {/* 订单信息 */}
        <AtCard
          title='订单信息'
          className='info-card shadow-default'
        >
          <View className='info-item'>
            <Text className='info-label'>支付方式</Text>
            <Text className='info-value'>{PAYMENT_METHOD_MAP[order.paymentMethod] || order.paymentMethod}</Text>
          </View>
          <View className='info-item'>
            <Text className='info-label'>下单时间</Text>
            <Text className='info-value'>{order.createTime}</Text>
          </View>
          {order.deliveryTimeSlot && (
            <View className='info-item'>
              <Text className='info-label'>配送时段</Text>
              <Text className='info-value'>{DELIVERY_TIME_SLOT_MAP[order.deliveryTimeSlot] || order.deliveryTimeSlot}</Text>
            </View>
          )}
          {order.paymentTime && (
            <View className='info-item'>
              <Text className='info-label'>支付时间</Text>
              <Text className='info-value'>{order.paymentTime}</Text>
            </View>
          )}
          {order.completedTime && (
            <View className='info-item'>
              <Text className='info-label'>完成时间</Text>
              <Text className='info-value'>{order.completedTime}</Text>
            </View>
          )}
          {order.remark && (
            <View className='info-item'>
              <Text className='info-label'>订单备注</Text>
              <Text className='info-value'>{order.remark}</Text>
            </View>
          )}
        </AtCard>

        {/* 收货地址 */}
        <AtCard
          title='收货地址'
          className='address-card shadow-default'
        >
          <View className='address-content'>
            <View className='address-header'>
              <Text className='receiver-name'>{order.address.receiverName}</Text>
              <Text className='receiver-phone'>{order.address.phone}</Text>
            </View>
            <Text className='address-detail'>
              {order.address.province} {order.address.city} {order.address.district} {order.address.detailAddress}
            </Text>
          </View>
        </AtCard>

        {/* 底部操作按钮 */}
        <View className='bottom-actions shadow-action'>
          {order.status === 'pending_payment' && (
            <>
              <AtButton
                trackTarget="order-cancel"
                trackElementType="button"
                trackElementText="取消订单"
                trackProps={{ orderId: order.id }}
                onClick={this.handleCancelOrder}
                  type='secondary'
                  size='normal'
                  className='action-button'
                >
                  取消订单
                </AtButton>
              <AtButton
                trackTarget="order-pay"
                trackElementType="button"
                trackElementText="立即支付"
                trackProps={{ orderId: order.id }}
                onClick={this.handlePayment}
                  type='primary'
                  size='normal'
                  className='action-button primary'
                >
                  立即支付
                </AtButton>
            </>
          )}

          {order.status === 'delivering' && (
            <AtButton
              type='secondary'
              size='normal'
              onClick={this.handleTrackDelivery}
              className='action-button'
            >
              查看配送
            </AtButton>
          )}

          {order.status === 'completed' && (
            <>
              <AtButton
                trackTarget="order-review"
                trackElementType="button"
                trackElementText={reviewInfo?.hasReviewed ? '查看评价' : '评价'}
                trackProps={{ orderId: order.id }}
                onClick={this.handleReview}
                type='secondary'
                size='normal'
                className='action-button'
              >
                {reviewInfo?.hasReviewed ? '查看评价' : '评价'}
              </AtButton>
              <AtButton
                trackTarget="order-reorder"
                trackElementType="button"
                trackElementText="再次购买"
                trackProps={{ orderId: order.id }}
                onClick={this.handleReorder}
                  type='secondary'
                  size='normal'
                  className='action-button'
                >
                  再次购买
                </AtButton>
              <AtButton
                type='secondary'
                size='normal'
                onClick={() => Taro.switchTab({ url: '/pages/home/index' })}
                className='action-button'
              >
                继续购物
              </AtButton>
            </>
          )}

          {(order.status === 'pending_delivery' || order.status === 'cancelled') && (
            <AtButton
              type='secondary'
              size='normal'
              onClick={this.handleReorder}
              className='action-button full-width'
            >
              再次购买
            </AtButton>
          )}
        </View>

        {/* Toast 提示 */}
        <AtToast
          isOpened={showToast}
          text={toastText}
          status={toastType}
          onClose={this.hideToast}
        />

        {/* 底部安全区域 */}
        <View className='safe-bottom' />
      </View>
    )
  }
}
