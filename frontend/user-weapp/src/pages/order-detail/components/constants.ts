import { OrderReviewCheckResponse } from '@aquarush/common'

// 商品行(R-MKT-02 优惠价快照;成交单价 ≠ 目录价 = 命中营销价,划线目录价展示依据)
export interface OrderItem {
  id: string
  name: string
  image: string
  // 成交单价快照(分)
  priceCents: number
  // 目录价(分,划线价展示依据)
  originalPriceCents?: number
  // 是否命中营销价(成交单价 ≠ 目录价快照 —— R-MKT-01/02)
  hasMarketingPrice?: boolean
  quantity: number
  specifications?: string[]
}

// 赠品行(R-MKT-09:附加 0 价行,随单配送)
export interface OrderGiftItem {
  productId: string
  productName: string
  quantity: number
}

// 评价信息(F-ORD-007):服务端返回结构,含已评价内容与是否可评
export type ReviewCheckState = OrderReviewCheckResponse

export interface DeliveryInfo {
  deliveryWorkerId?: string
  deliveryWorkerName?: string
  deliveryWorkerPhone?: string
  deliveryWorkerAvatar?: string
  estimatedDeliveryTime?: string
  currentLocation?: string
  deliveryDistance?: string
}

export interface OrderTimeline {
  time: string
  title: string
  content?: string
  icon?: string
  color?: string
}

export type OrderStatusKey = 'pending_payment' | 'pending_delivery' | 'delivering' | 'completed' | 'cancelled'

export interface OrderDetailAddress {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
}

export interface OrderDetail {
  id: string
  orderNo: string
  status: OrderStatusKey
  items: OrderItem[]
  totalAmount: number
  deliveryFee: number
  finalAmount: number
  // 营销计价快照(R-MKT-01/02/07/08/09/10:费用明细行展示依据,均以分为单位)
  subtotalCents?: number
  promotionDiscountCents?: number
  promotionActivityName?: string
  firstOrderDiscountCents?: number
  firstOrderActivityName?: string
  couponDiscountCents?: number
  couponName?: string
  ticketRedeemCount?: number
  ticketDiscountCents?: number
  giftItems?: OrderGiftItem[]
  // 水桶费快照(分,历史字段——2026-08 水桶费改造后新订单恒为 0,仅旧订单有值,见 14-misc.md §G06)
  depositCents?: number
  address: OrderDetailAddress
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
}

export const STATUS_MAP: Record<OrderStatusKey, { text: string; color: string; bg: string; step: number }> = {
  pending_payment: { text: '待付款', color: 'var(--status-pending-pay-text)', bg: 'var(--status-pending-pay-bg)', step: 1 },
  pending_delivery: { text: '待配送', color: 'var(--status-dispatch-text)', bg: 'var(--status-dispatch-bg)', step: 2 },
  delivering: { text: '配送中', color: 'var(--status-delivering-text)', bg: 'var(--status-delivering-bg)', step: 3 },
  completed: { text: '已完成', color: 'var(--status-completed-text)', bg: 'var(--status-completed-bg)', step: 4 },
  cancelled: { text: '已取消', color: 'var(--status-cancelled-text)', bg: 'var(--status-cancelled-bg)', step: 0 }
}

export const PAYMENT_METHOD_MAP: Record<string, string> = {
  wechat: '微信支付',
  balance: '余额支付'
}

// 配送时段文案兜底(AC-ORD-001-05,与 DeliveryTimeSelector/后端 DeliveryTimeSlot 枚举对应;
// 在线时优先按后台 F-SYS-002 配置渲染,仅配置获取失败/离线时使用本兜底)
export const DELIVERY_TIME_SLOT_MAP: Record<string, string> = {
  IMMEDIATE: '立即配送（30分钟内）',
  MORNING: '上午配送（9:00-12:00）',
  AFTERNOON: '下午配送（14:00-18:00）',
  EVENING: '晚上配送（18:00-21:00）'
}
