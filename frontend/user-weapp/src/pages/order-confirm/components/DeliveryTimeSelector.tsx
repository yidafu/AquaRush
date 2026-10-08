import React from 'react'
import { View, Text } from '@tarojs/components'
import { DeliverySlotVo } from '@aquarush/common'
import './DeliveryTimeSelector.scss'

interface DeliveryTimeSelectorProps {
  /** 配送时段选项(F-SYS-002:由后台「订单与履约参数」配置驱动,已过滤启用项) */
  slots: DeliverySlotVo[]
  selectedTime: string
  onTimeChange: (time: string) => void
}

/**
 * 配送时段选择器(AC-ORD-001-05:id 与后端 DeliveryTimeSlot 枚举对应,提交时大写随单落库)
 * 选项不写死:由后台配置(启用/时间窗/立即配送时限)下发,页面经 enabledDeliverySlots 过滤后传入。
 */
const DeliveryTimeSelector: React.FC<DeliveryTimeSelectorProps> = ({
  slots,
  selectedTime,
  onTimeChange
}) => {
  return (
    <View className='section delivery-section'>
      <View className='section-label-row'>
        <Text className='section-label'>配送时间</Text>
      </View>
      <View className='delivery-grid'>
        {slots.map(slot => {
          const selected = selectedTime === slot.id
          return (
            <View
              key={slot.id}
              className={`delivery-chip${selected ? ' delivery-chip-selected' : ''}`}
              onClick={() => onTimeChange(slot.id)}
            >
              <Text className='delivery-chip-name'>{slot.name}</Text>
              <Text className='delivery-chip-hint'>{slot.hint}</Text>
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default DeliveryTimeSelector
