import React from 'react'
import { View, Textarea } from '@tarojs/components'
import './OrderRemark.scss'

interface OrderRemarkProps {
  remark: string
  onRemarkChange: (remark: string) => void
}

/**
 * 订单备注 —— WeUI .weui-textarea + .weui-textarea__counter
 * (设计稿 02-order/order-confirm.html「订单备注」,≤200 字)
 */
const OrderRemark: React.FC<OrderRemarkProps> = ({ remark, onRemarkChange }) => {
  const handleInputChange = (e: any) => {
    onRemarkChange(e.detail.value)
  }

  return (
    <View className='remark-section'>
      <View className='weui-cells__title'>订单备注</View>
      <View className='weui-cells remark-cells'>
        <Textarea
          placeholder='选填，如放置点、送水时段偏好等（≤200 字）'
          placeholderClass='ph'
          value={remark}
          onInput={handleInputChange}
          maxlength={200}
          className='weui-textarea remark-input'
        />
        <View className='weui-textarea__counter'>{remark.length}/200</View>
      </View>
    </View>
  )
}

export default OrderRemark
