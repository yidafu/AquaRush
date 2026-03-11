import React from 'react'
import { View, Text, Radio, RadioGroup } from '@tarojs/components'
import './DeliveryTimeSelector.scss'

interface DeliveryTime {
  id: string
  name: string
}

interface DeliveryTimeSelectorProps {
  selectedTime: string
  onTimeChange: (time: string) => void
}

const DeliveryTimeSelector: React.FC<DeliveryTimeSelectorProps> = ({
  selectedTime,
  onTimeChange
}) => {
  const deliveryTimes: DeliveryTime[] = [
    { id: 'immediate', name: '立即配送（30分钟内）' },
    { id: 'morning', name: '上午配送（9:00-12:00）' },
    { id: 'afternoon', name: '下午配送（14:00-18:00）' },
    { id: 'evening', name: '晚上配送（18:00-21:00）' }
  ]

  const handleTimeChange = (e: any) => {
    onTimeChange(e.detail.value)
  }

  return (
    <View className='section delivery-section'>
      <View className='section-header'>
        <Text className='section-title'>配送时间</Text>
      </View>
      <View className='delivery-time-wrapper'>
        <RadioGroup onChange={handleTimeChange}>
          {deliveryTimes.map(time => (
            <View key={time.id} className='delivery-time'>
              <Radio
                value={time.id}
                checked={selectedTime === time.id}
                color='var(--theme-primary)'
              />
              <Text className='time-name'>{time.name}</Text>
            </View>
          ))}
        </RadioGroup>
      </View>
    </View>
  )
}

export default DeliveryTimeSelector