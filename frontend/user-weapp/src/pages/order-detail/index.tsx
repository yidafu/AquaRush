import React, { useCallback, useEffect, useRef, useState } from 'react'
import { View, Text } from '@tarojs/components'
import { AtButton, AtToast } from 'taro-ui'
import Taro, { useReady, useDidShow, usePullDownRefresh } from '@tarojs/taro'
import { DeliverySlotVo, normalizeCents } from '@aquarush/common'
import OrderService from '../../services/OrderService'
import ConfigService from '../../services/ConfigService'
import { userActionTracker } from '../../utils/tracking'
import { buildOrderDetailCostRows } from '../../utils/orderCost'
import { DEFAULT_DELIVERY_SLOTS } from '../../utils/deliverySlots'
import { buildMockOrder } from './utils/orderDetailMock'

// 子组件
import OrderStatusHeader from './components/OrderStatusHeader'
import DeliveryWorkerCard from './components/DeliveryWorkerCard'
import OrderTimelineCard from './components/OrderTimelineCard'
import OrderItemsCard from './components/OrderItemsCard'
import OrderReviewCard from './components/OrderReviewCard'
import OrderInfoCard from './components/OrderInfoCard'
import OrderAddressCard from './components/OrderAddressCard'
import OrderActions from './components/OrderActions'
import type { OrderDetail, ReviewCheckState } from './components/constants'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/toast.scss"
import "taro-ui/dist/style/components/tag.scss"
import "taro-ui/dist/style/components/timeline.scss"
import "taro-ui/dist/style/components/progress.scss"
import './index.scss'

/**
 * 订单详情页(F-ORD-003)
 * 页面仅保留数据加载/映射与事件处理,渲染全部由 components/ 子组件完成;
 * 已完成订单额外加载评价信息(F-ORD-007);配送时段配置(F-SYS-002)并行加载不阻塞详情。
 */
