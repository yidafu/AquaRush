/**
 * 支付对账页共享常量与行类型(F-ORD-031/R-PAY-04,由 index.tsx 拆分)。
 */
import type {
  DiscrepancyType,
  ReconciliationDiscrepancy,
  ReconciliationTaskType,
} from '../../../../types/reconciliation';

export const PAGE_SIZE = 10;

/** 任务类型 → 标签文案(设计稿:支付账单/退款账单/结算账单) */
export const TASK_TYPE_MAP: Record<ReconciliationTaskType, { text: string; color: string }> = {
  PAYMENT: { text: '支付账单', color: 'blue' },
  REFUND: { text: '退款账单', color: 'purple' },
  SETTLEMENT: { text: '结算账单', color: 'geekblue' },
};

/** 触发方式文案(F-ORD-031 详情「触发方式」) */
export const TRIGGER_TYPE_TEXT: Record<string, string> = {
  SCHEDULED: '每日调度(02:00/02:05)',
  MANUAL: '管理员手动',
};

/** 差异类型筛选选项 */
export const DISCREPANCY_TYPE_OPTIONS: { value: DiscrepancyType; label: string }[] = [
  { value: 'MISSING', label: '缺失(短款/长款)' },
  { value: 'MISMATCH', label: '金额不一致' },
  { value: 'EXTRA', label: '多余记录' },
];

export interface DiscrepancyRow extends ReconciliationDiscrepancy {
  wechatCents: number | null;
  localCents: number | null;
}
