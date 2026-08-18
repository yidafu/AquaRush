/**
 * 订单费用明细行组装单测(确认订单页 F-ORD-023 计价展示 / 订单详情页 F-ORD-003 费用明细卡)
 * 覆盖:全优惠栈顺序 / 仅水票 / 仅满减 / 仅首单 / 仅优惠券 / 赠品 / 免运费 / 全零 / 水票覆盖 0;
 * 详情页行组装与 buildCostRows 同构;maxTicketRedeem 水票优先抵扣上限。
 */
import { describe, expect, it } from '@jest/globals'
import { OrderPreviewVo, PriceSource } from '@aquarush/common'
import { buildCostRows, buildOrderDetailCostRows, maxTicketRedeem, ticketDiscountCentsOf } from './orderCost'

const makePreview = (overrides: Partial<OrderPreviewVo> = {}): OrderPreviewVo => ({
  availableCoupons: [],
  availableTicketCount: 0,
  cashSubtotalCents: 0,
  couponDiscountCents: 0,
  couponOptions: [],
  deliveryFeeCents: 0,
  depositCents: 0,
  firstOrderDiscountCents: 0,
  giftItems: [],
  items: [],
  payAmountCents: 0,
  payableProductCents: 0,
  priceSource: PriceSource.CATALOG_PRICE,
  promotionDiscountCents: 0,
  subtotalCents: 0,
  ticketCoveredQuantity: 0,
  ticketRedeemCount: 0,
  totalDiscountCents: 0,
  unitPriceCents: 0,
  ...overrides
})

describe('buildCostRows', () => {
  it('全优惠栈:顺序 = 商品金额/水票/满减/优惠券/赠品/配送费/实付,扣减项标记 discount', () => {
    const rows = buildCostRows(makePreview({
      subtotalCents: 5000,
      cashSubtotalCents: 3000, // 水票抵扣 2000
      ticketCoveredQuantity: 2,
      promotionDiscountCents: 500,
      promotionActivityName: '满100减10',
      couponDiscountCents: 300,
      selectedCouponName: '满50减3',
      giftItems: [
        { productId: 'g1', productName: '矿泉水', quantity: 1 },
        { productId: 'g2', productName: '杯垫', quantity: 2 }
      ],
      deliveryFeeCents: 800,
      payAmountCents: 3000
    }))

    expect(rows.map(r => r.key)).toEqual([
      'subtotal', 'ticket', 'promotion', 'coupon', 'gift', 'delivery', 'total'
    ])
    expect(rows[0]).toMatchObject({ label: '商品金额', value: '¥50.00' })
    expect(rows[1]).toMatchObject({ label: '水票抵扣（2桶）', value: '-¥20.00', discount: true })
    expect(rows[2]).toMatchObject({ label: '满减（满100减10）', value: '-¥5.00', discount: true })
    expect(rows[3]).toMatchObject({ label: '优惠券（满50减3）', value: '-¥3.00', discount: true })
    expect(rows[4]).toMatchObject({ label: '赠品', value: '矿泉水 ×1、杯垫 ×2（¥0.00）' })
    expect(rows[5]).toMatchObject({ label: '配送费', value: '¥8.00' })
    expect(rows[6]).toMatchObject({ label: '实付金额', value: '¥30.00', total: true })
  })

  it('仅首单立减:展示 首单立减(活动名) 行', () => {
    const rows = buildCostRows(makePreview({
      subtotalCents: 6000,
      firstOrderDiscountCents: 600,
      firstOrderActivityName: '新人立减',
      payAmountCents: 5400
    }))
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'firstOrder', 'delivery', 'total'])
    expect(rows[1]).toMatchObject({ label: '首单立减（新人立减）', value: '-¥6.00', discount: true })
  })

  it('无任何优惠:仅 商品金额/配送费/实付,免配送费文案', () => {
    const rows = buildCostRows(makePreview({
      subtotalCents: 1000,
      deliveryFeeCents: 0,
      payAmountCents: 1000
    }))
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'delivery', 'total'])
    expect(rows[1]).toMatchObject({ label: '配送费', value: '免配送费' })
    expect(rows.some(r => r.discount)).toBe(false)
  })

  it('配送费非 0 时展示金额', () => {
    const rows = buildCostRows(makePreview({ deliveryFeeCents: 200, payAmountCents: 1200 }))
    expect(rows.find(r => r.key === 'delivery')).toMatchObject({ value: '¥2.00' })
  })

  it('水票覆盖为 0(ticketCoveredQuantity>0 但现金小计=商品金额)不产生水票行', () => {
    const rows = buildCostRows(makePreview({
      subtotalCents: 4000,
      cashSubtotalCents: 4000,
      ticketCoveredQuantity: 2,
      payAmountCents: 4000
    }))
    expect(rows.map(r => r.key)).not.toContain('ticket')
  })

  it('全零:仅基础三行且金额为 ¥0.00', () => {
    const rows = buildCostRows(makePreview())
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'delivery', 'total'])
    expect(rows[0].value).toBe('¥0.00')
    expect(rows[2].value).toBe('¥0.00')
  })

  it('水桶费(历史字段,2026-08 改造):旧订单押金快照 > 0 时在配送费与实付之间展示水桶费行', () => {
    const rows = buildCostRows(makePreview({
      subtotalCents: 5000,
      deliveryFeeCents: 800,
      depositCents: 500,
      payAmountCents: 6300
    }))
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'delivery', 'deposit', 'total'])
    expect(rows[2]).toMatchObject({ label: '水桶费', value: '¥5.00' })
    expect(rows[3]).toMatchObject({ label: '实付金额', value: '¥63.00', total: true })
  })

  it('水桶费为 0 时不展示水桶费行(新订单恒为 0)', () => {
    const rows = buildCostRows(makePreview({ subtotalCents: 1000, deliveryFeeCents: 0, depositCents: 0, payAmountCents: 1000 }))
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'delivery', 'total'])
  })
})

