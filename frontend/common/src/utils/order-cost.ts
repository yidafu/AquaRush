/**
 * 订单费用明细行组装 —— 三端统一实现(用户端确认/详情页 + 送水工端详情页)
 *
 * 收敛来源:
 * - user-weapp src/utils/orderCost.ts(buildCostRows(preview) / buildOrderDetailCostRows / ticketDiscountCentsOf / maxTicketRedeem)
 * - delivery-app src/utils/orderCost.ts(buildCostRows(OrderCostInput),items 推导小计)
 *
 * 行序/文案/键与两端现状逐字一致:商品金额 → 水票抵扣 → 满减 → 首单立减 → 优惠券 → 赠品 → 配送费 → 水桶押金 → 实付。
 * 金额均为分(R-MON-01),仅展示格式化;所有分字段统一 normalizeCents(number|string 兼容 Long 传输)。
 */
import type { OrderVo, OrderPreviewVo } from '../types/graphql'
import { displayCents, normalizeCents } from './money'

/** 费用明细行(行唯一标识 / 标签 / 展示值;扣减项 discount;实付合计 total) */
export interface CostRow {
  key: string
  label: string
  value: string
  discount?: boolean
  total?: boolean
}

/** 统一输入快照:每个分字段容忍 number|string|null;subtotal 优先直传,缺省按 items 推导 */
export interface OrderCostSnapshot {
  /** 商品金额(分):直传 ?? Σ(items) ?? 0 */
  subtotalCents?: number | string | null
  /** 商品明细(小计推导源,delivery 端):商品金额 = Σ(成交单价×数量) */
  items?: ReadonlyArray<{ unitPriceCents?: number | string | null; quantity?: number }> | null
  /** 水票核销张数(一票抵一桶,R-MKT-08) */
  ticketRedeemCount?: number | null
  /** 水票抵扣金额(分) */
  ticketDiscountCents?: number | string | null
  /** 满减/折扣优惠(分,叠加①,R-MKT-07) */
  promotionDiscountCents?: number | string | null
  promotionActivityName?: string | null
  /** 首单立减优惠(分,与满减互斥取最大,R-MKT-10) */
  firstOrderDiscountCents?: number | string | null
  firstOrderActivityName?: string | null
  /** 优惠券优惠(分,叠加③,R-MKT-07) */
  couponDiscountCents?: number | string | null
  couponName?: string | null
  /** 赠品行(附加 0 价行,R-MKT-09) */
  giftItems?: ReadonlyArray<{ productName?: string | null; quantity?: number }> | null
  /** 配送费(分) */
  deliveryFeeCents?: number | string | null
  /** 水桶押金快照(分,循环水桶押金随单收取,R-MON-03) */
  depositCents?: number | string | null
  /** 实付合计(分) */
  payAmountCents?: number | string | null
}

const zero = (v: number | string | null | undefined): number => normalizeCents(v) ?? 0

/**
 * 组装费用明细行(顺序:商品金额 → 水票抵扣 → 满减/首单立减 → 优惠券 → 赠品 → 配送费 → 水桶押金 → 实付)
 */
export const buildCostRows = (snapshot: OrderCostSnapshot): CostRow[] => {
  const rows: CostRow[] = []

  const subtotalCents =
    normalizeCents(snapshot.subtotalCents) ??
    (snapshot.items ?? []).reduce(
      (sum, item) => sum + zero(item.unitPriceCents) * (item.quantity ?? 0),
      0
    )
  rows.push({ key: 'subtotal', label: '商品金额', value: displayCents(subtotalCents) })

  const ticketRedeemCount = snapshot.ticketRedeemCount ?? 0
  const ticketDiscountCents = zero(snapshot.ticketDiscountCents)
  if (ticketRedeemCount > 0 && ticketDiscountCents > 0) {
    rows.push({
      key: 'ticket',
      label: `水票抵扣（${ticketRedeemCount}桶）`,
      value: `-${displayCents(ticketDiscountCents)}`,
      discount: true,
    })
  }

  const promotionDiscountCents = zero(snapshot.promotionDiscountCents)
  if (promotionDiscountCents > 0) {
    rows.push({
      key: 'promotion',
      label: snapshot.promotionActivityName ? `满减（${snapshot.promotionActivityName}）` : '满减',
      value: `-${displayCents(promotionDiscountCents)}`,
      discount: true,
    })
  }

  const firstOrderDiscountCents = zero(snapshot.firstOrderDiscountCents)
  if (firstOrderDiscountCents > 0) {
    rows.push({
      key: 'firstOrder',
      label: snapshot.firstOrderActivityName ? `首单立减（${snapshot.firstOrderActivityName}）` : '首单立减',
      value: `-${displayCents(firstOrderDiscountCents)}`,
      discount: true,
    })
  }

  const couponDiscountCents = zero(snapshot.couponDiscountCents)
  if (couponDiscountCents > 0) {
    rows.push({
      key: 'coupon',
      label: snapshot.couponName ? `优惠券（${snapshot.couponName}）` : '优惠券',
      value: `-${displayCents(couponDiscountCents)}`,
      discount: true,
    })
  }

  const giftItems = snapshot.giftItems ?? []
  if (giftItems.length > 0) {
    const giftText = giftItems
      .map((gift) => `${gift.productName ?? '赠品'} ×${gift.quantity ?? 0}`)
      .join('、')
    rows.push({ key: 'gift', label: '赠品', value: `${giftText}（${displayCents(0)}）` })
  }

  const deliveryFeeCents = zero(snapshot.deliveryFeeCents)
  rows.push({
    key: 'delivery',
    label: '配送费',
    value: deliveryFeeCents === 0 ? '免配送费' : displayCents(deliveryFeeCents),
  })

  // 水桶押金(循环水桶押金随单收取,R-MON-03;为 0 不展示)
  const depositCents = zero(snapshot.depositCents)
  if (depositCents > 0) {
    rows.push({ key: 'deposit', label: '水桶押金', value: displayCents(depositCents) })
  }

  rows.push({
    key: 'total',
    label: '实付金额',
    value: displayCents(zero(snapshot.payAmountCents)),
    total: true,
  })

  return rows
}

