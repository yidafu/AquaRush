import React from 'react'
import { View, Text } from '@tarojs/components'
import './index.scss'

interface OrderInfoRowProps {
  label: string
  value: React.ReactNode
  valueClass?: string
}

const OrderInfoRow: React.FC<OrderInfoRowProps> = ({ label, value, valueClass }) => {
  return (
    <View className='order-info-row'>
      <Text className='order-info-label'>{label}</Text>
      <Text className={`order-info-value ${valueClass || ''}`}>{value}</Text>
    </View>
  )
}

export default OrderInfoRow