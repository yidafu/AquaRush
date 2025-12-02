import { Component } from 'react'
import { View, Text, Image, Button, Progress, Steps } from '@tarojs/components'
import { AtCard, AtIcon, AtButton, AtToast, AtTag, AtTimeline, AtProgress } from 'taro-ui'
import Taro from '@tarojs/taro'

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
  price: number
  quantity: number
  specifications?: string[]
}

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
    createTime: string
    paymentTime?: string
    acceptTime?: string
    deliveryTime?: string
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
}

const STATUS_MAP = {
  pending_payment: { text: '待付款', color: '#ff6b35', step: 1 },
  pending_delivery: { text: '待配送', color: '#667eea', step: 2 },
  delivering: { text: '配送中', color: '#19be6b', step: 3 },
  completed: { text: '已完成', color: '#999', step: 4 },
  cancelled: { text: '已取消', color: '#ccc', step: 0 }
}

const PAYMENT_METHOD_MAP = {
  wechat: '微信支付',
  balance: '余额支付'
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
      refreshing: false
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

      // TODO: 实际项目中这里应该调用获取订单详情的API
      // const order = await getOrderDetail(this.orderId)

      // 模拟订单详情数据
      const mockOrder = {
        id: this.orderId,
        orderNo: 'ORD202412020001',
        status: 'delivering',
        items: [
          {
            id: '1',
            name: '农夫山泉 天然矿泉水 550ml',
            image: '/assets/product-water.jpg',
            price: 2.00,
            quantity: 2,
            specifications: ['24瓶装']
          },
          {
            id: '2',
            name: '怡宝 纯净水 380ml',
            image: '/assets/product-water.jpg',
            price: 1.50,
            quantity: 3,
            specifications: ['36瓶装']
          }
        ],
        totalAmount: 87.00,
        deliveryFee: 0,
        finalAmount: 87.00,
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
        deliveryTime: '2024-12-02 10:50:00',
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
            color: '#667eea'
          },
          {
            time: '2024-12-02 10:35:00',
            title: '支付成功',
            content: '使用微信支付成功',
            icon: 'credit-card',
            color: '#ff6b35'
          },
          {
            time: '2024-12-02 10:45:00',
            title: '商家接单',
            content: '商家已接单，正在准备商品',
            icon: 'shopping-bag',
            color: '#667eea'
          },
          {
            time: '2024-12-02 10:50:00',
            title: '配送员取货',
            content: '配送员小李已取货，正在配送中',
            icon: 'truck',
            color: '#19be6b'
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

  handlePayment = () => {
    const { order } = this.state
    if (!order) return

    // 跳转到支付页面
    Taro.navigateTo({
      url: `/pages/payment/index?orderId=${order.id}&amount=${order.finalAmount}`
    })
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
            // TODO: 实际项目中这里应该调用取消订单的API
            // await cancelOrder(order.id)

            this.showToast('订单已取消', 'success')

            // 刷新订单详情
            this.loadOrderDetail()
          } catch (error) {
            console.error('取消订单失败:', error)
            this.showToast('取消订单失败', 'error')
          }
        }
      }
    })
  }

  handleContactDelivery = () => {
    const { order } = this.state
    if (!order || !order.deliveryInfo?.deliveryWorkerPhone) return

    Taro.makePhoneCall({
      phoneNumber: order.deliveryInfo.deliveryWorkerPhone
    })
  }

  handleConfirmReceive = () => {
    const { order } = this.state
    if (!order) return

    Taro.showModal({
      title: '确认收货',
      content: '确定已收到商品吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            // TODO: 实际项目中这里应该调用确认收货的API
            // await confirmReceive(order.id)

            this.showToast('确认收货成功', 'success')

            // 刷新订单详情
            this.loadOrderDetail()
          } catch (error) {
            console.error('确认收货失败:', error)
            this.showToast('确认收货失败', 'error')
          }
        }
      }
    })
  }

  handleReorder = () => {
    const { order } = this.state
    if (!order) return

    // 将订单商品添加到订单确认页面
    const orderData = {
      products: order.items.map(item => ({
        id: item.id,
        name: item.name,
        price: item.price,
        quantity: item.quantity,
        image: item.image,
        specifications: item.specifications
      })),
      totalAmount: order.totalAmount
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

  renderOrderItems = (items: OrderItem[]) => {
    return (
      <View className='order-items'>
        {items.map((item, index) => (
          <View key={`${item.id}-${index}`} className='order-item'>
            <Image
              src={item.image}
              mode='aspectFill'
              className='item-image'
            />
            <View className='item-info'>
              <Text className='item-name'>{item.name}</Text>
              {item.specifications && item.specifications.length > 0 && (
                <Text className='item-spec'>
                  规格：{item.specifications.join(', ')}
                </Text>
              )}
              <View className='item-bottom'>
                <Text className='item-price'>¥{item.price.toFixed(2)}</Text>
                <Text className='item-quantity'>x{item.quantity}</Text>
              </View>
            </View>
          </View>
        ))}
      </View>
    )
  }

  render() {
    const { order, loading, showToast, toastText, toastType } = this.state

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
            className='delivery-info-card'
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
        {order.timeline && order.timeline.length > 0 && (
          <AtCard
            title='订单跟踪'
            className='timeline-card'
          >
            <AtTimeline items={order.timeline} />
          </AtCard>
        )}

        {/* 商品信息 */}
        <AtCard
          title='商品信息'
          className='items-card'
        >
          {this.renderOrderItems(order.items)}
          <View className='order-summary'>
            <View className='summary-item'>
              <Text className='summary-label'>商品总价</Text>
              <Text className='summary-value'>¥{order.totalAmount.toFixed(2)}</Text>
            </View>
            <View className='summary-item'>
              <Text className='summary-label'>配送费</Text>
              <Text className='summary-value'>
                {order.deliveryFee === 0 ? '免配送费' : `¥${order.deliveryFee.toFixed(2)}`}
              </Text>
            </View>
            <View className='summary-item total'>
              <Text className='summary-label'>实付金额</Text>
              <Text className='summary-value total-amount'>¥{order.finalAmount.toFixed(2)}</Text>
            </View>
          </View>
        </AtCard>

        {/* 订单信息 */}
        <AtCard
          title='订单信息'
          className='info-card'
        >
          <View className='info-item'>
            <Text className='info-label'>支付方式</Text>
            <Text className='info-value'>{PAYMENT_METHOD_MAP[order.paymentMethod] || order.paymentMethod}</Text>
          </View>
          <View className='info-item'>
            <Text className='info-label'>下单时间</Text>
            <Text className='info-value'>{order.createTime}</Text>
          </View>
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
          className='address-card'
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
        <View className='bottom-actions'>
          {order.status === 'pending_payment' && (
            <>
              <AtButton
                type='secondary'
                size='normal'
                onClick={this.handleCancelOrder}
                className='action-button'
              >
                取消订单
              </AtButton>
              <AtButton
                type='primary'
                size='normal'
                onClick={this.handlePayment}
                className='action-button primary'
              >
                立即支付
              </AtButton>
            </>
          )}

          {order.status === 'delivering' && (
            <>
              <AtButton
                type='secondary'
                size='normal'
                onClick={this.handleTrackDelivery}
                className='action-button'
              >
                查看配送
              </AtButton>
              <AtButton
                type='primary'
                size='normal'
                onClick={this.handleConfirmReceive}
                className='action-button primary'
              >
                确认收货
              </AtButton>
            </>
          )}

          {order.status === 'completed' && (
            <>
              <AtButton
                type='secondary'
                size='normal'
                onClick={this.handleReorder}
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