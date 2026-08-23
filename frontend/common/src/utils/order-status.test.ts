/**
 * order-status 单测:文案全覆盖枚举、进度步骤、Tab 口径、getStatusView 派生。
 */
import { describe, expect, it } from '@jest/globals'
import { OrderStatus } from '../types/graphql'
import {
  ORDER_STEPS,
  ORDER_TABS,
  ORDER_STATUS_TEXT,
  getCurrentStep,
  getOrderStatusText,
  getStatusView,
} from './order-status'

describe('ORDER_STATUS_TEXT', () => {
  it('覆盖全部 OrderStatus 且文案统一', () => {
    expect(Object.keys(ORDER_STATUS_TEXT).sort()).toEqual(Object.values(OrderStatus).sort())
    expect(ORDER_STATUS_TEXT[OrderStatus.PENDING_PAYMENT]).toBe('待付款')
    expect(ORDER_STATUS_TEXT[OrderStatus.PENDING_DISPATCH]).toBe('待派单')
    expect(ORDER_STATUS_TEXT[OrderStatus.PENDING_DELIVERY]).toBe('待配送')
    expect(ORDER_STATUS_TEXT[OrderStatus.DELIVERING]).toBe('配送中')
    expect(ORDER_STATUS_TEXT[OrderStatus.COMPLETED]).toBe('已完成')
    expect(ORDER_STATUS_TEXT[OrderStatus.CANCELLED]).toBe('已取消')
    expect(ORDER_STATUS_TEXT[OrderStatus.REFUNDED]).toBe('已退款')
  })

  it('getOrderStatusText 未命中/空返回 undefined', () => {
    expect(getOrderStatusText(OrderStatus.DELIVERING)).toBe('配送中')
    expect(getOrderStatusText('UNKNOWN')).toBeUndefined()
    expect(getOrderStatusText(null)).toBeUndefined()
  })
})

describe('ORDER_STEPS / getCurrentStep', () => {
  it('步骤文案 = 待派单/待配送/配送中/已完成', () => {
    expect(ORDER_STEPS).toEqual(['待派单', '待配送', '配送中', '已完成'])
  })

  it('进度下标:待派单=0 … 已完成=3,其余=0', () => {
    expect(getCurrentStep(OrderStatus.PENDING_DISPATCH)).toBe(0)
    expect(getCurrentStep(OrderStatus.PENDING_DELIVERY)).toBe(1)
    expect(getCurrentStep(OrderStatus.DELIVERING)).toBe(2)
    expect(getCurrentStep(OrderStatus.COMPLETED)).toBe(3)
    expect(getCurrentStep(OrderStatus.PENDING_PAYMENT)).toBe(0)
    expect(getCurrentStep(OrderStatus.CANCELLED)).toBe(0)
    expect(getCurrentStep(null)).toBe(0)
  })
})

describe('ORDER_TABS', () => {
  it('「待发货」口径 = 待派单 + 待配送', () => {
    expect(ORDER_TABS.map((t) => t.title)).toEqual(['全部', '待付款', '待配送', '配送中', '已完成'])
    expect(ORDER_TABS[0].statuses).toBeNull()
    expect(ORDER_TABS[2].statuses).toEqual([OrderStatus.PENDING_DISPATCH, OrderStatus.PENDING_DELIVERY])
  })
})

describe('getStatusView', () => {
  it('派生 text/tone/step', () => {
    expect(getStatusView(OrderStatus.DELIVERING)).toEqual({ text: '配送中', tone: 'success', step: 2 })
    expect(getStatusView(OrderStatus.CANCELLED)).toEqual({ text: '已取消', tone: 'error', step: 0 })
    expect(getStatusView(null)).toEqual({ text: undefined, tone: undefined, step: 0 })
  })
})
