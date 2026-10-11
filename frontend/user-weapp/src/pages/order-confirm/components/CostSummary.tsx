import React from 'react'
import { View, Text } from '@tarojs/components'
import { OrderPreviewVo, buildCostRows, minimumOrderHint, orderCostFromPreview } from '@aquarush/common'
import CostRowList from '../../../components/CostRowList'
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
 * 金额明细(F-ORD-023 服务端计价展示)—— WeUI .weui-cells 组
 * (设计稿 02-order/order-confirm.html「金额明细」)。
 * 行数据全部由 buildCostRows(preview) 派生:商品金额 / 水票抵扣(-¥x,桶数入标签)/
 * 满减或首单立减 / 优惠券 / 赠品(¥0.00) / 配送费 / 实付;计价失败/未就绪态内联展示。
 */
const CostSummary: React.FC<CostSummaryProps> = ({
  preview,
  error,
  loading = false,
  onRetry
}) => {
  const rows = preview ? buildCostRows(orderCostFromPreview(preview)) : []
  // 起送门槛未达提示(R-OPS-03/G96):与服务端 createOrder 拦截同口径,前端先行提示并禁提交
  const minimumHint = preview ? minimumOrderHint(preview) : null

  return (
    <View className='cost-section'>
      <View className='weui-cells__title'>金额明细</View>

      {/* 计价刷新中(细条提示,不占独立区块) */}
      {preview && loading && !error && (
        <View className='weui-cells__tips cost-hint'>
          <Text>价格更新中…</Text>
        </View>
      )}

      {/* 计价失败:内联重试行 */}
      {error && (
        <View className='weui-cells'>
          <View className='weui-cell' onClick={onRetry}>
            <View className='weui-cell__bd'>计价失败</View>
            <View className='weui-cell__ft cost-error-text'>{error}，点击重试</View>
          </View>
        </View>
      )}

      {rows.length > 0 ? (
        <View className='weui-cells'>
          <View className='weui-cell weui-cell_vertical'>
            <CostRowList rows={rows} />
          </View>
        </View>
      ) : null}

      {minimumHint && (
        <View className='weui-cells'>
          <View className='weui-cell'>
            <View className='weui-cell__bd cost-minimum-hint'>{minimumHint}</View>
          </View>
        </View>
      )}

      {rows.length === 0 ? (
        !error && (
          <View className='weui-cells'>
            <View className='weui-cell'>
              <View className='weui-cell__bd'>费用明细</View>
              <View className='weui-cell__ft'>价格计算中...</View>
            </View>
          </View>
        )
      )}
    </View>
  )
}

export default CostSummary
