/**
 * 配送时段配置工具(确认订单页 F-ORD-001 配送时间选项 / 订单详情时段文案)
 *
 * 配送时段由后台「订单与履约参数」(F-SYS-002,PRD 11-system-settings.md)配置,
 * 用户端经 `deliverySlots` 查询读取,不再写死。本文件提供:
 * - 兜底默认值(仅在请求失败/离线时使用,在线时以服务端返回为准)
 * - 启用时段过滤(确认订单页选项)
 * - 时段文案组装(订单详情展示,含已停用时段的历史快照)
 * - 选中态回退(后台停用某时段后,若当前选中项失效则回退第一项)
 */
import { DeliverySlotVo, DeliveryTimeSlot } from '@aquarush/common'

/**
 * 兜底配送时段(与改造前硬编码一致的默认值;服务端未配置时也返回同值种子,
 * 前端兜底仅覆盖请求失败/离线场景,遵循「未配置按默认值兜底」惯例)
 */
export const DEFAULT_DELIVERY_SLOTS: DeliverySlotVo[] = [
  { id: DeliveryTimeSlot.IMMEDIATE, name: '立即配送', hint: '30分钟内', enabled: true },
  { id: DeliveryTimeSlot.MORNING, name: '上午配送', hint: '9:00-12:00', enabled: true },
  { id: DeliveryTimeSlot.AFTERNOON, name: '下午配送', hint: '14:00-18:00', enabled: true },
  { id: DeliveryTimeSlot.EVENING, name: '晚上配送', hint: '18:00-21:00', enabled: true }
]

/** 仅启用时段(确认订单页渲染选项用;立即配送恒启用,至少一项) */
export const enabledDeliverySlots = (slots: DeliverySlotVo[]): DeliverySlotVo[] =>
  (slots.length > 0 ? slots : DEFAULT_DELIVERY_SLOTS).filter(slot => slot.enabled)

/**
 * 时段文案(订单详情展示:「上午配送(9:00-12:00)」)。
 * 用全量时段(含已停用)映射历史快照;匹配不到时返回 null,调用方回退兜底文案。
 */
export const deliverySlotLabel = (slots: DeliverySlotVo[], id: string | null | undefined): string | null => {
  const slot = (slots.length > 0 ? slots : DEFAULT_DELIVERY_SLOTS).find(item => item.id === id)
  if (!slot) return null
  return `${slot.name}（${slot.hint}）`
}

/**
 * 选中态回退:当前选中时段仍在启用列表则保留,否则回退第一个启用时段
 * (处理后台停用某时段后已选状态失效;启用列表恒非空)
 */
export const resolveSelectedSlot = (slots: DeliverySlotVo[], current: string): string => {
  const available = enabledDeliverySlots(slots)
  if (available.some(slot => slot.id === current)) return current
  return available[0].id
}
