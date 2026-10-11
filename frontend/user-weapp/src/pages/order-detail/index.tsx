import React, { useCallback, useEffect, useMemo, useRef, useState } from 'react'
import { View, Text } from '@tarojs/components'
import Taro, { useReady, useDidShow, usePullDownRefresh } from '@tarojs/taro'
import {
  DeliverySlotVo,
  OrderStatus,
  useOrderDetail,
  type OrderDetailView
} from '@aquarush/common'
import OrderService from '../../services/OrderService'
import ConfigService from '../../services/ConfigService'
import { userActionTracker } from '../../utils/tracking'
import { DEFAULT_DELIVERY_SLOTS } from '../../utils/deliverySlots'

// 子组件
import OrderStatusHeader from './components/OrderStatusHeader'
import DeliveryWorkerCard from '../../components/DeliveryWorkerCard'
import OrderTimelineCard from './components/OrderTimelineCard'
import OrderItemsCard from './components/OrderItemsCard'
import OrderReviewCard from './components/OrderReviewCard'
import OrderInfoCard from './components/OrderInfoCard'
import AddressSummaryCard from '../../components/AddressSummaryCard'
import OrderActions from './components/OrderActions'
import type { OrderDetail, ReviewCheckState } from './components/constants'

import './index.scss'

/** 时间轴图标语义键 → 图标值(common 视图为平台无关语义;WeUI 版时间轴不再渲染图标,仅保留映射兼容) */
const TIMELINE_ICON: Record<OrderDetailView['timeline'][number]['icon'], string> = {
  'order-created': 'bookmark',
  paid: 'credit-card',
  'delivery-started': 'truck',
  delivered: 'check-circle',
  completed: 'check-circle'
}

/** 时间轴 tone → 页面 CSS 变量色 */
const TIMELINE_COLOR: Record<string, string> = {
  info: 'var(--weui-BRAND)',
  accent: 'var(--action)',
  success: 'var(--success)'
}

/**
 * 订单详情页(F-ORD-003,设计稿 02-order/order-detail.html WeUI 视觉)
 * 加载编排/订单快照归一化/费用明细行/状态派生由 @aquarush/common 的 useOrderDetail 共享
 * (weapp 与 delivery-app 同源);本页保留 weapp 展示模型适配与事件处理,
 * 渲染全部由 components/ 子组件完成。
 * 已完成订单额外加载评价信息(F-ORD-007);配送时段配置(F-SYS-002)并行加载不阻塞详情。
 */
