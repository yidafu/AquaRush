/**
 * 订单详情视图映射 —— 服务端 OrderVo 快照 → 各端共用的展示模型(纯函数,无平台依赖)
 *
 * 收敛来源:user-weapp pages/order-detail/index.tsx 的 mapStatus/mapRealOrder/时间轴组装;
 * delivery-app 直接消费原始 OrderVo(本视图为其可选迁移目标)。
 * 平台相关渲染语义(taro-ui 图标名/CSS 变量色)不进入本模块,由各端映射:
 * - timeline.icon 为语义键,weapp 映射为 AtIcon value;
 * - timeline.tone 为 StatusTone,weapp 映射为 CSS 变量色。
 */
import type { OrderVo } from '../types/graphql'
import { OrderStatus } from '../types/graphql'
import { normalizeCents } from '../utils/money'
import type { StatusTone } from '../utils/status-tone'

/** 商品行展示模型(成交单价/目录价/是否命中营销价 —— R-MKT-01/02 划线价判定依据) */
export interface OrderItemView {
  id: string
  name: string
  image: string
  /** 成交单价(分,快照) */
  unitPriceCents: number
  /** 目录价(分,划线价展示依据;成交价≠目录价 = 命中营销价) */
  catalogPriceCents?: number
  hasMarketingPrice: boolean
  quantity: number
  specification?: string
}

/** 时间轴条目语义(icon 为语义键,由端映射到图标库) */
export interface OrderTimelineView {
  time: string
  title: string
  content: string
  icon: 'order-created' | 'paid' | 'delivery-started' | 'delivered' | 'completed'
  tone: StatusTone
}

export interface OrderAddressView {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
}

/** 订单状态派生标志(页面渲染条件用,替代两端内联判断) */
export interface OrderStatusFlags {
  isPendingPayment: boolean
  isPendingDelivery: boolean
  isDelivering: boolean
  isAwaitingConfirmation: boolean
  isCompleted: boolean
  isCancelledOrRefunded: boolean
}

export interface OrderDetailView {
  order: OrderVo
  items: OrderItemView[]
  timeline: OrderTimelineView[]
  address: OrderAddressView | null
  flags: OrderStatusFlags
}

export const statusFlagsOf = (status: OrderStatus): OrderStatusFlags => ({
  isPendingPayment: status === OrderStatus.PENDING_PAYMENT,
  isPendingDelivery: status === OrderStatus.PENDING_DISPATCH || status === OrderStatus.PENDING_DELIVERY,
  isDelivering: status === OrderStatus.DELIVERING,
  isAwaitingConfirmation: status === OrderStatus.AWAITING_CONFIRMATION,
  isCompleted: status === OrderStatus.COMPLETED,
  isCancelledOrRefunded: status === OrderStatus.CANCELLED || status === OrderStatus.REFUNDED,
})

/** 订单状态 → 时间轴条目(仅已发生节点;time 为空则跳过) */
export const buildOrderTimeline = (order: OrderVo): OrderTimelineView[] => {
  const timeline: OrderTimelineView[] = []
  if (order.createdAt) {
    timeline.push({ time: order.createdAt, title: '订单创建', content: '您的订单已创建', icon: 'order-created', tone: 'info' })
  }
  if (order.paymentTime) {
    timeline.push({ time: order.paymentTime, title: '支付成功', content: '使用微信支付成功', icon: 'paid', tone: 'accent' })
  }
  if (order.deliveryStartedAt) {
    timeline.push({ time: order.deliveryStartedAt, title: '开始配送', content: '配送员正在配送中', icon: 'delivery-started', tone: 'success' })
  }
  // 已送达(送达时间,待确认收货 —— R-ORD-11)
  if (order.deliveryConfirmedAt) {
    timeline.push({
      time: order.deliveryConfirmedAt,
      title: '已送达',
      content: order.status === OrderStatus.AWAITING_CONFIRMATION ? '商品已送达,请确认收货' : '商品已送达',
      icon: 'delivered',
      tone: 'success',
    })
  }
  if (order.completedAt) {
    timeline.push({ time: order.completedAt, title: '订单完成', content: '订单已完成', icon: 'completed', tone: 'success' })
  }
  return timeline
}

/** 服务端 OrderVo → 展示模型 */
export const toOrderDetailView = (order: OrderVo): OrderDetailView => {
  const items: OrderItemView[] = (order.items ?? []).map((item) => {
    // 成交单价(分,快照);目录价(分,商品快照)—— 划线价判定:成交单价 ≠ 目录价即命中营销价(AC-ORD-003-09,R-MKT-01/02)
    const unitPriceCents = normalizeCents(item.unitPriceCents) ?? normalizeCents(item.product?.priceCents) ?? 0
    const catalogPriceCents = normalizeCents(item.product?.priceCents) ?? unitPriceCents
    return {
      id: item.productId,
      name: item.product?.name || '商品',
      image: item.product?.coverImageUrl || '',
      unitPriceCents,
      catalogPriceCents: unitPriceCents !== catalogPriceCents ? catalogPriceCents : undefined,
      hasMarketingPrice: unitPriceCents !== catalogPriceCents,
      quantity: item.quantity,
      specification: item.product?.specification || undefined,
    }
  })

  const address: OrderAddressView | null = order.address
    ? {
        receiverName: order.address.receiverName,
        phone: order.address.phone,
        province: order.address.province,
        city: order.address.city,
        district: order.address.district,
        detailAddress: order.address.detailAddress,
      }
    : null

  return {
    order,
    items,
    timeline: buildOrderTimeline(order),
    address,
    flags: statusFlagsOf(order.status),
  }
}
