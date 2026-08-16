/**
 * date 工具单测
 */
import { describe, expect, it } from '@jest/globals'
import { formatDate, formatDateTime, formatTime } from './date'

describe('formatDateTime', () => {
  it('格式化完整日期时间 yyyy-MM-dd HH:mm', () => {
    expect(formatDateTime('2026-01-05T09:08:07')).toBe('2026-01-05 09:08')
    expect(formatDateTime('2026-12-31T23:59:59')).toBe('2026-12-31 23:59')
  })

  it('月份/日期/时分补零', () => {
    expect(formatDateTime('2026-01-03T00:05:00')).toBe('2026-01-03 00:05')
  })

  it('空字符串返回空串', () => {
    expect(formatDateTime('')).toBe('')
  })

  it('非法日期原样返回', () => {
    expect(formatDateTime('not-a-date')).toBe('not-a-date')
  })
})

describe('formatDate', () => {
  it('格式化仅日期 yyyy-MM-dd', () => {
    expect(formatDate('2026-01-05T09:08:07')).toBe('2026-01-05')
    expect(formatDate('2026-11-30T00:00:00')).toBe('2026-11-30')
  })

  it('空字符串返回空串', () => {
    expect(formatDate('')).toBe('')
  })

  it('非法日期原样返回', () => {
    expect(formatDate('oops')).toBe('oops')
  })
})

describe('formatTime', () => {
  it('格式化仅时间 HH:mm', () => {
    expect(formatTime('2026-01-05T09:08:07')).toBe('09:08')
    expect(formatTime('2026-01-05T18:30:00')).toBe('18:30')
  })

  it('空字符串返回空串', () => {
    expect(formatTime('')).toBe('')
  })

  it('非法日期原样返回', () => {
    expect(formatTime('invalid')).toBe('invalid')
  })
})
