import React from 'react'
import { View, Text } from '@tarojs/components'
import { SORT_TABS } from './constants'

import './SortBar.scss'

interface SortBarProps {
  activeSort: string
  /** 价格排序方向(仅价格 Tab 展示升降序箭头) */
  priceAsc: boolean
  total: number
  onSortClick: (key: string) => void
}

/**
 * 排序分段控件(product-list.html .weui-segmented:综合/销量/价格 ↑↓,白底条) + 商品总数
 */
const SortBar: React.FC<SortBarProps> = ({ activeSort, priceAsc, total, onSortClick }) => {
  return (
    <View className='sort-bar'>
      <View className='weui-segmented'>
        {SORT_TABS.map((tab) => (
          <View
            key={tab.key}
            className={`weui-segmented__item ${activeSort === tab.key ? 'on' : ''}`}
            trackTarget='product-list-sort'
            trackElementType='tab'
            trackElementText={tab.label}
            onClick={() => onSortClick(tab.key)}
          >
            <Text>
              {tab.label}
              {tab.key === 'price' && activeSort === 'price' ? (priceAsc ? ' ↑' : ' ↓') : ''}
            </Text>
          </View>
        ))}
      </View>
      {total > 0 && <Text className='sort-total'>共 {total} 件</Text>}
    </View>
  )
}

export default SortBar
