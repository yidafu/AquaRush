import { OrderStatus } from '@aquarush/common'

export interface OrderProduct {
  id: string
  name: string
  image: string
  priceCents: string | number
  specification?: string
}

export interface OrderItem {
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
  /** 是否命中营销价(成交单价 ≠ 目录价快照,划线价展示 —— AC-ORD-002-09) */
  hasMarketingPrice: boolean
  amountCents: string | number
  deliveryFeeCents: string | number
  payAmountCents: string | number
  promotionDiscountCents: string | number | null
  promotionActivityName?: string | null
  couponDiscountCents: string | number | null
  couponName?: string | null
  firstOrderDiscountCents: string | number | null
  firstOrderActivityName?: string | null
  ticketRedeemCount?: number | null
  ticketDiscountCents?: string | number | null
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

export const TAB_LIST: { title: string; statuses: OrderStatus[] | null }[] = [
  { title: '全部', statuses: null },
  { title: '待付款', statuses: [OrderStatus.PENDING_PAYMENT] },
  // 「待发货」口径=待派单+待配送(02-business-flows.md §订单状态机)
  { title: '待配送', statuses: [OrderStatus.PENDING_DISPATCH, OrderStatus.PENDING_DELIVERY] },
  { title: '配送中', statuses: [OrderStatus.DELIVERING] },
  { title: '已完成', statuses: [OrderStatus.COMPLETED] }
]

// 状态标签(§2.2 设计系统:共享 .status-tag 类 + 修饰符,样式见 styles/common.scss)
export const STATUS_MAP: Record<string, { text: string; modifier: string }> = {
  [OrderStatus.PENDING_PAYMENT]: { text: '待付款', modifier: 'status-tag--pending-pay' },
  [OrderStatus.PENDING_DISPATCH]: { text: '待配送', modifier: 'status-tag--dispatch' },
  [OrderStatus.PENDING_DELIVERY]: { text: '待配送', modifier: 'status-tag--dispatch' },
  [OrderStatus.DELIVERING]: { text: '配送中', modifier: 'status-tag--delivering' },
  [OrderStatus.COMPLETED]: { text: '已完成', modifier: 'status-tag--completed' },
  [OrderStatus.CANCELLED]: { text: '已取消', modifier: 'status-tag--cancelled' },
  [OrderStatus.REFUNDED]: { text: '已退款', modifier: 'status-tag--cancelled' }
}
