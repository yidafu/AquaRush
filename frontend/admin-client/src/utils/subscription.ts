/**
 * 周期订购计划展示工具(F-ORD-026/G77):
 * 状态/时间段文案与周期规则格式化为纯函数,供列表页与单测共用。
 * 文案口径对齐 spec/design/admin/02-order/subscriptions.html(生效中/已暂停/已终止)。
 */

export type SubscriptionStatusValue = 'ACTIVE' | 'PAUSED' | 'TERMINATED';

/** 计划状态 → 中文文案(设计稿状态标签) */
export const SUBSCRIPTION_STATUS_TEXT: Record<SubscriptionStatusValue, string> = {
  ACTIVE: '生效中',
  PAUSED: '已暂停',
  TERMINATED: '已终止',
};

/** 计划状态 → AntD Tag 色(生效中成功绿/已暂停 warning/已终止中性灰,设计稿红线) */
export const SUBSCRIPTION_STATUS_COLOR: Record<SubscriptionStatusValue, string> = {
  ACTIVE: 'success',
  PAUSED: 'warning',
  TERMINATED: 'default',
};

/** 按状态取文案(未知状态回退原样),供表格列渲染 */
export function getSubscriptionStatusText(status: string | null | undefined): string {
  if (status == null) return '-';
  return SUBSCRIPTION_STATUS_TEXT[status as SubscriptionStatusValue] ?? status;
}

/** 按状态取 Tag 色名(未知状态回退 default) */
export function getSubscriptionStatusColor(status: string | null | undefined): string {
  if (status == null) return 'default';
  return SUBSCRIPTION_STATUS_COLOR[status as SubscriptionStatusValue] ?? 'default';
}

/** 配送时间段 → 展示文案(时段区间口径同设计稿/用户端 deliverySlots) */
export const TIME_SLOT_TEXT: Record<string, string> = {
  IMMEDIATE: '立即配送',
  MORNING: '上午 9:00-12:00',
  AFTERNOON: '下午 14:00-18:00',
  EVENING: '晚上 18:00-21:00',
};

/** 新建计划表单下拉(短文案:立即配送/上午/下午/晚上) */
export const TIME_SLOT_OPTIONS: { value: string; label: string }[] = [
  { value: 'IMMEDIATE', label: '立即配送' },
  { value: 'MORNING', label: '上午' },
  { value: 'AFTERNOON', label: '下午' },
  { value: 'EVENING', label: '晚上' },
];

/** ISO 星期(1=周一 .. 7=周日)→ 中文 */
export const WEEKDAY_TEXT: Record<number, string> = {
  1: '周一',
  2: '周二',
  3: '周三',
  4: '周四',
  5: '周五',
  6: '周六',
  7: '周日',
};

/** 新建计划表单:星期多选选项(1=周一 .. 7=周日) */
export const WEEKDAY_OPTIONS: { value: number; label: string }[] = [1, 2, 3, 4, 5, 6, 7].map((value) => ({
  value,
  label: WEEKDAY_TEXT[value],
}));

/**
 * 周期规则文案:
 * - WEEKLY   每周一 / 周四
 * - BIWEEKLY 每两周(周一 / 周四)
 * - MONTHLY  每月 1 日(N 超过当月天数时后端取月末,展示不额外说明)
 */
export function formatCycleRule(cycle: {
  cycleType: string;
  weekdays?: readonly number[] | null;
  monthDay?: number | null;
}): string {
  if (cycle.cycleType === 'MONTHLY') {
    return cycle.monthDay != null ? `每月 ${cycle.monthDay} 日` : '每月';
  }
  const names = [...(cycle.weekdays ?? [])]
    .filter((day) => WEEKDAY_TEXT[day] != null)
    .sort((a, b) => a - b)
    .map((day) => WEEKDAY_TEXT[day]);
  const joined = names.join(' / ');
  if (cycle.cycleType === 'BIWEEKLY') {
    return joined ? `每两周(${joined})` : '每两周';
  }
  // WEEKDAY_TEXT 自带「周」前缀,直接拼「每」得「每周一 / 周四」
  return joined ? `每${joined}` : '每周';
}

/** 商品摘要:怡宝 18.9L × 2、农夫山泉 19L × 3 */
export function formatItemsSummary(
  items: readonly { productName: string; quantity: number }[] | null | undefined,
): string {
  if (!items || items.length === 0) return '-';
  return items.map((item) => `${item.productName} × ${item.quantity}`).join('、');
}
