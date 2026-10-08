import React from 'react'
import { View, Text } from '@tarojs/components'

import './DeliveryTimeCard.scss'

interface DeliveryTimeCardProps {
  businessHours: string
}

/**
 * ④ 配送时效卡(浅蓝左侧色条)
 */
const DeliveryTimeCard: React.FC<DeliveryTimeCardProps> = ({ businessHours }) => {
  return (
    <View className='delivery-time-card'>
      <View className='delivery-time-bar' />
      <View className='delivery-time-content'>
        <Text className='delivery-time-main'>预计 28 分钟送达</Text>
        <Text className='delivery-time-sub'>营业中 · 每日 {businessHours}</Text>
      </View>
    </View>
  )
}

export default DeliveryTimeCard
