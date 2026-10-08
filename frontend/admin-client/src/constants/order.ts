import { ORDER_STATUS_TEXT, ORDER_STATUS_TONE } from '@aquarush/common';
import type { OrderStatus, StatusTone } from '@aquarush/common';

/**
 * 订单状态展示配置 —— 文本与 tone 全部来自 common(文本见 order-status.ts,
 * tone 见 status-tone.ts),颜色由 constants/status.ts 的 toneToAntDColor 桥接。
 * 页面禁止再定义本地订单状态颜色/文本字面量。
 */
export const ORDER_STATUS_CONFIG: {
  text: Record<OrderStatus, string>;
  tone: Record<OrderStatus, StatusTone>;
} = {
  text: ORDER_STATUS_TEXT,
  tone: ORDER_STATUS_TONE,
};

/** 按状态取展示信息(未知状态回退为原样文本 + neutral tone) */
export function getOrderStatusConfig(status: string | null | undefined): {
  text: string;
  tone: StatusTone;
} {
  if (status == null) return { text: '-', tone: 'neutral' };
  const text = ORDER_STATUS_TEXT[status as OrderStatus];
  const tone = ORDER_STATUS_TONE[status as OrderStatus];
  if (!text) return { text: status, tone: 'neutral' };
  return { text, tone };
}
