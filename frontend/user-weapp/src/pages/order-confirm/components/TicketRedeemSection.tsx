import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'

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
 * 水票抵扣选择(F-MKT-007,F-ORD-023)—— WeUI .weui-cells 单元格 + .weui-stepper
 * 步进器 0 ~ min(quantity, availableTicketCount);变化后由父级重新请求 orderPreview 计价。
 *
 * 样式:StyleX(原 TicketRedeemSection.scss 迁移;750 基准 rpx,weui-* 类保留;
 * 原后代选择器拍平到子元素)
 */
const styles = stylex.create({
  ticketRedeemSection: {
    marginTop: '8rpx',
  },
  ticketRedeemText: {
    display: 'block',
    fontSize: 'var(--weui-fs-cell)',
    color: 'var(--weui-FG-0)',
  },
  ticketRedeemHint: {
    display: 'block',
    marginTop: '8rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
  },
})
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
    <View {...stylex.props(styles.ticketRedeemSection)}>
      <View className='weui-cells__title'>水票抵扣</View>
      <View className='weui-cells'>
        <View className='weui-cell'>
          <View className='weui-cell__bd'>
            <Text {...stylex.props(styles.ticketRedeemText)}>可用 {availableTicketCount} 张，最多核销 {max} 张</Text>
            <Text {...stylex.props(styles.ticketRedeemHint)}>每张水票抵扣 1 桶</Text>
          </View>
          <View className='weui-cell__ft'>
            <View className='weui-stepper'>
              <View
                className={`weui-stepper__btn${value <= 0 ? ' disabled' : ''}`}
                onClick={decrement}
              >
                <Text>−</Text>
              </View>
              <Text className='weui-stepper__num'>{value}</Text>
              <View
                className={`weui-stepper__btn${value >= max ? ' disabled' : ''}`}
                onClick={increment}
              >
                <Text>+</Text>
              </View>
            </View>
          </View>
        </View>
      </View>
    </View>
  )
}

export default TicketRedeemSection
