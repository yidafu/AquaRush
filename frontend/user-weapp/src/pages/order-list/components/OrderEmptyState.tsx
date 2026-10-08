import React from 'react'
import EmptyState from '../../../components/EmptyState'

interface OrderEmptyStateProps {
  /** 是否「全部」Tab(仅全部 Tab 展示「去逛逛」按钮) */
  isAllTab: boolean
  /** 当前 Tab 标题(用于「暂无X订单」文案) */
  tabTitle: string
  onGoShopping: () => void
}

/**
 * 订单列表空态:空图 + 文案 + 「去逛逛」按钮(仅全部 Tab;渲染统一走共享 EmptyState)
 */
const OrderEmptyState: React.FC<OrderEmptyStateProps> = ({ isAllTab, tabTitle, onGoShopping }) => {
  return (
    <EmptyState
      image='/assets/empty-order.png'
      title='暂无订单'
      description={isAllTab ? '您还没有任何订单哦' : `暂无${tabTitle}订单`}
      actionText={isAllTab ? '去逛逛' : undefined}
      onAction={isAllTab ? onGoShopping : undefined}
    />
  )
}

export default OrderEmptyState
