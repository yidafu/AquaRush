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
 * 组装费用明细行(顺序:商品金额 → 水票抵扣 → 满减/首单立减 → 优惠券 → 赠品 → 配送费 → 实付)
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

  rows.push({
    key: 'total',
    label: '实付金额',
    value: displayCents(preview.payAmountCents),
    total: true
  })

  return rows
}
