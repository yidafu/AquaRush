import React from 'react'
import { View, Text } from '@tarojs/components'
import { OrderPreviewVo } from '@aquarush/common'
import { buildCostRows } from '../../../utils/orderCost'
import './CostSummary.scss'

interface CostSummaryProps {
  /** 服务端计价预览(R-MON-01);null=计价未就绪 */
  preview: OrderPreviewVo | null
  /** 计价错误信息(error 时展示「点击重试」行,底部提交禁用) */
  error?: string | null
  /** 计价刷新中(费用卡顶部展示「价格更新中…」细条) */
  loading?: boolean
  /** 计价失败重试回调 */
  onRetry?: () => void
}

/**
 * 费用明细(F-ORD-023 服务端计价展示)
 * 行数据全部由 buildCostRows(preview) 派生:商品金额 / 水票抵扣(-¥x,桶数入标签)/
 * 满减或首单立减 / 优惠券 / 赠品(¥0.00) / 配送费 / 实付;计价失败/未就绪态内联展示。
 */
const CostSummary: React.FC<CostSummaryProps> = ({
  preview,
  error,
  loading = false,
  onRetry
}) => {
  const rows = preview ? buildCostRows(preview) : []

  return (
    <View className='cost-section'>
      {/* 计价刷新中(细条提示,不占独立区块) */}
      {preview && loading && !error && (
        <View className='cost-hint'>
          <Text className='cost-hint-text'>价格更新中…</Text>
        </View>
      )}

      {/* 计价失败:内联重试行 */}
      {error && (
        <View className='cost-item cost-error-row' onClick={onRetry}>
          <Text className='cost-label'>计价失败</Text>
          <Text className='cost-value cost-error-text'>{error}，点击重试</Text>
        </View>
      )}

      {rows.length > 0 ? (
        rows.map(row => (
          <View key={row.key} className={`cost-item${row.total ? ' total' : ''}`}>
            <Text className='cost-label'>{row.label}</Text>
            <Text
              className={`cost-value${row.discount ? ' cost-discount' : ''}${row.total ? ' total-amount' : ''}`}
            >
              {row.value}
            </Text>
          </View>
        ))
      ) : (
        !error && (
          <View className='cost-item'>
            <Text className='cost-label'>费用明细</Text>
            <Text className='cost-value'>价格计算中...</Text>
          </View>
        )
      )}
    </View>
  )
}

export default CostSummary
