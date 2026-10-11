import React from 'react'
import { View, Textarea } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'

interface OrderRemarkProps {
  remark: string
  onRemarkChange: (remark: string) => void
}

/**
 * 订单备注 —— WeUI .weui-textarea + .weui-textarea__counter
 * (设计稿 02-order/order-confirm.html「订单备注」,≤200 字)
 *
 * 样式:StyleX(原 OrderRemark.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原后代选择器拍平到子元素)
 */
const styles = stylex.create({
  remarkSection: {
    marginTop: '8rpx',
  },
  remarkCells: {
    display: 'block',
    paddingBottom: '0rpx',
  },
  remarkInput: {
    width: '100%',
    minHeight: '192rpx',
    boxSizing: 'border-box',
  },
})
const OrderRemark: React.FC<OrderRemarkProps> = ({ remark, onRemarkChange }) => {
  const handleInputChange = (e: any) => {
    onRemarkChange(e.detail.value)
  }

  return (
    <View {...stylex.props(styles.remarkSection)}>
      <View className='weui-cells__title'>订单备注</View>
      <View {...props('weui-cells', styles.remarkCells)}>
        <Textarea
          placeholder='选填，如放置点、送水时段偏好等（≤200 字）'
          placeholderClass='ph'
          value={remark}
          onInput={handleInputChange}
          maxlength={200}
          {...props('weui-textarea', styles.remarkInput)}
        />
        <View className='weui-textarea__counter'>{remark.length}/200</View>
      </View>
    </View>
  )
}

export default OrderRemark
