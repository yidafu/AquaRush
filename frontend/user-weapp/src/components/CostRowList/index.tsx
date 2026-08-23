import React from 'react'
import { View, Text } from '@tarojs/components'
import type { CostRow } from '@aquarush/common'

import './index.scss'

/**
 * 费用明细行列表(确认订单页 CostSummary / 订单详情 OrderItemsCard 汇总块统一):
 * 行数据由 @aquarush/common utils/order-cost 的 buildCostRows 派生(CostRow[]),
 * 扣减行语义红、实付行主色加粗(§2.2 语义色;SCSS 漂移已按设计系统统一)。
 */
interface CostRowListProps {
  rows: CostRow[]
  className?: string
}

const CostRowList: React.FC<CostRowListProps> = ({ rows, className = '' }) => {
  if (rows.length === 0) return null

  return (
    <View className={`cost-row-list ${className}`}>
      {rows.map(row => (
        <View key={row.key} className={`cost-row${row.total ? ' cost-row--total' : ''}`}>
          <Text className='cost-row-label'>{row.label}</Text>
          <Text
            className={`cost-row-value${row.discount ? ' cost-row-value--discount' : ''}${row.total ? ' cost-row-value--total' : ''}`}
          >
            {row.value}
          </Text>
        </View>
      ))}
    </View>
  )
}

export default CostRowList
