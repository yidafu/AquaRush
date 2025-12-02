import { Component } from 'react'
import { View, Text, Image, Tabs, TabPane } from '@tarojs/components'
import { AtCard, AtIcon, AtButton, AtTabs, AtTabsPane, AtToast, AtBadge, AtTag } from 'taro-ui'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/tabs.scss"
import "taro-ui/dist/style/components/badge.scss"
import "taro-ui/dist/style/components/tag.scss"
import './index.scss'

interface OrderItem {
  id: string
  orderNo: string
  status: 'pending_payment' | 'pending_delivery' | 'delivering' | 'completed' | 'cancelled'
  items: Array<{
    id: string
    name: string
    image: string
    price: number
    quantity: number
    specifications?: string[]
  }>
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
  deliveryTime?: string
  createTime: string
  paymentTime?: string
  deliveryTime?: string
  completedTime?: string
  remark?: string
}

interface OrderListState {
  currentTab: number
  orders: OrderItem[]
  loading: boolean
  refreshing: boolean
  hasMore: boolean
  page: number
  showToast: boolean
  toastText: string
  toastType: 'success' | 'error' | 'loading'
}

const TAB_LIST = [
  { title: '全部', status: 'all' },
  { title: '待付款', status: 'pending_payment' },
  { title: '待配送', status: 'pending_delivery' },
  { title: '配送中', status: 'delivering' },
  { title: '已完成', status: 'completed' }
]

const STATUS_MAP = {
  pending_payment: { text: '待付款', color: '#ff6b35', bg: 'rgba(255, 107, 53, 0.1)' },
  pending_delivery: { text: '待配送', color: '#667eea', bg: 'rgba(102, 126, 234, 0.1)' },
  delivering: { text: '配送中', color: '#19be6b', bg: 'rgba(25, 190, 107, 0.1)' },
  completed: { text: '已完成', color: '#999', bg: 'rgba(153, 153, 153, 0.1)' },
  cancelled: { text: '已取消', color: '#ccc', bg: 'rgba(204, 204, 204, 0.1)' }
}

export default class OrderList extends Component<{}, OrderListState> {
  pageSize = 10

  constructor(props) {
    super(props)
    this.state = {
      currentTab: 0,
      orders: [],
      loading: false,
      refreshing: false,
      hasMore: true,
      page: 1,
      showToast: false,
      toastText: '',
      toastType: 'success'
    }
  }

  componentDidMount() {
    this.loadOrders(true)
  }

  componentDidShow() {
    // 页面显示时刷新第一页数据
    this.setState({ page: 1, hasMore: true }, () => {
      this.loadOrders(true)
    })
  }

  onPullDownRefresh = () => {
    this.setState({ refreshing: true, page: 1, hasMore: true }, () => {
      this.loadOrders(true)
    })
  }

