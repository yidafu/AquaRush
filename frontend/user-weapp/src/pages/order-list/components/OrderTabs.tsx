import React from 'react'
import { View, Text } from '@tarojs/components'
import { TAB_LIST } from './constants'

import './OrderTabs.scss'

interface OrderTabsProps {
  current: number
  onChange: (index: number) => void
}

/**
 * 状态 Tab 栏(分段胶囊:容器 tint 圆角,选中主色 tint 底 + 主色字,§5)
 */
const OrderTabs: React.FC<OrderTabsProps> = ({ current, onChange }) => {
  return (
    <View className='order-tabs'>
      {TAB_LIST.map((tab, index) => (
        <View
          key={tab.title}
          className={`order-tab-item ${index === current ? 'order-tab-item-active' : ''}`}
          onClick={() => onChange(index)}
        >
          <Text className='order-tab-text'>{tab.title}</Text>
        </View>
      ))}
    </View>
  )
}

export default OrderTabs
