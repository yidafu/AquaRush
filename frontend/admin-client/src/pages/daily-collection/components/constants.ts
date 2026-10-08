import type { DailyCollectionStatus, DailyReconciliationStatus } from '../../../types/daily-collection';

// 收款记录状态颜色/文案(收银对账管理页)
export const statusColorMap: Record<DailyCollectionStatus, string> = {
  DRAFT: 'default',
  SUBMITTED: 'processing',
  CONFIRMED: 'success',
  DISPUTED: 'error',
};

export const statusTextMap: Record<DailyCollectionStatus, string> = {
  DRAFT: '草稿',
  SUBMITTED: '已提交',
  CONFIRMED: '已确认',
  DISPUTED: '有争议',
};

// 对账记录状态颜色/文案
export const reconciliationStatusColorMap: Record<DailyReconciliationStatus, string> = {
  PENDING: 'default',
  MATCHED: 'success',
  DISCREPANCY: 'warning',
  REVIEWED: 'processing',
};

export const reconciliationStatusTextMap: Record<DailyReconciliationStatus, string> = {
  PENDING: '待对账',
  MATCHED: '匹配',
  DISCREPANCY: '有差异',
  REVIEWED: '已复核',
};