  onReachBottom = () => {
    const { loading, hasMore, currentTab } = this.state

    if (!loading && hasMore && currentTab !== 0) {
      this.loadOrders(false)
    }
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

  loadOrders = async (refresh: boolean = false) => {
    try {
      const { currentTab, page } = this.state
      const status = TAB_LIST[currentTab].status

      this.setState({
        loading: true,
        refreshing: refresh
      })

      // TODO: 实际项目中这里应该调用获取订单列表的API
      // const result = await getOrders({
      //   status: status === 'all' ? undefined : status,
      //   page: refresh ? 1 : page,
      //   pageSize: this.pageSize
      // })

      // 模拟订单数据
      const mockOrders: OrderItem[] = this.generateMockOrders(status)

      // 过滤和分页处理
      const filteredOrders = status === 'all'
        ? mockOrders
        : mockOrders.filter(order => order.status === status)

      const startIndex = refresh ? 0 : (page - 1) * this.pageSize
      const endIndex = startIndex + this.pageSize
      const pageOrders = filteredOrders.slice(startIndex, endIndex)

      const newOrders = refresh ? pageOrders : [...this.state.orders, ...pageOrders]
      const hasMore = endIndex < filteredOrders.length

      this.setState({
        orders: newOrders,
        page: refresh ? 2 : page + 1,
        hasMore,
        loading: false,
        refreshing: false
      })
    } catch (error) {
      console.error('获取订单列表失败:', error)
      this.showToast('获取订单列表失败', 'error')
      this.setState({
        loading: false,
        refreshing: false
      })
    }
  }

  generateMockOrders = (status: string): OrderItem[] => {
    const baseOrders: OrderItem[] = [
      {
        id: '1',
        orderNo: 'ORD202412020001',
        status: 'pending_payment',
        items: [
          {
            id: '1',
            name: '农夫山泉 天然矿泉水 550ml',
            image: '/assets/product-water.jpg',
            price: 2.00,
            quantity: 2,
            specifications: ['24瓶装']
          }
        ],
        totalAmount: 48.00,
        deliveryFee: 0,
        finalAmount: 48.00,
        address: {
          receiverName: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detailAddress: '科技园南区深南大道9988号'
        },
        createTime: '2024-12-02 10:30:00'
      },
      {
        id: '2',
        orderNo: 'ORD202412020002',
        status: 'pending_delivery',
        items: [
          {
            id: '2',
            name: '怡宝 纯净水 380ml',
            image: '/assets/product-water.jpg',
            price: 1.50,
            quantity: 3,
            specifications: ['36瓶装']
          }
        ],
        totalAmount: 162.00,
        deliveryFee: 0,
        finalAmount: 162.00,
        address: {
          receiverName: '李四',
          phone: '13900139000',
          province: '广东省',
          city: '深圳市',
          district: '福田区',
          detailAddress: '华强北电子世界1栋'
        },
        createTime: '2024-12-02 09:15:00',
        paymentTime: '2024-12-02 09:20:00'
      },
      {
        id: '3',
        orderNo: 'ORD202412010001',
        status: 'delivering',
        items: [
          {
            id: '3',
            name: '娃哈哈 AD钙奶 596ml',
            image: '/assets/product-water.jpg',
            price: 2.50,
            quantity: 4,
            specifications: ['12瓶装']
          }
        ],
        totalAmount: 120.00,
        deliveryFee: 0,
        finalAmount: 120.00,
        address: {
          receiverName: '王五',
          phone: '13700137000',
          province: '广东省',
          city: '深圳市',
          district: '宝安区',
          detailAddress: '宝安中心区新安街道'
        },
        createTime: '2024-12-01 14:30:00',
        paymentTime: '2024-12-01 14:35:00',
        deliveryTime: '2024-12-01 15:00:00'
      },
      {
        id: '4',
        orderNo: 'ORD202411300001',
        status: 'completed',
        items: [
          {
            id: '4',
            name: '农夫山泉 天然矿泉水 550ml',
            image: '/assets/product-water.jpg',
            price: 2.00,
            quantity: 1,
            specifications: ['24瓶装']
          }
        ],
        totalAmount: 48.00,
        deliveryFee: 0,
        finalAmount: 48.00,
        address: {
          receiverName: '张三',
          phone: '13800138000',
          province: '广东省',
          city: '深圳市',
          district: '南山区',
          detailAddress: '科技园南区深南大道9988号'
        },
        createTime: '2024-11-30 10:00:00',
        paymentTime: '2024-11-30 10:05:00',
        deliveryTime: '2024-11-30 10:30:00',
        completedTime: '2024-11-30 11:00:00'
      }
    ]

    return baseOrders
  }

  handleTabChange = (value: number) => {
    this.setState({
      currentTab: value,
      page: 1,
      hasMore: true,
      orders: []
    }, () => {
      this.loadOrders(true)
    })
  }

  handleOrderClick = (order: OrderItem) => {
    Taro.navigateTo({
      url: `/pages/order-detail/index?orderId=${order.id}`
    })
  }

  handlePayment = (order: OrderItem) => {
    // 跳转到支付页面
    Taro.navigateTo({
      url: `/pages/payment/index?orderId=${order.id}&amount=${order.finalAmount}`
    })
  }

  handleCancelOrder = (order: OrderItem) => {
    Taro.showModal({
      title: '确认取消',
      content: '确定要取消这个订单吗？取消后无法恢复。',
      success: async (res) => {
        if (res.confirm) {
          try {
            // TODO: 实际项目中这里应该调用取消订单的API
            // await cancelOrder(order.id)

            this.showToast('订单已取消', 'success')

            // 刷新订单列表
            this.loadOrders(true)
          } catch (error) {
            console.error('取消订单失败:', error)
            this.showToast('取消订单失败', 'error')
          }
        }
      }
    })
  }

  handleConfirmReceive = (order: OrderItem) => {
    Taro.showModal({
      title: '确认收货',
      content: '确定已收到商品吗？',
      success: async (res) => {
        if (res.confirm) {
          try {
            // TODO: 实际项目中这里应该调用确认收货的API
            // await confirmReceive(order.id)

            this.showToast('确认收货成功', 'success')

            // 刷新订单列表
            this.loadOrders(true)
          } catch (error) {
            console.error('确认收货失败:', error)
            this.showToast('确认收货失败', 'error')
          }
        }
      }
    })
  }

  handleDeleteOrder = (order: OrderItem) => {
    Taro.showModal({
      title: '确认删除',
      content: '确定要删除这个订单吗？删除后无法恢复。',
      success: async (res) => {
        if (res.confirm) {
          try {
            // TODO: 实际项目中这里应该调用删除订单的API
            // await deleteOrder(order.id)

            this.showToast('订单已删除', 'success')

            // 刷新订单列表
            this.loadOrders(true)
          } catch (error) {
            console.error('删除订单失败:', error)
            this.showToast('删除订单失败', 'error')
          }
        }
      }
    })
  }

  handleReorder = (order: OrderItem) => {
    // 将订单商品添加到购物车或直接跳转到订单确认页面
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

  renderOrderItem = (order: OrderItem) => {
    const status = STATUS_MAP[order.status] || STATUS_MAP.cancelled

    return (
      <AtCard key={order.id} className='order-card'>
        <View className='order-header'>
          <View className='order-info'>
            <Text className='order-no'>订单号：{order.orderNo}</Text>
            <AtTag
              type='primary'
              size='small'
              circle
              customStyle={{
                background: status.bg,
                color: status.color,
                border: 'none'
              }}
            >
              {status.text}
            </AtTag>
          </View>
          <Text className='order-time'>{order.createTime}</Text>
        </View>

        <View className='order-items'>
          {order.items.map((item, index) => (
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

        <View className='order-address'>
          <AtIcon value='map-pin' size='14' color='#999' />
          <Text className='address-text'>
            {order.address.receiverName} {order.address.phone}
            {order.address.province} {order.address.city} {order.address.district} {order.address.detailAddress}
          </Text>
        </View>

        <View className='order-footer'>
          <View className='order-amount'>
            <Text className='amount-label'>实付：</Text>
            <Text className='amount-value'>¥{order.finalAmount.toFixed(2)}</Text>
          </View>

          <View className='order-actions'>
            {order.status === 'pending_payment' && (
              <>
                <AtButton
                  type='secondary'
                  size='small'
                  onClick={() => this.handleCancelOrder(order)}
                  className='action-button'
                >
                  取消订单
                </AtButton>
                <AtButton
                  type='primary'
                  size='small'
                  onClick={() => this.handlePayment(order)}
                  className='action-button'
                >
                  立即支付
                </AtButton>
              </>
            )}

            {order.status === 'pending_delivery' && (
              <>
                <AtButton
                  type='secondary'
                  size='small'
                  onClick={() => this.handleOrderClick(order)}
                  className='action-button'
                >
                  查看详情
                </AtButton>
                <AtButton
                  type='secondary'
                  size='small'
                  onClick={() => this.handleCancelOrder(order)}
                  className='action-button'
                >
                  取消订单
                </AtButton>
              </>
            )}

            {order.status === 'delivering' && (
              <>
                <AtButton
                  type='secondary'
                  size='small'
                  onClick={() => this.handleOrderClick(order)}
                  className='action-button'
                >
                  查看详情
                </AtButton>
                <AtButton
                  type='primary'
                  size='small'
                  onClick={() => this.handleConfirmReceive(order)}
                  className='action-button'
                >
                  确认收货
                </AtButton>
              </>
            )}

            {order.status === 'completed' && (
              <>
                <AtButton
                  type='secondary'
                  size='small'
                  onClick={() => this.handleReorder(order)}
                  className='action-button'
                >
                  再次购买
                </AtButton>
                <AtButton
                  type='secondary'
                  size='small'
                  onClick={() => this.handleDeleteOrder(order)}
                  className='action-button'
                >
                  删除订单
                </AtButton>
              </>
            )}
          </View>
        </View>
      </AtCard>
    )
  }

  render() {
    const { currentTab, orders, loading, hasMore, showToast, toastText, toastType } = this.state

    return (
      <View className='order-list-page'>
        <AtTabs
          current={currentTab}
          tabList={TAB_LIST}
          onClick={this.handleTabChange}
          swipeable={false}
          className='order-tabs'
        />

        <View className='order-content'>
          {loading && orders.length === 0 ? (
            <View className='loading-container'>
              <Text>加载中...</Text>
            </View>
          ) : orders.length === 0 ? (
            <View className='empty-container'>
              <Image
                src='/assets/empty-order.png'
                mode='aspectFit'
                className='empty-image'
              />
              <Text className='empty-text'>暂无订单</Text>
              <Text className='empty-desc'>
                {currentTab === 0 ? '您还没有任何订单哦' : `暂无${TAB_LIST[currentTab].title}订单`}
              </Text>
              {currentTab === 0 && (
                <AtButton
                  type='primary'
                  size='normal'
                  onClick={() => Taro.switchTab({ url: '/pages/home/index' })}
                  className='go-shopping-button'
                >
                  去逛逛
                </AtButton>
              )}
            </View>
          ) : (
            <View className='order-list'>
              {orders.map(order => this.renderOrderItem(order))}

              {loading && (
                <View className='loading-more'>
                  <Text>加载中...</Text>
                </View>
              )}

              {!hasMore && orders.length > 0 && (
                <View className='no-more'>
                  <Text>没有更多订单了</Text>
                </View>
              )}
            </View>
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