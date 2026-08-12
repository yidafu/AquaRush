import type { StatusTone } from '@/constants/theme';

/** 每日收款容差阈值（分）：现金/扫码差异 ≤ 1 元视为正常。见 weapp daily-collection。 */
export const TOLERANCE_CENTS = 100;

/** 每日收款状态 → 文案。 */
export const DAILY_COLLECTION_STATUS_MAP: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  CONFIRMED: '已确认',
  DISPUTED: '有争议',
};

/** 每日收款状态 → 标签 tone（DRAFT 用 textTertiary 纯文本渲染）。 */
export const DAILY_COLLECTION_STATUS_TONE: Record<string, StatusTone> = {
  SUBMITTED: 'processing',
  CONFIRMED: 'success',
  DISPUTED: 'error',
};

/** 对账记录状态 → 文案。 */
export const RECONCILIATION_STATUS_MAP: Record<string, string> = {
  PENDING: '待对账',
  MATCHED: '匹配',
  DISCREPANCY: '有差异',
  REVIEWED: '已复核',
};

/** 对账记录状态 → 标签 tone。 */
export const RECONCILIATION_STATUS_TONE: Record<string, StatusTone> = {
  PENDING: 'warning',
  MATCHED: 'success',
  DISCREPANCY: 'error',
  REVIEWED: 'processing',
};
