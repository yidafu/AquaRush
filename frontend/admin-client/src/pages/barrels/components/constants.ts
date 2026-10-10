import type {
  DepositPriceStatus,
  DepositRefundApplicationStatus,
  DepositStatus,
} from '../../../types/barrel';

/** 押金价目状态展示(G06) */
export const DEPOSIT_PRICE_STATUS_MAP: Record<DepositPriceStatus, { text: string; color: string }> = {
  ENABLED: { text: '启用', color: 'processing' },
  DISABLED: { text: '停用', color: 'default' },
};

/** 押金记录状态展示(R-MON-03:收取 → 在租 → 退还) */
export const DEPOSIT_RECORD_STATUS_MAP: Record<DepositStatus, { text: string; color: string }> = {
  PENDING: { text: '待收取', color: 'warning' },
  HELD: { text: '在租', color: 'processing' },
  REFUNDED: { text: '已退还', color: 'success' },
};

/** 退押申请状态展示(R-PRO-04:待处理 → 已回收 → 已退还 / 已驳回) */
export const REFUND_APPLICATION_STATUS_MAP: Record<
  DepositRefundApplicationStatus,
  { text: string; color: string }
> = {
  PENDING: { text: '待处理', color: 'warning' },
  RECOVERED: { text: '已回收', color: 'blue' },
  REFUNDED: { text: '已退还', color: 'success' },
  REJECTED: { text: '已驳回', color: 'red' },
};
