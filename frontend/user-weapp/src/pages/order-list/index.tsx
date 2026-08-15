import { Component } from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtCard, AtIcon, AtButton, AtTabs, AtToast, AtTag } from 'taro-ui'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/tabs.scss"
import "taro-ui/dist/style/components/tag.scss"
import './index.scss'
import { Money, OrderStatus, normalizeCents } from '@aquarush/common'
import { userActionTracker } from '../../utils/tracking'
import OrderService from '../../services/OrderService'

interface OrderProduct {
  id: string
  name: string
  image: string
  priceCents: string | number
  specification?: string
}

interface OrderItem {
  id: string
  orderNo: string
  status: OrderStatus
  /** 首件商品(F-ORD-004:列表卡片展示首件+「等N件」) */
  product: OrderProduct | null
  /** 全部商品明细(F-ORD-004 多商品下单) */
  items: OrderProduct[]
  quantity: number
  // 营销计价快照(R-MKT-01/02/07):优惠价+划线价/优惠明细展示依据
  unitPriceCents: string | number | null
  priceSource: string | null
  amountCents: string | number
  deliveryFeeCents: string | number
  payAmountCents: string | number
  promotionDiscountCents: string | number | null
  couponDiscountCents: string | number | null
  firstOrderDiscountCents: string | number | null
  address: {
    receiverName: string
    phone: string
    province: string
    city: string
    district: string
    detailAddress: string
  } | null
  createTime: string
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
  { title: '全部', statuses: null as OrderStatus[] | null },
  { title: '待付款', statuses: [OrderStatus.PENDING_PAYMENT] },
  // 「待发货」口径=待派单+待配送(02-business-flows.md §订单状态机)
  { title: '待配送', statuses: [OrderStatus.PENDING_DISPATCH, OrderStatus.PENDING_DELIVERY] },
  { title: '配送中', statuses: [OrderStatus.DELIVERING] },
  { title: '已完成', statuses: [OrderStatus.COMPLETED] }
]

