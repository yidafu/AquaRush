/**
 * 支付对账页纯函数单测(F-ORD-031/R-PAY-04):
 * 任务/差异状态文案映射、差异语义映射(短款/长款/金额不一致)、
 * 账单日窗口校验(T-1~T-30)、差异两侧金额提取。
 */
import { describe, expect, it } from '@jest/globals'
import {
  DISCREPANCY_STATUS_MAP,
  TASK_STATUS_MAP,
  discrepancyLabel,
  extractDiscrepancyAmounts,
  isWithinReconciliationWindow,
} from './helpers'

describe('TASK_STATUS_MAP', () => {
  it('映射五种任务状态文案(差异数>0 的任务仍为「成功」)', () => {
    expect(TASK_STATUS_MAP.SUCCESS).toEqual({ text: '成功', color: 'success' })
    expect(TASK_STATUS_MAP.FAILED).toEqual({ text: '失败', color: 'error' })
    expect(TASK_STATUS_MAP.RUNNING).toEqual({ text: '执行中', color: 'processing' })
    expect(TASK_STATUS_MAP.PENDING).toEqual({ text: '待执行', color: 'default' })
    expect(TASK_STATUS_MAP.SKIPPED).toEqual({ text: '跳过', color: 'default' })
  })
})

describe('DISCREPANCY_STATUS_MAP', () => {
  it('映射三种处理状态(未处置持续可见,已解决/已忽略)', () => {
    expect(DISCREPANCY_STATUS_MAP.UNRESOLVED.text).toBe('未处置')
    expect(DISCREPANCY_STATUS_MAP.RESOLVED.text).toBe('已解决')
    expect(DISCREPANCY_STATUS_MAP.IGNORED.text).toBe('已忽略')
  })
})

describe('discrepancyLabel', () => {
  it('MISSING+WECHAT 映射为「短款(微信有本地无)」', () => {
    expect(discrepancyLabel('MISSING', 'WECHAT')).toEqual({ text: '短款', description: '微信有本地无' })
  })
  it('MISSING+INTERNAL 映射为「长款(本地有微信无)」', () => {
    expect(discrepancyLabel('MISSING', 'INTERNAL')).toEqual({ text: '长款', description: '本地有微信无' })
  })
  it('MISMATCH 映射为「金额不一致」', () => {
    expect(discrepancyLabel('MISMATCH', 'INTERNAL')).toEqual({ text: '金额不一致' })
    expect(discrepancyLabel('MISMATCH', 'WECHAT')).toEqual({ text: '金额不一致' })
  })
  it('EXTRA 兜底为「多余记录」', () => {
    expect(discrepancyLabel('EXTRA', 'WECHAT').text).toBe('多余记录')
  })
})

describe('isWithinReconciliationWindow', () => {
  const today = new Date('2026-10-11T10:30:00')

  it('T-1 与 T-30 可手动对账', () => {
    expect(isWithinReconciliationWindow('2026-10-10', today)).toBe(true)
    expect(isWithinReconciliationWindow('2026-09-11', today)).toBe(true)
  })
  it('当日与窗口外(T-31/未来日期)拒绝', () => {
    expect(isWithinReconciliationWindow('2026-10-11', today)).toBe(false)
    expect(isWithinReconciliationWindow('2026-09-10', today)).toBe(false)
    expect(isWithinReconciliationWindow('2026-10-12', today)).toBe(false)
  })
  it('非法格式与空值拒绝', () => {
    expect(isWithinReconciliationWindow('', today)).toBe(false)
    expect(isWithinReconciliationWindow('2026/10/01', today)).toBe(false)
    expect(isWithinReconciliationWindow('not-a-date', today)).toBe(false)
  })
})

describe('extractDiscrepancyAmounts', () => {
  it('金额不一致差异:微信/本地两侧金额分别提取(分)', () => {
    expect(
      extractDiscrepancyAmounts({
        wechat: { amountCents: 8800 },
        internal: { amountCents: 7600 },
      }),
    ).toEqual({ wechatCents: 8800, localCents: 7600 })
  })
  it('短款差异:本地侧缺失返回 null(展示为「—」)', () => {
    expect(extractDiscrepancyAmounts({ wechat: { amountCents: 3600 } })).toEqual({
      wechatCents: 3600,
      localCents: null,
    })
  })
  it('长款差异:微信侧缺失回退顶层 amountCents', () => {
    expect(extractDiscrepancyAmounts({ internal: { amountCents: 1000 }, amountCents: null })).toEqual({
      wechatCents: null,
      localCents: 1000,
    })
  })
  it('null 与空对象安全兜底', () => {
    expect(extractDiscrepancyAmounts(null)).toEqual({ wechatCents: null, localCents: null })
    expect(extractDiscrepancyAmounts({})).toEqual({ wechatCents: null, localCents: null })
  })
})
