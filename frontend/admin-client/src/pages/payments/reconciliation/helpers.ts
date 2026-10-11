/**
 * 微信支付对账页纯函数辅助(F-ORD-031/R-PAY-04,G93)
 * 状态/差异类型文案与语义色、账单日窗口校验(T-1~T-30)、差异金额提取。
 * 金额一律分(number),展示层才格式化(R-MON-01)。
 */
import type {
  DiscrepancyAmounts,
  DiscrepancyStatus,
  DiscrepancyType,
  ReconciliationTaskStatus,
  SourceSystem,
} from '../../../types/reconciliation'

/** 任务状态 → 文案与语义色(管理端不出现强调橙;成功绿/失败红/执行中蓝/跳过灰) */
export const TASK_STATUS_MAP: Record<ReconciliationTaskStatus, { text: string; color: string }> = {
  PENDING: { text: '待执行', color: 'default' },
  RUNNING: { text: '执行中', color: 'processing' },
  SUCCESS: { text: '成功', color: 'success' },
  FAILED: { text: '失败', color: 'error' },
  SKIPPED: { text: '跳过', color: 'default' },
}

/** 差异处理状态 → 文案与语义色(未处置持续可见,不自动闭环) */
export const DISCREPANCY_STATUS_MAP: Record<DiscrepancyStatus, { text: string; color: string }> = {
  UNRESOLVED: { text: '未处置', color: 'warning' },
  RESOLVED: { text: '已解决', color: 'success' },
  IGNORED: { text: '已忽略', color: 'default' },
}

/**
 * 差异类型 + 来源系统 → 设计稿语义文案(spec/design/_spec/04-order.md §页面25):
 * MISSING+WECHAT=短款(微信有本地无,资损风险最高) / MISSING+INTERNAL=长款(本地有微信无) /
 * MISMATCH=金额不一致;退款账单「已退成功但本地无 COMPLETED 记录」按 MISSING+WECHAT 呈现,
 * 处置时提示核实退款结果(状态不一致语义)。
 */
export function discrepancyLabel(
  type: DiscrepancyType,
  source: SourceSystem,
): { text: string; description?: string } {
  if (type === 'MISMATCH') {
    return { text: '金额不一致' }
  }
  if (type === 'MISSING') {
    return source === 'WECHAT'
      ? { text: '短款', description: '微信有本地无' }
      : { text: '长款', description: '本地有微信无' }
  }
  return { text: '多余记录' }
}

/**
 * 手动对账账单日窗口校验:仅支持近 30 天账单(T-1 ~ T-30,R-PAY-04 开放决策 §13-3)。
 * today 供注入(yyyy-MM-dd 或 Date),便于测试。
 */
export function isWithinReconciliationWindow(
  billDate: string,
  today: Date = new Date(),
): boolean {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(billDate)) return false
  const date = new Date(`${billDate}T00:00:00`)
  if (Number.isNaN(date.getTime())) return false
  const startOfDay = (d: Date) => new Date(d.getFullYear(), d.getMonth(), d.getDate())
  const dayMs = 24 * 60 * 60 * 1000
  const todayStart = startOfDay(today).getTime()
  const diffDays = Math.round((todayStart - date.getTime()) / dayMs)
  return diffDays >= 1 && diffDays <= 30
}

/** recordDetails 中提取微信/本地两侧金额(分;缺失侧返回 null,展示为「—」) */
export function extractDiscrepancyAmounts(details: DiscrepancyAmounts | null): {
  wechatCents: number | null
  localCents: number | null
} {
  if (!details) return { wechatCents: null, localCents: null }
  const toCents = (v: unknown): number | null => {
    if (v === undefined || v === null || v === '') return null
    const n = Number(v)
    return Number.isFinite(n) ? n : null
  }
  const wechatCents =
    toCents(details.wechat?.amountCents) ??
    toCents(details.wechat?.refundFeeCents) ??
    toCents(details.amountCents)
  const localCents = toCents(details.internal?.amountCents)
  return { wechatCents, localCents }
}