const OrderDetailPage: React.FC = () => {
  const [order, setOrder] = useState<OrderDetail | null>(null)
  const [loading, setLoading] = useState<boolean>(true)
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  // 评价信息(F-ORD-007):已完成订单的评价入口/已评内容
  const [reviewInfo, setReviewInfo] = useState<ReviewCheckState | null>(null)
  // 配送时段配置(F-SYS-002:全量含已停用,历史订单时段文案按配置渲染,兜底见 DELIVERY_TIME_SLOT_MAP)
  const [deliverySlots, setDeliverySlots] = useState<DeliverySlotVo[]>(DEFAULT_DELIVERY_SLOTS)

  const orderIdRef = useRef<string>('')
  const loadingRef = useRef<boolean>(true)

  useEffect(() => {
    loadingRef.current = loading
  }, [loading])

  const showToastMessage = useCallback((text: string, type: 'success' | 'error' | 'loading' = 'success') => {
    setShowToast(true)
    setToastText(text)
    setToastType(type)
  }, [])

  const hideToast = useCallback(() => {
    setShowToast(false)
  }, [])

  const loadDeliverySlots = useCallback(async () => {
    try {
      const result = await ConfigService.getInstance().getDeliverySlots()
      if (result.success && result.data && result.data.length > 0) {
        setDeliverySlots(result.data)
      }
    } catch (error) {
      console.error('获取配送时段配置失败:', error)
    }
  }, [])

  /**
   * GraphQL OrderStatus → 页面状态键
   */
  const mapStatus = useCallback((status: string): OrderDetail['status'] => {
    const map: Record<string, OrderDetail['status']> = {
      PENDING_PAYMENT: 'pending_payment',
      PENDING_DELIVERY: 'pending_delivery',
      PENDING_DISPATCH: 'pending_delivery',
      DELIVERING: 'delivering',
      COMPLETED: 'completed',
      CANCELLED: 'cancelled',
      REFUNDED: 'cancelled'
    }
    return map[status] || 'pending_payment'
  }, [])

  /**
   * 真实订单 → 页面订单结构(F-ORD-004 多商品:商品信息来自 order.items 随单快照,金额均为分)
   */
  const mapRealOrder = useCallback((order: any): OrderDetail => {
    const rawItems: any[] = Array.isArray(order.items) && order.items.length > 0 ? order.items : []
    const items = rawItems.map((item: any) => {
      // 成交单价(分,快照);目录价(分,商品快照) —— 划线价判定:成交单价 ≠ 目录价即命中营销价(AC-ORD-003-09,R-MKT-01/02)
      const unitPriceCents = normalizeCents(item.unitPriceCents) ?? normalizeCents(item.product?.priceCents) ?? 0
      const catalogPriceCents = normalizeCents(item.product?.priceCents) ?? unitPriceCents
      return {
        id: item.productId,
        name: item.product?.name || '商品',
        image: item.product?.coverImageUrl || '/assets/default-product.png',
        priceCents: unitPriceCents,
        originalPriceCents: unitPriceCents !== catalogPriceCents ? catalogPriceCents : undefined,
        hasMarketingPrice: unitPriceCents !== catalogPriceCents,
        quantity: item.quantity,
        specifications: item.product?.specification ? [item.product.specification] : []
      }
    })

    const status = mapStatus(order.status)
    const timeline: OrderDetail['timeline'] = []
    if (order.createdAt) {
      timeline.push({ time: order.createdAt, title: '订单创建', content: '您的订单已创建', icon: 'bookmark', color: 'var(--brand-primary)' })
    }
    if (order.paymentTime) {
      timeline.push({ time: order.paymentTime, title: '支付成功', content: '使用微信支付成功', icon: 'credit-card', color: 'var(--action)' })
    }
    if (order.deliveryStartedAt) {
      timeline.push({ time: order.deliveryStartedAt, title: '开始配送', content: '配送员正在配送中', icon: 'truck', color: 'var(--success)' })
    }
    if (order.completedAt) {
      timeline.push({ time: order.completedAt, title: '订单完成', content: '订单已完成', icon: 'check-circle', color: 'var(--success)' })
    }

    return {
      id: order.id,
      orderNo: order.orderNo,
      status,
      items,
      totalAmount: order.amountCents,
      deliveryFee: order.deliveryFeeCents,
      finalAmount: order.payAmountCents,
      // 营销计价快照(分,费用明细行展示依据 —— R-MKT-02/07/08/09/10)
      subtotalCents: items.reduce((sum, item) => sum + item.priceCents * item.quantity, 0),
      promotionDiscountCents: normalizeCents(order.promotionDiscountCents) ?? 0,
      promotionActivityName: order.promotionActivityName || undefined,
      firstOrderDiscountCents: normalizeCents(order.firstOrderDiscountCents) ?? 0,
      firstOrderActivityName: order.firstOrderActivityName || undefined,
      couponDiscountCents: normalizeCents(order.couponDiscountCents) ?? 0,
      couponName: order.couponName || undefined,
      ticketRedeemCount: order.ticketRedeemCount ?? 0,
      ticketDiscountCents: normalizeCents(order.ticketDiscountCents) ?? 0,
      // 水桶费快照(历史字段,2026-08 水桶费改造后新订单恒为 0,仅旧订单有值,见 14-misc.md §G06)
      depositCents: normalizeCents(order.depositCents) ?? 0,
      giftItems: Array.isArray(order.giftItems)
        ? order.giftItems.map((gift: any) => ({
            productId: String(gift.productId),
            productName: gift.productName || '赠品',
            quantity: gift.quantity ?? 0
          }))
        : [],
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
  }, [mapStatus])

  const loadReviewCheck = useCallback(async (orderId: string) => {
    try {
      const result = await OrderService.getInstance().getReviewCheck(orderId)
      if (result.success && result.data) {
        setReviewInfo(result.data)
      }
    } catch (error) {
      console.error('获取评价信息失败:', error)
    }
  }, [])

  const loadOrderDetail = useCallback(async () => {
    const orderId = orderIdRef.current
    try {
      setLoading(true)

      // 优先加载真实订单数据(F-ORD-003);失败回退模拟数据(离线/演示场景)
      const result = await OrderService.getInstance().getOrder(orderId)
      if (result.success && result.data) {
        const mapped = mapRealOrder(result.data)
        setOrder(mapped)
        // 已完成订单:加载评价信息(F-ORD-007)
        if (mapped.status === 'completed') {
          loadReviewCheck(orderId)
        }
        return
      }

      // 模拟订单详情数据
      setOrder(buildMockOrder(orderId))
    } catch (error) {
      console.error('获取订单详情失败:', error)
      showToastMessage('获取订单详情失败', 'error')
    } finally {
      setLoading(false)
    }
  }, [mapRealOrder, loadReviewCheck, showToastMessage])

  // 初始化:读取路由参数 + 并行加载配送时段配置(F-SYS-002,失败兜底默认,不阻塞详情加载)
  useReady(() => {
    const instance = Taro.getCurrentInstance()
    const { orderId } = instance.router?.params || {}

    loadDeliverySlots()

    if (orderId) {
      orderIdRef.current = orderId
      loadOrderDetail()
    } else {
      showToastMessage('订单ID缺失', 'error')
      setTimeout(() => {
        Taro.navigateBack()
      }, 1500)
    }
  })

  // 页面显示时刷新订单详情
  useDidShow(() => {
    userActionTracker.trackPageView()
    if (orderIdRef.current && !loadingRef.current) {
      loadOrderDetail()
    }
  })

  // 下拉刷新
  usePullDownRefresh(() => {
    loadOrderDetail().finally(() => {
      Taro.stopPullDownRefresh()
    })
  })

  /**
   * 立即支付(F-ORD-005):测试环境假支付自动完成,真实环境拉起微信支付
   */
  const handlePayment = async () => {
    if (!order) return

    showToastMessage('正在发起支付...', 'loading')

    const payResult = await OrderService.getInstance().createOrderPayment(order.id)
    if (!payResult.success || !payResult.data) {
      showToastMessage(payResult.error?.message || '支付发起失败', 'error')
      return
    }
    const { autoCompleted, requiresSimulation, paymentParams } = payResult.data

    // 测试环境假支付:服务端已自动完成,直接刷新订单状态
    if (autoCompleted) {
      showToastMessage('支付成功（模拟支付自动完成）', 'success')
      setTimeout(() => loadOrderDetail(), 800)
      return
    }
    if (requiresSimulation || !paymentParams) {
      showToastMessage('支付功能待接入', 'error')
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
      showToastMessage('支付成功', 'success')
      setTimeout(() => loadOrderDetail(), 800)
    } catch (e) {
      console.error('微信支付未完成:', e)
      showToastMessage('支付未完成，请重试', 'error')
    }
  }

  const handleCancelOrder = () => {
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
            showToastMessage('订单已取消', 'success')

            // 刷新订单详情
            loadOrderDetail()
          } catch (error) {
            console.error('取消订单失败:', error)
            showToastMessage(error instanceof Error ? error.message : '取消订单失败', 'error')
          }
        }
      }
    })
  }

  /**
   * 催单(F-ORD-008):仅配送中订单展示按钮;服务端防抖,短时间重复点击仅一次生效
   */
  const handleUrgeOrder = async () => {
    if (!order) return
    try {
      const result = await OrderService.getInstance().urgeOrder(order.id)
      if (!result.success) {
        throw new Error(result.error?.message || '催单失败')
      }
      showToastMessage('催单已发出', 'success')
    } catch (error) {
      console.error('催单失败:', error)
      showToastMessage(error instanceof Error ? error.message : '催单失败,请稍后再试', 'error')
    }
  }

  /**
   * 进入评价页(F-ORD-007)
   */
  const handleReview = () => {
    Taro.navigateTo({
      url: `/pages/order-review/index?orderId=${orderIdRef.current}`
    })
  }

  const handleContactDelivery = () => {
    if (!order || !order.deliveryInfo?.deliveryWorkerPhone) return

    Taro.makePhoneCall({
      phoneNumber: order.deliveryInfo.deliveryWorkerPhone
    })
  }

  const handleReorder = () => {
    if (!order) return

    // 检查order.items是否为数组
    if (!Array.isArray(order.items)) {
      console.warn('handleReorder: order.items is not an array', order.items)
      showToastMessage('订单商品信息异常', 'error')
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

  const handleCopyOrderNo = () => {
    if (!order) return

    Taro.setClipboardData({
      data: order.orderNo,
      success: () => {
        showToastMessage('订单号已复制', 'success')
      }
    })
  }

  const handleTrackDelivery = () => {
    if (!order) return

    // 跳转到配送跟踪页面
    Taro.navigateTo({
      url: `/pages/delivery-tracking/index?orderId=${order.id}`
    })
  }

  const handleContinueShopping = () => {
    Taro.switchTab({ url: '/pages/home/index' })
  }

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

  // 费用明细行(F-ORD-003,AC-ORD-003-09:营销优惠明细来自订单快照 —— R-MKT-02/07/08/09/10)
  const costRows = buildOrderDetailCostRows({
    ...order,
    deliveryFeeCents: order.deliveryFee,
    payAmountCents: order.finalAmount
  })

  return (
    <View className='order-detail-page'>
      <OrderStatusHeader
        status={order.status}
        orderNo={order.orderNo}
        onCopyOrderNo={handleCopyOrderNo}
      />

      {/* 配送员信息(配送中订单) */}
      {order.status === 'delivering' && order.deliveryInfo && (
        <DeliveryWorkerCard
          deliveryInfo={order.deliveryInfo}
          onContact={handleContactDelivery}
          onTrack={handleTrackDelivery}
        />
      )}

      {/* 订单时间轴 */}
      <OrderTimelineCard timeline={order.timeline} />

      {/* 商品信息 + 费用明细 */}
      <OrderItemsCard items={order.items} costRows={costRows} />

      {/* 订单评价(F-ORD-007):已评价展示内容 */}
      {order.status === 'completed' && reviewInfo?.hasReviewed && reviewInfo.review && (
        <OrderReviewCard review={reviewInfo.review} />
      )}

      {/* 订单信息 */}
      <OrderInfoCard order={order} deliverySlots={deliverySlots} />

      {/* 收货地址 */}
      <OrderAddressCard address={order.address} />

      {/* 底部操作按钮 */}
      <OrderActions
        order={order}
        reviewInfo={reviewInfo}
        onPayment={handlePayment}
        onCancel={handleCancelOrder}
        onReview={handleReview}
        onReorder={handleReorder}
        onTrack={handleTrackDelivery}
        onUrge={handleUrgeOrder}
        onContinueShopping={handleContinueShopping}
      />

      {/* Toast 提示 */}
      <AtToast
        isOpened={showToast}
        text={toastText}
        status={toastType}
        onClose={hideToast}
      />

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default OrderDetailPage
