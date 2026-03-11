import React from 'react'
import { View, Text, Textarea } from '@tarojs/components'
import './OrderRemark.scss'

interface OrderRemarkProps {
  remark: string
  onRemarkChange: (remark: string) => void
}

const OrderRemark: React.FC<OrderRemarkProps> = ({ remark, onRemarkChange }) => {
  const handleInputChange = (e: any) => {
    onRemarkChange(e.detail.value)
  }

  return (
    <View className='section remark-section'>
      <View className='section-header'>
        <Text className='section-title'>订单备注</Text>
      </View>
      <View className='remark-wrapper'>
        <Textarea
          placeholder='请输入订单备注（选填）'
          value={remark}
          onInput={handleInputChange}
          maxlength={200}
          className='remark-input'
        />
        <Text className='remark-count'>{remark.length}/200</Text>
      </View>
    </View>
  )
}

export default OrderRemark