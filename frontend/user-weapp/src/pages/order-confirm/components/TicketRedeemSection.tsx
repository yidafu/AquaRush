import React from 'react'
import { View, Text } from '@tarojs/components'
import './TicketRedeemSection.scss'

interface TicketRedeemSectionProps {
  /** 可用水票张数(preview.availableTicketCount) */
  availableTicketCount: number
  /** 购买数量(核销上限取 min(quantity, availableTicketCount)) */
  quantity: number
  /** 当前已选核销张数 */
  value: number
  /** 核销张数变化(触发父级重新计价) */
  onChange: (value: number) => void
}

/**
 * 水票抵扣选择(F-MKT-007,F-ORD-023)
 * 步进器 0 ~ min(quantity, availableTicketCount);变化后由父级重新请求 orderPreview 计价。
 */
const TicketRedeemSection: React.FC<TicketRedeemSectionProps> = ({
  availableTicketCount,
  quantity,
  value,
  onChange
}) => {
  const max = Math.min(quantity, availableTicketCount)

  const decrement = (): void => {
    if (value > 0) onChange(value - 1)
  }

  const increment = (): void => {
    if (value < max) onChange(value + 1)
  }

  return (
    <View className='section ticket-redeem-section'>
      <View className='section-label-row'>
        <Text className='section-label'>水票抵扣</Text>
      </View>
      <View className='ticket-redeem-content'>
        <View className='ticket-redeem-info'>
          <Text className='ticket-redeem-text'>可用 {availableTicketCount} 张，最多核销 {max} 张</Text>
          <Text className='ticket-redeem-hint'>每张水票抵扣 1 桶</Text>
        </View>
        <View className='ticket-stepper'>
          <View
            className={`stepper-btn ${value <= 0 ? 'stepper-btn-disabled' : ''}`}
            onClick={decrement}
          >
            <Text className='stepper-btn-icon'>−</Text>
          </View>
          <Text className='stepper-value'>{value}</Text>
          <View
            className={`stepper-btn ${value >= max ? 'stepper-btn-disabled' : ''}`}
            onClick={increment}
          >
            <Text className='stepper-btn-icon'>+</Text>
          </View>
        </View>
      </View>
    </View>
  )
}

export default TicketRedeemSection
