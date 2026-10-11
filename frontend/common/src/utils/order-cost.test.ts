/**
 * order-cost 单测(合并 user-weapp 与 delivery-app 两端测试):
 * 全优惠栈顺序 / 仅水票 / 仅满减 / 仅首单 / 仅优惠券 / 赠品 / 免运费 / 全零 / 水票覆盖 0 /
 * 详情快照(含水桶押金)/ 小计直传与 items 推导 / ticketDiscountCentsOf / maxTicketRedeem / orderCostFromPreview。
 */
import { describe, expect, it } from '@jest/globals'
import { OrderPreviewVo, PriceSource } from '../types/graphql'
import {
  buildCostRows,
  maxTicketRedeem,
  minimumOrderHint,
  orderCostFromPreview,
  ticketDiscountCentsOf,
  type OrderCostSnapshot,
} from './order-cost'

const makePreview = (overrides: Partial<OrderPreviewVo> = {}): OrderPreviewVo => ({
  availableCoupons: [],
  availableTicketCount: 0,
  belowMinimum: false,
  cashSubtotalCents: 0,
  couponDiscountCents: 0,
  couponOptions: [],
  deliveryFeeCents: 0,
  depositCents: 0,
  firstOrderDiscountCents: 0,
  giftItems: [],
  items: [],
  minimumOrderCents: 0,
  payAmountCents: 0,
  payableProductCents: 0,
  priceSource: PriceSource.CATALOG_PRICE,
  promotionDiscountCents: 0,
  subtotalCents: 0,
  ticketCoveredQuantity: 0,
  ticketRedeemCount: 0,
  totalDiscountCents: 0,
  unitPriceCents: 0,
  ...overrides,
})

describe('buildCostRows', () => {
  it('全优惠栈:顺序 = 商品金额/水票/满减/首单/优惠券/赠品/配送费/实付,扣减项标记 discount', () => {
    const snapshot: OrderCostSnapshot = {
      items: [
        { unitPriceCents: 2500, quantity: 2 },
        { unitPriceCents: 1500, quantity: 1 },
      ],
      deliveryFeeCents: 800,
      payAmountCents: 3000,
      ticketRedeemCount: 1,
      ticketDiscountCents: 2500,
      promotionDiscountCents: 500,
      promotionActivityName: '满100减10',
      firstOrderDiscountCents: 300,
      firstOrderActivityName: '新人立减',
      couponDiscountCents: 200,
      couponName: '满50减3',
      giftItems: [{ productName: '矿泉水', quantity: 1 }],
    }

    const rows = buildCostRows(snapshot)

    expect(rows.map((r) => r.key)).toEqual([
      'subtotal',
      'ticket',
      'promotion',
      'firstOrder',
      'coupon',
      'gift',
      'delivery',
      'total',
    ])
    expect(rows[0]).toMatchObject({ label: '商品金额', value: '¥65.00' })
    expect(rows[1]).toMatchObject({ label: '水票抵扣（1桶）', value: '-¥25.00', discount: true })
    expect(rows[2]).toMatchObject({ label: '满减（满100减10）', value: '-¥5.00', discount: true })
    expect(rows[3]).toMatchObject({ label: '首单立减（新人立减）', value: '-¥3.00', discount: true })
    expect(rows[4]).toMatchObject({ label: '优惠券（满50减3）', value: '-¥2.00', discount: true })
    expect(rows[5]).toMatchObject({ label: '赠品', value: '矿泉水 ×1（¥0.00）' })
    expect(rows[6]).toMatchObject({ label: '配送费', value: '¥8.00' })
    expect(rows[7]).toMatchObject({ label: '实付金额', value: '¥30.00', total: true })
  })

  it('小计直传优先于 items 推导(number|string 均容忍)', () => {
    const rows = buildCostRows({
      subtotalCents: '6550',
      items: [{ unitPriceCents: 2500, quantity: 2 }],
      deliveryFeeCents: 0,
      payAmountCents: 6550,
    })
    expect(rows[0].value).toBe('¥65.50')
  })

  it('小计缺省按 items 推导', () => {
    const rows = buildCostRows({
      items: [{ unitPriceCents: '2500', quantity: 2 }],
      deliveryFeeCents: 0,
      payAmountCents: 5000,
    })
    expect(rows[0].value).toBe('¥50.00')
  })

  it('仅水票:抵扣行与实付', () => {
    const rows = buildCostRows({
      subtotalCents: 5000,
      ticketRedeemCount: 2,
      ticketDiscountCents: 5000,
      deliveryFeeCents: 800,
      payAmountCents: 800,
    })
    expect(rows.map((r) => r.key)).toEqual(['subtotal', 'ticket', 'delivery', 'total'])
    expect(rows[1]).toMatchObject({ label: '水票抵扣（2桶）', value: '-¥50.00' })
  })

  it('仅满减(无活动名)/仅首单(无活动名)/仅优惠券(无名称)', () => {
    const rows = buildCostRows({
      subtotalCents: 10000,
      promotionDiscountCents: 1000,
      firstOrderDiscountCents: 500,
      couponDiscountCents: 300,
      deliveryFeeCents: 0,
      payAmountCents: 8200,
    })
    expect(rows.map((r) => r.key)).toEqual(['subtotal', 'promotion', 'firstOrder', 'coupon', 'delivery', 'total'])
    expect(rows[1]).toMatchObject({ label: '满减', value: '-¥10.00' })
    expect(rows[2]).toMatchObject({ label: '首单立减', value: '-¥5.00' })
    expect(rows[3]).toMatchObject({ label: '优惠券', value: '-¥3.00' })
  })

  it('赠品行:多赠品顿号拼接;免运费文案', () => {
    const rows = buildCostRows({
      subtotalCents: 3000,
      giftItems: [
        { productName: '矿泉水', quantity: 1 },
        { productName: '纸巾', quantity: 2 },
      ],
      deliveryFeeCents: 0,
      payAmountCents: 3000,
    })
    expect(rows[1]).toMatchObject({ label: '赠品', value: '矿泉水 ×1、纸巾 ×2（¥0.00）' })
    expect(rows[2]).toMatchObject({ label: '配送费', value: '免配送费' })
  })

  it('赠品名缺省兜底「赠品」;全零场景只出商品金额/配送费/实付', () => {
    const rows = buildCostRows({
      subtotalCents: 0,
      giftItems: [{ quantity: 1 }],
      deliveryFeeCents: 0,
      payAmountCents: 0,
    })
    expect(rows[1]).toMatchObject({ label: '赠品', value: '赠品 ×1（¥0.00）' })
    expect(rows.map((r) => r.key)).toEqual(['subtotal', 'gift', 'delivery', 'total'])
  })

  it('详情快照:水票覆盖 0 时无抵扣行;押金行展示', () => {
    const rows = buildCostRows({
      subtotalCents: 6500,
      ticketRedeemCount: 3,
      ticketDiscountCents: 0,
      deliveryFeeCents: 800,
      depositCents: 1200,
      payAmountCents: 7000,
    })
    expect(rows.map((r) => r.key)).toEqual(['subtotal', 'delivery', 'deposit', 'total'])
    expect(rows[2]).toMatchObject({ label: '水桶押金', value: '¥12.00' })
    expect(rows[3]).toMatchObject({ label: '实付金额', value: '¥70.00' })
  })

  it('优惠为 0 不产生扣减行', () => {
    const rows = buildCostRows({
      subtotalCents: 1000,
      promotionDiscountCents: 0,
      firstOrderDiscountCents: 0,
      couponDiscountCents: 0,
      ticketRedeemCount: 0,
      ticketDiscountCents: 0,
      deliveryFeeCents: 500,
      payAmountCents: 1500,
    })
    expect(rows.map((r) => r.key)).toEqual(['subtotal', 'delivery', 'total'])
  })
})

