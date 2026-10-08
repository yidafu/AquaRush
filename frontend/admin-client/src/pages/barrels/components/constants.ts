import type { BarrelFeeStatus } from '../../../types/barrel';

/** 价目状态展示(G06) */
export const FEE_STATUS_MAP: Record<BarrelFeeStatus, { text: string; color: string }> = {
  ENABLED: { text: '启用', color: 'processing' },
  DISABLED: { text: '停用', color: 'default' },
};

/** 购桶单状态展示(G06:一次性塑料水桶费,独立支付) */
export const PURCHASE_STATUS_MAP: Record<string, { text: string; color: string }> = {
  PENDING: { text: '待支付', color: 'warning' },
  PAID: { text: '已支付', color: 'processing' },
  CANCELLED: { text: '已取消', color: 'default' },
  REFUNDED: { text: '已退还', color: 'success' },
};