const OrderDetailPage: React.FC = () => {
  const [orderId, setOrderId] = useState<string>('')
  const [showToast, setShowToast] = useState<boolean>(false)
  const [toastText, setToastText] = useState<string>('')
  const [toastType, setToastType] = useState<'success' | 'error' | 'loading'>('success')
  // 评价信息(F-ORD-007):已完成订单的评价入口/已评内容
  const [reviewInfo, setReviewInfo] = useState<ReviewCheckState | null>(null)
  // 配送时段配置(F-SYS-002:全量含已停用,历史订单时段文案按配置渲染,兜底见 DELIVERY_TIME_SLOT_MAP)
  const [deliverySlots, setDeliverySlots] = useState<DeliverySlotVo[]>(DEFAULT_DELIVERY_SLOTS)

  const loadingRef = useRef<boolean>(true)

  /**
   * GraphQL OrderStatus → 页面状态键(weapp 展示模型小写键;渲染语义与文案见 common order-status)
   */
  const mapStatus = useCallback((status: OrderStatus): OrderDetail['status'] => {
    const map: Record<OrderStatus, OrderDetail['status']> = {
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
   * common OrderDetailView → 页面订单结构(仅做展示模型适配;items/时间轴/地址/营销快照
   * 已由 toOrderDetailView 归一化,金额均为分)
   */
  const mapViewToDetail = useCallback(
    (view: OrderDetailView): OrderDetail => {
      const order = view.order
      return {
        id: order.id,
        orderNo: order.orderNo,
        status: mapStatus(order.status),
        items: view.items.map((item) => ({
          id: item.id,
          name: item.name,
          image: item.image || '/assets/default-product.png',
          priceCents: item.unitPriceCents,
          originalPriceCents: item.catalogPriceCents,
          hasMarketingPrice: item.hasMarketingPrice,
          quantity: item.quantity,
          specifications: item.specification ? [item.specification] : []
        })),
        totalAmount: order.amountCents,
        deliveryFee: order.deliveryFeeCents,
        finalAmount: order.payAmountCents,
        // 营销计价快照(分,费用明细行展示依据 —— R-MKT-02/07/08/09/10)
        subtotalCents: view.items.reduce((sum, item) => sum + item.unitPriceCents * item.quantity, 0),
        promotionDiscountCents: order.promotionDiscountCents ?? 0,
        promotionActivityName: order.promotionActivityName || undefined,
        firstOrderDiscountCents: order.firstOrderDiscountCents ?? 0,
        firstOrderActivityName: order.firstOrderActivityName || undefined,
        couponDiscountCents: order.couponDiscountCents ?? 0,
        couponName: order.couponName || undefined,
        ticketRedeemCount: order.ticketRedeemCount ?? 0,
        ticketDiscountCents: order.ticketDiscountCents ?? 0,
        // 水桶费快照(历史字段,2026-08 水桶费改造后新订单恒为 0,仅旧订单有值,见 14-misc.md §G06)
        depositCents: order.depositCents ?? 0,
        giftItems: (order.giftItems ?? []).map((gift) => ({
          productId: String(gift.productId),
          productName: gift.productName || '赠品',
          quantity: gift.quantity ?? 0
        })),
        address: view.address ?? {
          receiverName: '',
          phone: '',
          province: '',
          city: '',
          district: '',
          detailAddress: ''
        },
        paymentMethod: order.paymentMethod === 'WECHAT_PAY' ? 'wechat' : (order.paymentMethod || 'wechat'),
        deliveryTime: 'immediate',
        // 配送时段(AC-ORD-001-05):随单快照,缺省=立即配送
        deliveryTimeSlot: order.deliveryTimeSlot || 'IMMEDIATE',
        createTime: order.createdAt || '',
        paymentTime: order.paymentTime || undefined,
        remark: order.remark || undefined,
        timeline: view.timeline.map((t) => ({
          time: t.time,
          title: t.title,
          content: t.content,
          icon: TIMELINE_ICON[t.icon],
          color: TIMELINE_COLOR[t.tone]
        })),
        deliveryInfo: order.deliveryWorker
          ? {
              deliveryWorkerId: String(order.deliveryWorker.id ?? ''),
              deliveryWorkerName: order.deliveryWorker.name,
              deliveryWorkerPhone: order.deliveryWorker.phone,
              deliveryWorkerAvatar: order.deliveryWorker.avatarUrl ?? undefined
            }
          : undefined
      }
    },
    [mapStatus]
  )

  // 共享加载编排(useOrderDetail:loading/error/refresh + 费用明细行 + 状态派生)
  const {
    order: rawOrder,
    loading,
    error,
    refresh,
    view,
    costRows
  } = useOrderDetail(orderId || undefined, {
    fetchOrder: async () => {
      const result = await OrderService.getInstance().getOrder(orderId)
      return result.success && result.data ? result.data : null
    }
  })

  const order = useMemo(() => (view ? mapViewToDetail(view) : null), [view, mapViewToDetail])

  // 加载失败提示(共享 hook 的 error)
  useEffect(() => {
    if (error) {
      showToastMessage('获取订单详情失败', 'error')
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [error])

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

  // WeUI toast 视觉容器:保留原 AtToast 的 3s 自动关闭行为
  useEffect(() => {
    if (!showToast) return
    const timer = setTimeout(hideToast, 3000)
    return () => clearTimeout(timer)
  }, [showToast, toastText, toastType, hideToast])

  const loadDeliverySlots = useCallback(async () => {
    try {
      const result = await ConfigService.getDeliverySlots()
      if (result.success && result.data && result.data.length > 0) {
        setDeliverySlots(result.data)
      }
    } catch (error) {
      console.error('获取配送时段配置失败:', error)
    }
  }, [])

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

  // 已完成订单:加载评价信息(F-ORD-007)
  useEffect(() => {
    if (rawOrder?.status === OrderStatus.COMPLETED && orderId) {
      loadReviewCheck(orderId)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [rawOrder?.status, orderId])

  // 初始化:读取路由参数 + 并行加载配送时段配置(F-SYS-002,失败兜底默认,不阻塞详情加载)
  useReady(() => {
    const instance = Taro.getCurrentInstance()
    const { orderId: routeOrderId } = instance.router?.params || {}

    loadDeliverySlots()

    if (routeOrderId) {
      setOrderId(routeOrderId)
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
    if (orderId && !loadingRef.current) {
      void refresh()
    }
  })

  // 下拉刷新
  usePullDownRefresh(() => {
    refresh().finally(() => {
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
      setTimeout(() => refresh(), 800)
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
      setTimeout(() => refresh(), 800)
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
            refresh()
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
      url: `/pages/order-review/index?orderId=${orderId}`
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

  // 首帧(useReady 尚未写入路由参数)与加载中统一显示加载态
  if (loading || !orderId) {
    return (
      <View className='order-detail-page'>
        <View className='loading-container'>
          <Text className='loading-text'>加载中...</Text>
        </View>
      </View>
    )
  }

  if (!order) {
    return (
      <View className='order-detail-page'>
        <View className='error-container'>
          <Text className='error-text'>订单不存在</Text>
          <View className='weui-btn weui-btn_plain-primary weui-btn_mini' onClick={() => Taro.navigateBack()}>
            返回
          </View>
        </View>
      </View>
    )
  }

  return (
    <View className='order-detail-page'>
      <OrderStatusHeader
        status={rawOrder?.status}
        orderNo={order.orderNo}
        onCopyOrderNo={handleCopyOrderNo}
      />

      {/* 配送员信息(配送中订单) */}
      {order.status === 'delivering' && order.deliveryInfo && (
        <DeliveryWorkerCard
          name={order.deliveryInfo.deliveryWorkerName}
          phone={order.deliveryInfo.deliveryWorkerPhone}
          avatar={order.deliveryInfo.deliveryWorkerAvatar}
          location={order.deliveryInfo.currentLocation}
          distance={order.deliveryInfo.deliveryDistance}
          onContact={handleContactDelivery}
          onTrack={handleTrackDelivery}
        />
      )}

      {/* 订单时间轴 */}
      <OrderTimelineCard timeline={order.timeline} />

      {/* 收货地址 */}
      <AddressSummaryCard
        receiverName={order.address.receiverName}
        phone={order.address.phone}
        province={order.address.province}
        city={order.address.city}
        district={order.address.district}
        detailAddress={order.address.detailAddress}
      />

      {/* 商品信息 + 费用明细 */}
      <OrderItemsCard items={order.items} costRows={costRows} />

      {/* 订单评价(F-ORD-007):已评价展示内容 */}
      {order.status === 'completed' && reviewInfo?.hasReviewed && reviewInfo.review && (
        <OrderReviewCard review={reviewInfo.review} />
      )}

      {/* 订单信息 */}
      <OrderInfoCard
        order={order}
        deliverySlots={deliverySlots}
        onCopyOrderNo={handleCopyOrderNo}
      />

      {/* 底部操作按钮(.mp-footbar) */}
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

      {/* Toast 提示(WeUI .weui-toast 视觉容器,状态逻辑与原 AtToast 一致) */}
      {showToast && (
        <View className='weui-toast weui-toast_text'>
          <Text className='weui-toast__text'>{toastText}</Text>
        </View>
      )}

      {/* 底部安全区域 */}
      <View className='safe-bottom' />
    </View>
  )
}

export default OrderDetailPage