/**
 * 起送门槛提示文案(R-OPS-03/G96):服务端 belowMinimum=true(商品成交小计 < 启用中的门槛)时返回
 * 「商品金额未达起送金额 ¥30.00，还差 ¥5.00」,否则 null;与服务端 createOrder 拦截同口径(小于拦、等于放行)。
 */
export const minimumOrderHint = (
  preview: Pick<OrderPreviewVo, 'minimumOrderCents' | 'belowMinimum' | 'subtotalCents'>
): string | null => {
  if (!preview.belowMinimum) return null
  const minimum = normalizeCents(preview.minimumOrderCents) ?? 0
  if (minimum <= 0) return null
  const gap = Math.max(0, minimum - (normalizeCents(preview.subtotalCents) ?? 0))
  return `商品金额未达起送金额 ${displayCents(minimum)}，还差 ${displayCents(gap)}`
}

/** 水票抵扣金额(分):覆盖桶按 ¥0 计入,差额即抵扣额(确认订单页计价口径:subtotalCents - cashSubtotalCents) */
export const ticketDiscountCentsOf = (
  subtotalCents: number | string | null | undefined,
  cashSubtotalCents: number | string | null | undefined
): number => Math.max(0, zero(subtotalCents) - zero(cashSubtotalCents))

/**
 * 水票优先抵扣可核销张数(F-MKT-007 确认订单页默认优先抵扣):
 * 核销上限 = min(下单总桶数, 可用张数),用户可改选/取消(R-MKT-08 一票抵一桶)。
 */
export const maxTicketRedeem = (
  totalQuantity: number,
  availableTicketCount: number
): number => Math.max(0, Math.min(totalQuantity, availableTicketCount))

/**
 * 订单快照(服务端 OrderVo)→ 统一费用快照(订单详情页费用明细卡;商品金额=order.amountCents,
 * 营销优惠明细来自订单快照 —— R-MKT-02/07/08/09/10)。
 */
export const orderCostSnapshotFromOrder = (order: OrderVo): OrderCostSnapshot => ({
  subtotalCents: order.amountCents,
  ticketRedeemCount: order.ticketRedeemCount ?? 0,
  ticketDiscountCents: order.ticketDiscountCents ?? 0,
  promotionDiscountCents: order.promotionDiscountCents ?? 0,
  promotionActivityName: order.promotionActivityName ?? null,
  firstOrderDiscountCents: order.firstOrderDiscountCents ?? 0,
  firstOrderActivityName: order.firstOrderActivityName ?? null,
  couponDiscountCents: order.couponDiscountCents ?? 0,
  couponName: order.couponName ?? null,
  giftItems: order.giftItems ?? [],
  deliveryFeeCents: order.deliveryFeeCents,
  depositCents: order.depositCents,
  payAmountCents: order.payAmountCents,
})

/**
 * 确认订单页预览 → 统一快照适配(preview 各营销字段即 OrderCostSnapshot 字段;
 * 水票张数/抵扣额由 ticketCoveredQuantity 与 ticketDiscountCentsOf 推导;
 * 优惠券名从 selectedCouponName 映射到 couponName)。
 */
export const orderCostFromPreview = (preview: OrderPreviewVo): OrderCostSnapshot => ({
  ...preview,
  ticketRedeemCount: preview.ticketCoveredQuantity,
  ticketDiscountCents: ticketDiscountCentsOf(preview.subtotalCents, preview.cashSubtotalCents),
  couponName: preview.selectedCouponName ?? null,
})
