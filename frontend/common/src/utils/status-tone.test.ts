/**
 * status-tone 单测:值与设计系统 §2.2 一致、各域映射覆盖枚举、getStatusTone 兜底。
 */
import { describe, expect, it } from '@jest/globals'
import {
  FeedbackStatus,
  OrderStatus,
  UserCouponStatus,
  UserTicketStatus,
} from '../types/graphql'
import {
  COUPON_STATUS_TONE,
  FEEDBACK_STATUS_TONE,
  ORDER_STATUS_TONE,
  STATUS_TONE_COLORS,
  TICKET_STATUS_TONE,
  getStatusTone,
} from './status-tone'

describe('STATUS_TONE_COLORS(§2.2)', () => {
  it('各 tone 提供 text 与 10% tint bg,且值符合设计系统', () => {
    expect(STATUS_TONE_COLORS.info).toEqual({ text: '#3A9BD9', bg: 'rgba(58, 155, 217, 0.10)' })
    expect(STATUS_TONE_COLORS.success).toEqual({ text: '#389E0D', bg: 'rgba(82, 196, 26, 0.10)' })
    expect(STATUS_TONE_COLORS.warning).toEqual({ text: '#FA8C16', bg: 'rgba(250, 173, 20, 0.10)' })
    expect(STATUS_TONE_COLORS.neutral).toEqual({ text: '#8C8C8C', bg: 'rgba(140, 140, 140, 0.10)' })
    expect(STATUS_TONE_COLORS.error).toEqual({ text: '#CF1322', bg: 'rgba(255, 77, 79, 0.10)' })
    expect(STATUS_TONE_COLORS.accent).toEqual({ text: '#FF5436', bg: 'rgba(255, 84, 54, 0.10)' })
  })

  it('六个 tone 键齐全', () => {
    expect(Object.keys(STATUS_TONE_COLORS).sort()).toEqual([
      'accent',
      'error',
      'info',
      'neutral',
      'success',
      'warning',
    ])
  })
})

describe('域映射覆盖枚举', () => {
  it('ORDER_STATUS_TONE 覆盖全部 OrderStatus', () => {
    expect(Object.keys(ORDER_STATUS_TONE).sort()).toEqual(Object.values(OrderStatus).sort())
    expect(ORDER_STATUS_TONE[OrderStatus.PENDING_PAYMENT]).toBe('warning')
    expect(ORDER_STATUS_TONE[OrderStatus.PENDING_DISPATCH]).toBe('info')
    expect(ORDER_STATUS_TONE[OrderStatus.PENDING_DELIVERY]).toBe('info')
    expect(ORDER_STATUS_TONE[OrderStatus.DELIVERING]).toBe('success')
    expect(ORDER_STATUS_TONE[OrderStatus.AWAITING_CONFIRMATION]).toBe('success')
    expect(ORDER_STATUS_TONE[OrderStatus.COMPLETED]).toBe('neutral')
    expect(ORDER_STATUS_TONE[OrderStatus.CANCELLED]).toBe('error')
    expect(ORDER_STATUS_TONE[OrderStatus.REFUNDED]).toBe('error')
  })

  it('FEEDBACK_STATUS_TONE 覆盖全部 FeedbackStatus', () => {
    expect(Object.keys(FEEDBACK_STATUS_TONE).sort()).toEqual(Object.values(FeedbackStatus).sort())
    expect(FEEDBACK_STATUS_TONE[FeedbackStatus.PENDING]).toBe('warning')
    expect(FEEDBACK_STATUS_TONE[FeedbackStatus.PROCESSING]).toBe('info')
    expect(FEEDBACK_STATUS_TONE[FeedbackStatus.RESOLVED]).toBe('success')
  })

  it('COUPON_STATUS_TONE / TICKET_STATUS_TONE 覆盖各自枚举', () => {
    expect(Object.keys(COUPON_STATUS_TONE).sort()).toEqual(Object.values(UserCouponStatus).sort())
    expect(Object.keys(TICKET_STATUS_TONE).sort()).toEqual(Object.values(UserTicketStatus).sort())
    expect(COUPON_STATUS_TONE[UserCouponStatus.RECEIVED]).toBe('info')
    expect(COUPON_STATUS_TONE[UserCouponStatus.USED]).toBe('success')
    expect(COUPON_STATUS_TONE[UserCouponStatus.RETURNED]).toBe('warning')
    expect(COUPON_STATUS_TONE[UserCouponStatus.EXPIRED]).toBe('neutral')
    expect(TICKET_STATUS_TONE[UserTicketStatus.UNUSED]).toBe('info')
  })
})

describe('getStatusTone', () => {
  it('命中返回 tone,未命中/空返回 undefined', () => {
    expect(getStatusTone(ORDER_STATUS_TONE, 'DELIVERING')).toBe('success')
    expect(getStatusTone(ORDER_STATUS_TONE, 'UNKNOWN')).toBeUndefined()
    expect(getStatusTone(ORDER_STATUS_TONE, null)).toBeUndefined()
    expect(getStatusTone(ORDER_STATUS_TONE, undefined)).toBeUndefined()
  })
})
