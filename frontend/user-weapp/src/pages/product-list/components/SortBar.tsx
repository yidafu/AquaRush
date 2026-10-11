import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { SORT_TABS } from './constants'

interface SortBarProps {
  activeSort: string
  /** 价格排序方向(仅价格 Tab 展示升降序箭头) */
  priceAsc: boolean
  total: number
  onSortClick: (key: string) => void
}

/**
 * 排序分段控件(product-list.html .weui-segmented:综合/销量/价格 ↑↓,白底条) + 商品总数
 *
 * 样式:StyleX(原 SortBar.scss 迁移,750 基准 rpx;weui-* 类保留)
 */
const styles = stylex.create({
  sortBar: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    columnGap: '16rpx',
    marginTop: '16rpx',
    paddingTop: '20rpx',
    paddingRight: '32rpx',
    paddingBottom: '20rpx',
    paddingLeft: '32rpx',
    backgroundColor: 'var(--weui-BG-2)',
  },
  sortTotal: {
    flexGrow: 0,
    flexShrink: 0,
    fontSize: '24rpx',
    color: 'var(--weui-FG-2)',
  },
})

const SortBar: React.FC<SortBarProps> = ({ activeSort, priceAsc, total, onSortClick }) => {
  return (
    <View {...stylex.props(styles.sortBar)}>
      <View className='weui-segmented'>
        {SORT_TABS.map((tab) => (
          <View
            key={tab.key}
            {...props('weui-segmented__item', activeSort === tab.key && 'on')}
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
      {total > 0 && (
        <Text {...stylex.props(styles.sortTotal)}>共 {total} 件</Text>
      )}
    </View>
  )
}

export default SortBar
