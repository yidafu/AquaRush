import React from 'react'
import { View, Text } from '@tarojs/components'
import './CostSummary.scss'

interface CostSummaryProps {
  totalAmount: number
  deliveryFee: number
  finalAmount: number
}

const CostSummary: React.FC<CostSummaryProps> = ({
  totalAmount,
  deliveryFee,
  finalAmount
}) => {
  return (
    <View className='cost-section'>
      <View className='cost-item'>
        <Text className='cost-label'>商品总价</Text>
        <Text className='cost-value'>¥{totalAmount.toFixed(2)}</Text>
      </View>
      <View className='cost-item'>
        <Text className='cost-label'>配送费</Text>
        <Text className='cost-value'>
          {deliveryFee === 0 ? '免配送费' : `¥${deliveryFee.toFixed(2)}`}
        </Text>
      </View>
      <View className='cost-item total'>
        <Text className='cost-label'>实付金额</Text>
        <Text className='cost-value total-amount'>¥{finalAmount.toFixed(2)}</Text>
      </View>
    </View>
  )
}

export default CostSummary