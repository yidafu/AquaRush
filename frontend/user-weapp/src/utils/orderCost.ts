/**
 * 订单费用明细行组装(确认订单页 F-ORD-023 计价展示,R-MON-01)
 * 纯函数:输入服务端 orderPreview 计价结果,输出费用明细各行展示数据。
 * 水票抵扣金额(分)= 商品金额 - 水票抵扣后现金小计(PricingState 口径:subtotalCents - cashSubtotalCents)。
 */
import { OrderPreviewVo, displayCents } from '@aquarush/common'

export interface CostRow {
  /** 行唯一标识(渲染 key 与单测断言用) */
  key: string
  /** 行标签(含活动名/桶数等说明) */
  label: string
  /** 展示值(已格式化,含 ¥) */
  value: string
  /** 扣减项(优惠/抵扣,语义红展示) */
  discount?: boolean
  /** 实付合计行 */
  total?: boolean
}

/** 水票抵扣金额(分):覆盖桶按 ¥0 计入,差额即抵扣额 */
export const ticketDiscountCentsOf = (preview: OrderPreviewVo): number => {
  return Math.max(0, (preview.subtotalCents ?? 0) - (preview.cashSubtotalCents ?? 0))
}

/**
 * 水票优先抵扣可核销张数(F-MKT-007 确认订单页默认优先抵扣):
 * 核销上限 = min(下单总桶数, 可用张数),用户可改选/取消(R-MKT-08 一票抵一桶)。
 */
export const maxTicketRedeem = (
  totalQuantity: number,
  availableTicketCount: number
): number => Math.max(0, Math.min(totalQuantity, availableTicketCount))

/**
 * 组装费用明细行(顺序:商品金额 → 水票抵扣 → 满减/首单立减 → 优惠券 → 赠品 → 配送费 → 押金 → 实付)
 */
export const buildCostRows = (preview: OrderPreviewVo): CostRow[] => {
  const rows: CostRow[] = []

  rows.push({
    key: 'subtotal',
    label: '商品金额',
    value: displayCents(preview.subtotalCents)
  })

  const ticketDiscountCents = ticketDiscountCentsOf(preview)
  if ((preview.ticketCoveredQuantity ?? 0) > 0 && ticketDiscountCents > 0) {
    rows.push({
      key: 'ticket',
      label: `水票抵扣（${preview.ticketCoveredQuantity}桶）`,
      value: `-${displayCents(ticketDiscountCents)}`,
      discount: true
    })
  }

  if ((preview.promotionDiscountCents ?? 0) > 0) {
    rows.push({
      key: 'promotion',
      label: preview.promotionActivityName ? `满减（${preview.promotionActivityName}）` : '满减',
      value: `-${displayCents(preview.promotionDiscountCents)}`,
      discount: true
    })
  }

  if ((preview.firstOrderDiscountCents ?? 0) > 0) {
    rows.push({
      key: 'firstOrder',
      label: preview.firstOrderActivityName ? `首单立减（${preview.firstOrderActivityName}）` : '首单立减',
      value: `-${displayCents(preview.firstOrderDiscountCents)}`,
      discount: true
    })
  }

  if ((preview.couponDiscountCents ?? 0) > 0) {
    rows.push({
      key: 'coupon',
      label: preview.selectedCouponName ? `优惠券（${preview.selectedCouponName}）` : '优惠券',
      value: `-${displayCents(preview.couponDiscountCents)}`,
      discount: true
    })
  }

  const giftItems = preview.giftItems ?? []
  if (giftItems.length > 0) {
    const giftText = giftItems
      .map(gift => `${gift.productName} ×${gift.quantity}`)
      .join('、')
    rows.push({
      key: 'gift',
      label: '赠品',
      value: `${giftText}（${displayCents(0)}）`
    })
  }

  rows.push({
    key: 'delivery',
    label: '配送费',
    value: (preview.deliveryFeeCents ?? 0) === 0 ? '免配送费' : displayCents(preview.deliveryFeeCents)
  })

  // 水桶费(历史字段,2026-08 水桶费改造后新订单恒为 0 不展示,仅旧订单有值,见 14-misc.md §G06)
  if ((preview.depositCents ?? 0) > 0) {
    rows.push({
      key: 'deposit',
      label: '水桶费',
      value: displayCents(preview.depositCents ?? 0)
    })
  }

  rows.push({
    key: 'total',
    label: '实付金额',
    value: displayCents(preview.payAmountCents),
    total: true
  })

  return rows
}

