import React from 'react'
import { View, Text } from '@tarojs/components'
import './DeliveryTimeSelector.scss'

interface DeliveryTime {
  id: string
  name: string
  hint: string
}

interface DeliveryTimeSelectorProps {
  selectedTime: string
  onTimeChange: (time: string) => void
}

/** 配送时段(AC-ORD-001-05:id 与后端 DeliveryTimeSlot 枚举对应,提交时大写落库) */
const DELIVERY_TIMES: DeliveryTime[] = [
  { id: 'immediate', name: '立即配送', hint: '30分钟内' },
  { id: 'morning', name: '上午配送', hint: '9:00-12:00' },
  { id: 'afternoon', name: '下午配送', hint: '14:00-18:00' },
  { id: 'evening', name: '晚上配送', hint: '18:00-21:00' }
]

const DeliveryTimeSelector: React.FC<DeliveryTimeSelectorProps> = ({
  selectedTime,
  onTimeChange
}) => {
  return (
    <View className='section delivery-section'>
      <View className='section-label-row'>
        <Text className='section-label'>配送时间</Text>
      </View>
      <View className='delivery-grid'>
        {DELIVERY_TIMES.map(time => {
          const selected = selectedTime === time.id
          return (
            <View
              key={time.id}
              className={`delivery-chip${selected ? ' delivery-chip-selected' : ''}`}
              onClick={() => onTimeChange(time.id)}
            >
              <Text className='delivery-chip-name'>{time.name}</Text>
              <Text className='delivery-chip-hint'>{time.hint}</Text>
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default DeliveryTimeSelector
