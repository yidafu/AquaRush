import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtButton } from 'taro-ui'

import './OrderEmptyState.scss'

interface OrderEmptyStateProps {
  /** 是否「全部」Tab(仅全部 Tab 展示「去逛逛」按钮) */
  isAllTab: boolean
  /** 当前 Tab 标题(用于「暂无X订单」文案) */
  tabTitle: string
  onGoShopping: () => void
}

/**
 * 订单列表空态:空图 + 文案 + 「去逛逛」按钮(仅全部 Tab)
 */
const OrderEmptyState: React.FC<OrderEmptyStateProps> = ({ isAllTab, tabTitle, onGoShopping }) => {
  return (
    <View className='empty-container'>
      <Image src='/assets/empty-order.png' mode='aspectFit' className='empty-image' />
      <Text className='empty-text'>暂无订单</Text>
      <Text className='empty-desc'>
        {isAllTab ? '您还没有任何订单哦' : `暂无${tabTitle}订单`}
      </Text>
      {isAllTab && (
        <AtButton
          type='primary'
          size='normal'
          onClick={onGoShopping}
          className='go-shopping-button'
        >
          去逛逛
        </AtButton>
      )}
    </View>
  )
}

export default OrderEmptyState