/**
 * 订单详情页费用明细行组装(F-ORD-003 费用明细卡,AC-ORD-003-09)
 * 输入为订单详情营销快照(服务端 order(id) 返回;商品金额=Σ(明细成交单价×数量));
 * 输出与确认订单页 buildCostRows 同构:商品金额 → 水票抵扣 → 满减 → 首单立减 → 优惠券 → 赠品 → 配送费 → 实付。
 * 金额均为分(R-MON-01),仅展示格式化。
 */
export interface OrderDetailCostInput {
  /** 商品金额(分)= Σ(明细成交单价×数量),由 items 推导 */
  subtotalCents?: number | null
  /** 水票核销张数快照(R-MKT-08) */
  ticketRedeemCount?: number | null
  /** 水票抵扣金额(分) */
  ticketDiscountCents?: number | null
  /** 满减/折扣优惠(分,叠加①) */
  promotionDiscountCents?: number | null
  promotionActivityName?: string | null
  /** 首单立减优惠(分,叠加①) */
  firstOrderDiscountCents?: number | null
  firstOrderActivityName?: string | null
  /** 优惠券优惠(分,叠加③) */
  couponDiscountCents?: number | null
  couponName?: string | null
  /** 赠品行(R-MKT-09) */
  giftItems?: Array<{ productName: string; quantity: number }> | null
  /** 配送费(分) */
  deliveryFeeCents?: number | null
  /** 水桶费快照(分,历史字段——2026-08 水桶费改造后新订单恒为 0,仅旧订单有值) */
  depositCents?: number | null
  /** 实付合计(分) */
  payAmountCents?: number | null
}

export const buildOrderDetailCostRows = (order: OrderDetailCostInput): CostRow[] => {
  const rows: CostRow[] = []

  rows.push({
    key: 'subtotal',
    label: '商品金额',
    value: displayCents(order.subtotalCents ?? 0)
  })

  const ticketDiscountCents = order.ticketDiscountCents ?? 0
  if ((order.ticketRedeemCount ?? 0) > 0 && ticketDiscountCents > 0) {
    rows.push({
      key: 'ticket',
      label: `水票抵扣（${order.ticketRedeemCount}桶）`,
      value: `-${displayCents(ticketDiscountCents)}`,
      discount: true
    })
  }

  if ((order.promotionDiscountCents ?? 0) > 0) {
    rows.push({
      key: 'promotion',
      label: order.promotionActivityName ? `满减（${order.promotionActivityName}）` : '满减',
      value: `-${displayCents(order.promotionDiscountCents ?? 0)}`,
      discount: true
    })
  }

  if ((order.firstOrderDiscountCents ?? 0) > 0) {
    rows.push({
      key: 'firstOrder',
      label: order.firstOrderActivityName ? `首单立减（${order.firstOrderActivityName}）` : '首单立减',
      value: `-${displayCents(order.firstOrderDiscountCents ?? 0)}`,
      discount: true
    })
  }

  if ((order.couponDiscountCents ?? 0) > 0) {
    rows.push({
      key: 'coupon',
      label: order.couponName ? `优惠券（${order.couponName}）` : '优惠券',
      value: `-${displayCents(order.couponDiscountCents ?? 0)}`,
      discount: true
    })
  }

  const giftItems = order.giftItems ?? []
  if (giftItems.length > 0) {
    const giftText = giftItems
      .map(gift => `${gift.productName} ×${gift.quantity}`)
      .join('、')
    rows.push({
      key: 'gift',
      label: '赠品',
      value: `${giftText}（${displayCents(0)}）`
    })
  }

  rows.push({
    key: 'delivery',
    label: '配送费',
    value: (order.deliveryFeeCents ?? 0) === 0 ? '免配送费' : displayCents(order.deliveryFeeCents ?? 0)
  })

  // 水桶费(历史字段,2026-08 水桶费改造后新订单恒为 0 不展示,仅旧订单有值,见 14-misc.md §G06)
  if ((order.depositCents ?? 0) > 0) {
    rows.push({
      key: 'deposit',
      label: '水桶费',
      value: displayCents(order.depositCents ?? 0)
    })
  }

  rows.push({
    key: 'total',
    label: '实付金额',
    value: displayCents(order.payAmountCents ?? 0),
    total: true
  })

  return rows
}
