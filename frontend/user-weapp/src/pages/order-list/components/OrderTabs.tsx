import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { ORDER_TABS } from '@aquarush/common'

// 订单状态 Tab:WeUI .weui-tabs 骨架由 weui.scss 提供,这里让 5 个 tab 均分一行
const styles = stylex.create({
  tabItem: {
    flexGrow: 1,
    flexShrink: 1,
    flexBasis: 0,
    textAlign: 'center',
  },
})

interface OrderTabsProps {
  current: number
  onChange: (index: number) => void
}

/**
 * 状态 Tab 栏 —— WeUI .weui-tabs(白底 + 选中项品牌蓝短下划线,
 * 设计稿 02-order/order-list.html;Tab 定义来自 common ORDER_TABS)
 */
const OrderTabs: React.FC<OrderTabsProps> = ({ current, onChange }) => {
  return (
    <View className='weui-tabs hair-b'>
      {ORDER_TABS.map((tab, index) => (
        <View
          key={tab.title}
          {...props('weui-tabs__item', index === current && 'on', styles.tabItem)}
          onClick={() => onChange(index)}
        >
          <Text>{tab.title}</Text>
        </View>
      ))}
    </View>
  )
}

export default OrderTabs
