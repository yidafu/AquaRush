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
 * 排序 Tab 栏(综合/销量/价格 ↑↓) + 商品总数
 */
const SortBar: React.FC<SortBarProps> = ({ activeSort, priceAsc, total, onSortClick }) => {
  return (
    <View className='sort-bar'>
      {SORT_TABS.map((tab) => (
        <View
          key={tab.key}
          className={`sort-tab ${activeSort === tab.key ? 'sort-tab-active' : ''}`}
          onClick={() => onSortClick(tab.key)}
        >
          <Text className='sort-tab-text'>{tab.label}</Text>
          {tab.key === 'price' && activeSort === 'price' && (
            <Text className='sort-tab-arrow'>{priceAsc ? '↑' : '↓'}</Text>
          )}
        </View>
      ))}
      {total > 0 && <Text className='sort-total'>共 {total} 件</Text>}
    </View>
  )
}

export default SortBar
