/**
 * order-view 纯映射单测:items 归一/营销价判定、时间轴、地址、状态标志、费用快照。
 */
import { describe, expect, it } from '@jest/globals'
import { OrderVo, OrderStatus } from '../types/graphql'
import { orderCostSnapshotFromOrder } from '../utils/order-cost'
import {
  buildOrderTimeline,
  statusFlagsOf,
  toOrderDetailView,
} from './order-view'

const makeOrder = (overrides: Partial<OrderVo> = {}): OrderVo => ({
  id: '1',
  orderNo: 'NO-1',
  status: OrderStatus.DELIVERING,
  amountCents: 6500,
  createdAt: '2026-08-01T10:00:00',
  deliveryFeeCents: 800,
  depositCents: 0,
  payAmountCents: 5800,
  isSelfCollect: false,
  items: [
    {
      productId: 'p1',
      quantity: 2,
      unitPriceCents: 2500,
      totalPriceCents: 5000,
      product: {
        id: 'p1',
        name: '天然矿泉水',
        coverImageUrl: '/img/a.png',
        priceCents: 3000,
        effectivePriceCents: 2500,
        specification: '18.9L',
        hasGroupPrice: false,
        priceSource: 'GROUP_PRICE' as OrderVo['items'][number]['product']['priceSource'],
        salesVolume: 10,
        sortOrder: 1,
      } as any,
    },
  ],
  updatedAt: '2026-08-01T10:00:00',
  ...overrides,
})

describe('toOrderDetailView.items', () => {
  it('归一成交价/目录价,命中营销价时带划线价', () => {
    const view = toOrderDetailView(makeOrder())
    expect(view.items[0]).toMatchObject({
      id: 'p1',
      name: '天然矿泉水',
      image: '/img/a.png',
      unitPriceCents: 2500,
      catalogPriceCents: 3000,
      hasMarketingPrice: true,
      quantity: 2,
      specification: '18.9L',
    })
  })

  it('成交价=目录价时不带划线价', () => {
    const view = toOrderDetailView(
      makeOrder({ items: [{ productId: 'p1', quantity: 1, unitPriceCents: 3000, totalPriceCents: 3000, product: { priceCents: 3000 } as any }] })
    )
    expect(view.items[0].hasMarketingPrice).toBe(false)
    expect(view.items[0].catalogPriceCents).toBeUndefined()
  })

  it('成交价缺省回退商品价;商品名/图缺省兜底', () => {
    const view = toOrderDetailView(
      makeOrder({ items: [{ productId: 'p9', quantity: 1, unitPriceCents: 0 as any, totalPriceCents: 0, product: undefined as any }] })
    )
    expect(view.items[0]).toMatchObject({ name: '商品', image: '', unitPriceCents: 0, quantity: 1 })
  })
})

describe('buildOrderTimeline', () => {
  it('按已发生节点顺序组装,缺省不出现', () => {
    const timeline = buildOrderTimeline(
      makeOrder({
        createdAt: '2026-08-01T10:00:00',
        paymentTime: '2026-08-01T10:01:00',
        deliveryStartedAt: '2026-08-01T11:00:00',
        completedAt: '2026-08-01T12:00:00',
      })
    )
    expect(timeline.map((t) => t.title)).toEqual(['订单创建', '支付成功', '开始配送', '已送达', '订单完成'])
    expect(timeline.map((t) => t.icon)).toEqual(['order-created', 'paid', 'delivery-started', 'delivered', 'completed'])
    expect(timeline.map((t) => t.tone)).toEqual(['info', 'accent', 'success', 'success', 'success'])
  })

  it('已完成订单(送达即完成):已送达与订单完成同为 completedAt 时间', () => {
    const timeline = buildOrderTimeline(
      makeOrder({
        status: OrderStatus.COMPLETED,
        createdAt: '2026-08-01T10:00:00',
        deliveryStartedAt: '2026-08-01T11:00:00',
        completedAt: '2026-08-01T11:30:00',
      })
    )
    expect(timeline.map((t) => t.title)).toEqual(['订单创建', '开始配送', '已送达', '订单完成'])
    expect(timeline[2].content).toBe('商品已送达')
    expect(timeline[2].time).toBe('2026-08-01T11:30:00')
  })

  it('无时间节点(createdAt 为空)返回空', () => {
    expect(buildOrderTimeline(makeOrder({ createdAt: '' as any }))).toEqual([])
  })

  it('仅 createdAt 时只出订单创建', () => {
    expect(buildOrderTimeline(makeOrder()).map((t) => t.title)).toEqual(['订单创建'])
  })
})

describe('address / flags', () => {
  it('地址映射与 null 兜底', () => {
    const withAddr = toOrderDetailView(
      makeOrder({
        address: {
          receiverName: '张三',
          phone: '13800000000',
          province: '浙江省',
          city: '杭州市',
          district: '西湖区',
          detailAddress: '文三路 1 号',
        } as any,
      })
    )
    expect(withAddr.address).toEqual({
      receiverName: '张三',
      phone: '13800000000',
      province: '浙江省',
      city: '杭州市',
      district: '西湖区',
      detailAddress: '文三路 1 号',
    })
    expect(toOrderDetailView(makeOrder()).address).toBeNull()
  })

  it('statusFlagsOf 各状态标志正确', () => {
    expect(statusFlagsOf(OrderStatus.PENDING_PAYMENT)).toMatchObject({ isPendingPayment: true })
    expect(statusFlagsOf(OrderStatus.PENDING_DISPATCH)).toMatchObject({ isPendingDelivery: true })
    expect(statusFlagsOf(OrderStatus.PENDING_DELIVERY)).toMatchObject({ isPendingDelivery: true })
    expect(statusFlagsOf(OrderStatus.DELIVERING)).toMatchObject({ isDelivering: true })
    expect(statusFlagsOf(OrderStatus.COMPLETED)).toMatchObject({ isCompleted: true })
    expect(statusFlagsOf(OrderStatus.CANCELLED)).toMatchObject({ isCancelledOrRefunded: true })
    expect(statusFlagsOf(OrderStatus.REFUNDED)).toMatchObject({ isCancelledOrRefunded: true })
  })
})

describe('orderCostSnapshotFromOrder', () => {
  it('订单快照 → 费用快照(营销字段归一)', () => {
    const order = makeOrder({
      amountCents: 6500,
      deliveryFeeCents: 800,
      depositCents: 1200,
      payAmountCents: 7000,
      ticketRedeemCount: 1,
      ticketDiscountCents: 1000,
      promotionDiscountCents: 500,
      promotionActivityName: '满减A',
      firstOrderDiscountCents: 300,
      firstOrderActivityName: '新人',
      couponDiscountCents: 200,
      couponName: '券B',
      giftItems: [{ productId: 'g1', productName: '赠品1', quantity: 1 }],
    })
    const snapshot = orderCostSnapshotFromOrder(order)
    expect(snapshot.subtotalCents).toBe(6500)
    expect(snapshot.ticketRedeemCount).toBe(1)
    expect(snapshot.ticketDiscountCents).toBe(1000)
    expect(snapshot.promotionActivityName).toBe('满减A')
    expect(snapshot.couponName).toBe('券B')
    expect(snapshot.depositCents).toBe(1200)
    expect(snapshot.payAmountCents).toBe(7000)
  })
})
