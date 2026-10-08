import type { StatusTone } from '@aquarush/common';

/**
 * StatusTone(common 语义 tone)→ AntD Tag 预设色名 —— 管理端渲染桥。
 *
 * 状态→tone 的权威定义在 @aquarush/common/utils/status-tone(ORDER_STATUS_TONE /
 * FEEDBACK_STATUS_TONE / COUPON_STATUS_TONE / TICKET_STATUS_TONE 等),本模块只负责
 * 把语义 tone 落到 AntD 的 preset 色名,不存任何状态文本与业务色值字面量。
 *
 * accent 为消费者端强调橙,管理端禁用(见 common status-tone §2.1 红线);此处兜底给
 * 'volcano' 仅为类型完整性,业务代码不应使用。
 */
export const toneToAntDColor: Record<StatusTone, string> = {
  info: 'blue',
  success: 'success',
  warning: 'warning',
  error: 'error',
  neutral: 'default',
  accent: 'volcano',
};

/** 从 tone 取 AntD 色名;未命中兜底 'default'(未知状态) */
export function getToneColor(tone: StatusTone | undefined): string {
  return tone ? toneToAntDColor[tone] : 'default';
}
