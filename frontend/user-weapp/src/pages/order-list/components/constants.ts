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

// 注:TAB_LIST 与 STATUS_MAP 已收敛到 @aquarush/common(ORDER_TABS / order-status + status-tone),
// 本文件仅保留页面列表卡片展示模型类型。

/**
 * Tab 定位 key(下标与 ORDER_TABS 一一对应):
 * 订单列表是 tabBar 页,调用方(「我的」宫格/首页宫格)只能 switchTab 无法带参,
 * 跨页经 storage key 'pendingOrderTab' 接力定位(onShow 读后即清);
 * key 字符串与「我的」页订单宫格约定一致。
 */
export const ORDER_TAB_KEYS: string[] = ['all', 'pendingPay', 'pendingDispatch', 'delivering', 'completed']

/** 跨页 tab 接力的 storage key(读后立即清除) */
export const PENDING_ORDER_TAB_STORAGE_KEY = 'pendingOrderTab'