describe('ticketDiscountCentsOf', () => {
  it('= 商品金额 - 水票后现金小计', () => {
    expect(ticketDiscountCentsOf(makePreview({ subtotalCents: 5000, cashSubtotalCents: 3000 }))).toBe(2000)
  })

  it('现金小计异常大于商品金额时钳制为 0', () => {
    expect(ticketDiscountCentsOf(makePreview({ subtotalCents: 1000, cashSubtotalCents: 2000 }))).toBe(0)
  })
})

describe('buildOrderDetailCostRows', () => {
  it('全优惠栈:顺序 = 商品金额/水票/满减/首单/优惠券/赠品/配送费/实付,扣减项标记 discount', () => {
    const rows = buildOrderDetailCostRows({
      subtotalCents: 6500,
      ticketRedeemCount: 1,
      ticketDiscountCents: 2500,
      promotionDiscountCents: 500,
      promotionActivityName: '满100减10',
      firstOrderDiscountCents: 300,
      firstOrderActivityName: '新人立减',
      couponDiscountCents: 200,
      couponName: '满50减3',
      giftItems: [
        { productName: '矿泉水', quantity: 1 },
        { productName: '杯垫', quantity: 2 }
      ],
      deliveryFeeCents: 800,
      payAmountCents: 3000
    })

    expect(rows.map(r => r.key)).toEqual([
      'subtotal', 'ticket', 'promotion', 'firstOrder', 'coupon', 'gift', 'delivery', 'total'
    ])
    expect(rows[0]).toMatchObject({ label: '商品金额', value: '¥65.00' })
    expect(rows[1]).toMatchObject({ label: '水票抵扣（1桶）', value: '-¥25.00', discount: true })
    expect(rows[2]).toMatchObject({ label: '满减（满100减10）', value: '-¥5.00', discount: true })
    expect(rows[3]).toMatchObject({ label: '首单立减（新人立减）', value: '-¥3.00', discount: true })
    expect(rows[4]).toMatchObject({ label: '优惠券（满50减3）', value: '-¥2.00', discount: true })
    expect(rows[5]).toMatchObject({ label: '赠品', value: '矿泉水 ×1、杯垫 ×2（¥0.00）' })
    expect(rows[6]).toMatchObject({ label: '配送费', value: '¥8.00' })
    expect(rows[7]).toMatchObject({ label: '实付金额', value: '¥30.00', total: true })
  })

  it('仅水票:展示 水票抵扣(N桶) 行,其余优惠行省略', () => {
    const rows = buildOrderDetailCostRows({
      subtotalCents: 6000,
      ticketRedeemCount: 2,
      ticketDiscountCents: 4000,
      deliveryFeeCents: 0,
      payAmountCents: 2000
    })
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'ticket', 'delivery', 'total'])
    expect(rows[1]).toMatchObject({ label: '水票抵扣（2桶）', value: '-¥40.00', discount: true })
  })

  it('水票核销 0 桶或抵扣金额 0 时不产生水票行', () => {
    const rows = buildOrderDetailCostRows({
      subtotalCents: 4000,
      ticketRedeemCount: 0,
      ticketDiscountCents: 0,
      payAmountCents: 4000
    })
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'delivery', 'total'])
  })

  it('无任何优惠:仅 商品金额/配送费/实付,免配送费文案', () => {
    const rows = buildOrderDetailCostRows({
      subtotalCents: 1000,
      deliveryFeeCents: 0,
      payAmountCents: 1000
    })
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'delivery', 'total'])
    expect(rows[1]).toMatchObject({ label: '配送费', value: '免配送费' })
    expect(rows.some(r => r.discount)).toBe(false)
  })

  it('缺失字段兜底为 0/空,不抛错', () => {
    const rows = buildOrderDetailCostRows({})
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'delivery', 'total'])
    expect(rows[0].value).toBe('¥0.00')
  })

  it('水桶费(历史字段):详情费用明细在配送费与实付之间展示水桶费行', () => {
    const rows = buildOrderDetailCostRows({
      subtotalCents: 5000,
      deliveryFeeCents: 0,
      depositCents: 500,
      payAmountCents: 5500
    })
    expect(rows.map(r => r.key)).toEqual(['subtotal', 'delivery', 'deposit', 'total'])
    expect(rows[2]).toMatchObject({ label: '水桶费', value: '¥5.00' })
    expect(rows[3]).toMatchObject({ label: '实付金额', value: '¥55.00', total: true })
  })
})

describe('maxTicketRedeem', () => {
  it('可用票充足:上限 = 下单总桶数(优先抵扣全部桶)', () => {
    expect(maxTicketRedeem(5, 10)).toBe(5)
  })

  it('可用票不足:上限 = 可用张数', () => {
    expect(maxTicketRedeem(5, 3)).toBe(3)
  })

  it('无可用票/数量为 0:结果为 0', () => {
    expect(maxTicketRedeem(5, 0)).toBe(0)
    expect(maxTicketRedeem(0, 3)).toBe(0)
  })
})