describe('ticketDiscountCentsOf / maxTicketRedeem', () => {
  it('抵扣额 = max(0, 小计 - 现金小计)', () => {
    expect(ticketDiscountCentsOf(6500, 1500)).toBe(5000)
    expect(ticketDiscountCentsOf(6500, 6500)).toBe(0)
    expect(ticketDiscountCentsOf('6500', '1000')).toBe(5500)
    expect(ticketDiscountCentsOf(0, 100)).toBe(0)
  })

  it('核销上限 = min(总桶数, 可用张数)', () => {
    expect(maxTicketRedeem(5, 3)).toBe(3)
    expect(maxTicketRedeem(2, 10)).toBe(2)
    expect(maxTicketRedeem(0, 3)).toBe(0)
  })
})

describe('orderCostFromPreview', () => {
  it('preview → 统一快照(水票张数/抵扣额推导)', () => {
    const preview = makePreview({
      subtotalCents: 6500,
      cashSubtotalCents: 1500,
      ticketCoveredQuantity: 3,
      deliveryFeeCents: 800,
      payAmountCents: 5800,
    })
    const snapshot = orderCostFromPreview(preview)
    expect(snapshot.ticketRedeemCount).toBe(3)
    expect(snapshot.ticketDiscountCents).toBe(5000)
    expect(snapshot.subtotalCents).toBe(6500)

    const rows = buildCostRows(snapshot)
    expect(rows[1]).toMatchObject({ label: '水票抵扣（3桶）', value: '-¥50.00', discount: true })
  })
})

describe('minimumOrderHint (R-OPS-03 起送门槛提示)', () => {
  it('未达门槛:返回差额文案', () => {
    const preview = makePreview({ belowMinimum: true, minimumOrderCents: 3000, subtotalCents: 2500 })
    expect(minimumOrderHint(preview)).toBe('商品金额未达起送金额 ¥30.00，还差 ¥5.00')
  })

  it('已达/等于门槛(belowMinimum=false):返回 null', () => {
    expect(minimumOrderHint(makePreview({ belowMinimum: false, minimumOrderCents: 3000, subtotalCents: 3000 }))).toBeNull()
    expect(minimumOrderHint(makePreview({ belowMinimum: false, minimumOrderCents: 3000, subtotalCents: 4000 }))).toBeNull()
  })

  it('门槛未启用(0):即使 belowMinimum 也返回 null', () => {
    expect(minimumOrderHint(makePreview({ belowMinimum: true, minimumOrderCents: 0, subtotalCents: 0 }))).toBeNull()
  })
})
