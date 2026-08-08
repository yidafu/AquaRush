import React from 'react'
import { View, Text } from '@tarojs/components'
import { Money } from '@aquarush/common'
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
        <Text className='cost-value'><Money cents={totalAmount} /></Text>
      </View>
      <View className='cost-item'>
        <Text className='cost-label'>配送费</Text>
        <Text className='cost-value'>
          {deliveryFee === 0 ? '免配送费' : <Money cents={deliveryFee} />}
        </Text>
      </View>
      <View className='cost-item total'>
        <Text className='cost-label'>实付金额</Text>
        <Text className='cost-value total-amount'><Money cents={finalAmount} /></Text>
      </View>
    </View>
  )
}

export default CostSummary