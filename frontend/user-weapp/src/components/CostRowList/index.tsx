import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import type { CostRow } from '@aquarush/common'

/**
 * 样式:StyleX(原 index.scss 迁移,长度值保持 750 基准 rpx;weui-* 平台变量保留字符串引用)。
 * 原 `.cost-row--total` 后代选择器拍平为 label/value 的条件 style;
 * `cost-row-value--discount` 与基础样式同色(var(--weui-price)),按死类删除。
 */
const styles = stylex.create({
  costRowList: {
    display: 'flex',
    flexDirection: 'column',
    rowGap: '24rpx',
  },
  costRow: {
    display: 'flex',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    columnGap: '24rpx',
  },
  costRowLabel: {
    fontSize: 'var(--weui-fs-cell)',
    color: 'var(--weui-FG-0)',
    flexShrink: 0,
  },
  costRowLabelTotal: {
    fontWeight: 600,
  },
  costRowValue: {
    fontSize: 'var(--weui-fs-cell)',
    color: 'var(--weui-price)',
    textAlign: 'right',
    fontVariantNumeric: 'tabular-nums',
  },
  costRowValueTotal: {
    fontSize: '44rpx',
    fontWeight: 600,
  },
})

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
    <View {...props(styles.costRowList, className)}>
      {rows.map(row => (
        <View key={row.key} {...stylex.props(styles.costRow)}>
          <Text {...stylex.props(styles.costRowLabel, row.total && styles.costRowLabelTotal)}>
            {row.label}
          </Text>
          <Text {...stylex.props(styles.costRowValue, row.total && styles.costRowValueTotal)}>
            {row.value}
          </Text>
        </View>
      ))}
    </View>
  )
}

export default CostRowList
