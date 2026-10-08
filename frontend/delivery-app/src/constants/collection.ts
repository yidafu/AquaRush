/** 每日收款容差阈值（分）：现金/扫码差异 ≤ 1 元视为正常。见 weapp daily-collection。 */
export const TOLERANCE_CENTS = 100;

/** 每日收款状态 → 文案。 */
export const DAILY_COLLECTION_STATUS_MAP: Record<string, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  CONFIRMED: '已确认',
  DISPUTED: '有争议',
};

/** 对账记录状态 → 文案。 */
export const RECONCILIATION_STATUS_MAP: Record<string, string> = {
  PENDING: '待对账',
  MATCHED: '匹配',
  DISCREPANCY: '有差异',
  REVIEWED: '已复核',
};

// 每日收款/对账状态 → 标签 tone —— 统一从 common 取（单来源,见 common/utils/status-tone）
export {
  DAILY_COLLECTION_STATUS_TONE,
  RECONCILIATION_STATUS_TONE,
} from '@aquarush/common/utils/status-tone';