const STATUS_MAP: Record<string, { text: string; color: string; bg: string }> = {
  [OrderStatus.PENDING_PAYMENT]: { text: '待付款', color: '#ff6b35', bg: 'rgba(255, 107, 53, 0.1)' },
  [OrderStatus.PENDING_DISPATCH]: { text: '待配送', color: '#667eea', bg: 'rgba(102, 126, 234, 0.1)' },
  [OrderStatus.PENDING_DELIVERY]: { text: '待配送', color: '#667eea', bg: 'rgba(102, 126, 234, 0.1)' },
  [OrderStatus.DELIVERING]: { text: '配送中', color: '#19be6b', bg: 'rgba(25, 190, 107, 0.1)' },
  [OrderStatus.COMPLETED]: { text: '已完成', color: '#999', bg: 'rgba(153, 153, 153, 0.1)' },
  [OrderStatus.CANCELLED]: { text: '已取消', color: '#ccc', bg: 'rgba(204, 204, 204, 0.1)' },
  [OrderStatus.REFUNDED]: { text: '已退款', color: '#ccc', bg: 'rgba(204, 204, 204, 0.1)' }
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
      page: 0,
      showToast: false,
      toastText: '',
      toastType: 'success'
    }
  }

  componentDidMount() {
    this.loadOrders(true)
  }

  componentDidShow() {
    userActionTracker.trackPageView()
    // 页面显示时刷新第一页数据(支付完成/取消后返回即时同步)
    this.setState({ page: 0, hasMore: true }, () => {
      this.loadOrders(true)
    })
  }

  onPullDownRefresh = () => {
    this.setState({ refreshing: true, page: 0, hasMore: true }, () => {
      this.loadOrders(true)
    })
  }

  onReachBottom = () => {
    const { loading, hasMore } = this.state

    if (!loading && hasMore) {
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
      const statuses = TAB_LIST[currentTab].statuses
      const targetPage = refresh ? 0 : page

      this.setState({
        loading: true,
        refreshing: refresh
      })

      const result = await OrderService.getInstance().getMyOrders(statuses, targetPage, this.pageSize)
      if (!result.success || !result.data) {
        throw new Error(result.error?.message || '获取订单列表失败')
      }

      const { content, totalPages } = result.data
      const newOrders: OrderItem[] = content.map(order => this.mapOrder(order))
      const mergedOrders = refresh ? newOrders : [...this.state.orders, ...newOrders]

      this.setState({
        orders: mergedOrders,
        page: targetPage + 1,
        hasMore: targetPage + 1 < totalPages,
        loading: false,
        refreshing: false
      })
    } catch (error) {
      console.error('获取订单列表失败:', error)
      this.showToast(error instanceof Error ? error.message : '获取订单列表失败', 'error')
      this.setState({
        loading: false,
        refreshing: false
      })
    }
  }

  /**
   * GraphQL Order → 页面订单结构(金额均为分,Long 字符串经 normalizeCents 归一化)
   */
  mapOrder = (order: any): OrderItem => {
    const rawItems: any[] = Array.isArray(order.items) && order.items.length > 0 ? order.items : []
    const items: OrderProduct[] = rawItems.map((item: any) => ({
      id: productId(item.product || { id: item.productId }),
      name: item.product?.name || '商品',
      image: item.product?.coverImageUrl || '/assets/default-product.png',
      priceCents: item.unitPriceCents ?? item.product?.priceCents ?? 0,
      specification: item.product?.specification
    }))
    const product = items[0] || null

    return {
      id: order.id,
      orderNo: order.orderNo,
      status: order.status as OrderStatus,
      product,
      items,
      quantity: order.items && order.items.length > 0 ? order.items[0].quantity : 0,
      unitPriceCents: items[0]?.priceCents ?? null,
      priceSource: 'CATALOG_PRICE',
      amountCents: order.amountCents,
      deliveryFeeCents: order.deliveryFeeCents,
      payAmountCents: order.payAmountCents,
      promotionDiscountCents: order.promotionDiscountCents ?? null,
      couponDiscountCents: order.couponDiscountCents ?? null,
      firstOrderDiscountCents: order.firstOrderDiscountCents ?? null,
      address: order.address
        ? {
            receiverName: order.address.receiverName,
            phone: order.address.phone,
            province: order.address.province,
            city: order.address.city,
            district: order.address.district,
            detailAddress: order.address.detailAddress
          }
        : null,
      createTime: order.createdAt || ''
    }
  }

  /**
   * 订单层总优惠(分)= 满减/折扣 + 优惠券 + 首单立减(R-MKT-07 叠加)
   */
  totalDiscount = (order: OrderItem): number =>
    (normalizeCents(order.promotionDiscountCents) ?? 0) +
    (normalizeCents(order.couponDiscountCents) ?? 0) +
    (normalizeCents(order.firstOrderDiscountCents) ?? 0)

  /**
   * 是否命中营销价(有优惠价时划线目录价,AC-ORD-002-09)
   */
  hasMarketingPrice = (order: OrderItem): boolean =>
    order.priceSource !== null && order.priceSource !== 'CATALOG_PRICE' && order.unitPriceCents != null

  handleTabChange = (value: number) => {
    this.setState({
      currentTab: value,
      page: 0,
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

  handlePayment = async (order: OrderItem) => {
    this.showToast('正在发起支付...', 'loading')

    const payResult = await OrderService.getInstance().createOrderPayment(order.id)
    if (!payResult.success || !payResult.data) {
      this.showToast(payResult.error?.message || '支付发起失败', 'error')
      return
    }
    const { autoCompleted, requiresSimulation, paymentParams } = payResult.data

    // 测试环境假支付:服务端已自动完成,直接刷新订单列表
    if (autoCompleted) {
      this.showToast('支付成功（模拟支付自动完成）', 'success')
      setTimeout(() => this.loadOrders(true), 800)
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
      setTimeout(() => this.loadOrders(true), 800)
    } catch (e) {
      console.error('微信支付未完成:', e)
      this.showToast('支付未完成，请重试', 'error')
    }
  }

  handleCancelOrder = (order: OrderItem) => {
    Taro.showModal({
      title: '确认取消',
      content: '确定要取消这个订单吗？取消后无法恢复。',
      success: async (res) => {
        if (res.confirm) {
          try {
            const result = await OrderService.getInstance().cancelOrder(order.id)
            if (!result.success) {
              throw new Error(result.error?.message || '取消订单失败')
            }
            this.showToast('订单已取消', 'success')

            // 刷新订单列表
            this.loadOrders(true)
          } catch (error) {
            console.error('取消订单失败:', error)
            this.showToast(error instanceof Error ? error.message : '取消订单失败', 'error')
          }
        }
      }
    })
  }

  handleReorder = (order: OrderItem) => {
    if (!order.product) {
      this.showToast('订单商品信息缺失', 'error')
      return
    }
    // 写入 pendingOrder 并跳转确认订单页(F-ORD-004:多商品全部明细再次购买)
    const products = order.items.length > 0
      ? order.items.map((p, idx) => ({
          id: p.id,
          name: p.name,
          price: normalizeCents(idx === 0 ? order.unitPriceCents : p.priceCents) ?? 0,
          quantity: idx === 0 ? order.quantity : 1,
          image: p.image,
          specifications: p.specification ? [p.specification] : []
        }))
      : [{
          id: order.product.id,
          name: order.product.name,
          price: normalizeCents(order.unitPriceCents) ?? 0,
          quantity: order.quantity,
          image: order.product.image,
          specifications: order.product.specification ? [order.product.specification] : []
        }]
    const orderData = {
      products,
      totalAmount: normalizeCents(order.payAmountCents) ?? 0
    }

    Taro.setStorageSync('pendingOrder', orderData)

    Taro.navigateTo({
      url: '/pages/order-confirm/index'
    })
  }

  renderOrderItem = (order: OrderItem) => {
    const status = STATUS_MAP[order.status] || STATUS_MAP[OrderStatus.CANCELLED]
    const discount = this.totalDiscount(order)
    const showMarketingPrice = this.hasMarketingPrice(order)

    return (
      <AtCard key={order.id} className='order-card' onClick={() => this.handleOrderClick(order)}>
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

        {order.product && (
          <View className='order-items'>
            <View className='order-item'>
              {order.items.length > 1 && (
                <Text className='item-more'>等{order.items.length}件</Text>
              )}
              <Image
                src={order.product.image}
                mode='aspectFill'
                className='item-image'
              />
              <View className='item-info'>
                <Text className='item-name'>{order.product.name}</Text>
                {order.product.specification && (
                  <Text className='item-spec'>
                    规格：{order.product.specification}
                  </Text>
                )}
                <View className='item-bottom'>
                  {showMarketingPrice ? (
                    <>
                      <Text className='item-price'>
                        <Money cents={order.unitPriceCents} />
                      </Text>
                      <Text className='item-original-price'>
                        <Money cents={order.product.priceCents} />
                      </Text>
                    </>
                  ) : (
                    <Text className='item-price'>
                      <Money cents={order.unitPriceCents ?? order.product.priceCents} />
                    </Text>
                  )}
                  <Text className='item-quantity'>x{order.quantity}</Text>
                </View>
              </View>
            </View>
          </View>
        )}

        {order.address && (
          <View className='order-address'>
            <AtIcon value='map-pin' size='14' color='#999' />
            <Text className='address-text'>
              {order.address.receiverName} {order.address.phone}
              {order.address.province} {order.address.city} {order.address.district} {order.address.detailAddress}
            </Text>
          </View>
        )}

        <View className='order-footer'>
          <View className='order-amount'>
            {discount > 0 && (
              <Text className='amount-discount'>已优惠 <Money cents={discount} /></Text>
            )}
            <Text className='amount-label'>实付：</Text>
            <Text className='amount-value'><Money cents={order.payAmountCents} /></Text>
          </View>

          <View className='order-actions'>
            {order.status === OrderStatus.PENDING_PAYMENT && (
              <>
                <AtButton
                  trackTarget="order-cancel"
                  trackElementType="button"
                  trackElementText="取消订单"
                  trackProps={{ orderId: order.id }}
                  onClick={() => this.handleCancelOrder(order)}
                  type='secondary'
                  size='small'
                  className='action-button'
                >
                    取消订单
                </AtButton>
                <AtButton
                  trackTarget="order-pay"
                  trackElementType="button"
                  trackElementText="立即支付"
                  trackProps={{ orderId: order.id }}
                  onClick={() => this.handlePayment(order)}
                  type='primary'
                  size='small'
                  className='action-button'
                >
                    立即支付
                </AtButton>
              </>
            )}

            {(order.status === OrderStatus.PENDING_DISPATCH || order.status === OrderStatus.PENDING_DELIVERY) && (
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

            {order.status === OrderStatus.DELIVERING && (
              <AtButton
                type='secondary'
                size='small'
                onClick={() => this.handleOrderClick(order)}
                className='action-button'
              >
                查看详情
              </AtButton>
            )}

            {order.status === OrderStatus.COMPLETED && (
              <AtButton
                trackTarget="order-reorder"
                trackElementType="button"
                trackElementText="再次购买"
                trackProps={{ orderId: order.id }}
                onClick={() => this.handleReorder(order)}
                type='secondary'
                size='small'
                className='action-button'
              >
                再次购买
              </AtButton>
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

/**
 * 商品 id 归一化(PrimaryId 传输为字符串)
 */
function productId(product: any): string {
  return String(product.id)
}