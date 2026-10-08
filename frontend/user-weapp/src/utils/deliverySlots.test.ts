/**
 * 配送时段配置工具单测(F-SYS-002,确认订单页/订单详情配置化展示)
 * 覆盖:兜底默认值 / 启用过滤(含空列表回退) / 详情文案(含已停用时段) / 选中态回退(含停用后回退第一项)。
 */
import { describe, expect, it } from '@jest/globals'
import { DeliverySlotVo, DeliveryTimeSlot } from '@aquarush/common'
import {
  DEFAULT_DELIVERY_SLOTS,
  deliverySlotLabel,
  enabledDeliverySlots,
  resolveSelectedSlot
} from './deliverySlots'

const CONFIG_SLOTS: DeliverySlotVo[] = [
  { id: DeliveryTimeSlot.IMMEDIATE, name: '立即配送', hint: '45分钟内', enabled: true },
  { id: DeliveryTimeSlot.MORNING, name: '上午配送', hint: '9:00-12:00', enabled: false },
  { id: DeliveryTimeSlot.AFTERNOON, name: '下午配送', hint: '15:30-17:30', enabled: true },
  { id: DeliveryTimeSlot.EVENING, name: '晚上配送', hint: '18:00-21:00', enabled: false }
]

describe('DEFAULT_DELIVERY_SLOTS', () => {
  it('与改造前硬编码默认值一致(立即30分钟/上午9-12/下午14-18/晚上18-21,全启用)', () => {
    expect(DEFAULT_DELIVERY_SLOTS).toEqual([
      { id: DeliveryTimeSlot.IMMEDIATE, name: '立即配送', hint: '30分钟内', enabled: true },
      { id: DeliveryTimeSlot.MORNING, name: '上午配送', hint: '9:00-12:00', enabled: true },
      { id: DeliveryTimeSlot.AFTERNOON, name: '下午配送', hint: '14:00-18:00', enabled: true },
      { id: DeliveryTimeSlot.EVENING, name: '晚上配送', hint: '18:00-21:00', enabled: true }
    ])
  })
})

describe('enabledDeliverySlots', () => {
  it('仅返回启用时段', () => {
    expect(enabledDeliverySlots(CONFIG_SLOTS).map(s => s.id)).toEqual([
      DeliveryTimeSlot.IMMEDIATE,
      DeliveryTimeSlot.AFTERNOON
    ])
  })
  it('空列表回退默认(请求失败/离线时兜底,选项不缺失)', () => {
    const result = enabledDeliverySlots([])
    expect(result.length).toBe(4)
    expect(result[0].id).toBe(DeliveryTimeSlot.IMMEDIATE)
  })
})

describe('deliverySlotLabel', () => {
  it('按配置渲染时段文案(名称+时间窗)', () => {
    expect(deliverySlotLabel(CONFIG_SLOTS, DeliveryTimeSlot.IMMEDIATE)).toBe('立即配送（45分钟内）')
    expect(deliverySlotLabel(CONFIG_SLOTS, DeliveryTimeSlot.AFTERNOON)).toBe('下午配送（15:30-17:30）')
  })
  it('已停用时段仍可映射(历史订单快照展示)', () => {
    expect(deliverySlotLabel(CONFIG_SLOTS, DeliveryTimeSlot.MORNING)).toBe('上午配送（9:00-12:00）')
  })
  it('未知/空 id 返回 null(调用方回退兜底文案)', () => {
    expect(deliverySlotLabel(CONFIG_SLOTS, null)).toBeNull()
    expect(deliverySlotLabel(CONFIG_SLOTS, 'UNKNOWN' as DeliveryTimeSlot)).toBeNull()
  })
})

describe('resolveSelectedSlot', () => {
  it('当前选中仍启用则保留', () => {
    expect(resolveSelectedSlot(CONFIG_SLOTS, DeliveryTimeSlot.AFTERNOON)).toBe(DeliveryTimeSlot.AFTERNOON)
  })
  it('当前选中已停用则回退第一启用时段', () => {
    expect(resolveSelectedSlot(CONFIG_SLOTS, DeliveryTimeSlot.MORNING)).toBe(DeliveryTimeSlot.IMMEDIATE)
  })
  it('空列表按默认值回退:当前时段在默认列表中则保留', () => {
    expect(resolveSelectedSlot([], DeliveryTimeSlot.EVENING)).toBe(DeliveryTimeSlot.EVENING)
  })
  it('空列表按默认值回退:未知时段回退第一项', () => {
    expect(resolveSelectedSlot([], 'UNKNOWN' as DeliveryTimeSlot)).toBe(DeliveryTimeSlot.IMMEDIATE)
  })
})
