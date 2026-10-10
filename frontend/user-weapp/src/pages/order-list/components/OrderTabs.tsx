import React from 'react'
import { View, Text } from '@tarojs/components'
import { ORDER_TABS } from '@aquarush/common'

import './OrderTabs.scss'

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
    <View className='weui-tabs order-tabs hair-b'>
      {ORDER_TABS.map((tab, index) => (
        <View
          key={tab.title}
          className={`weui-tabs__item${index === current ? ' on' : ''}`}
          onClick={() => onChange(index)}
        >
          <Text>{tab.title}</Text>
        </View>
      ))}
    </View>
  )
}

export default OrderTabs
