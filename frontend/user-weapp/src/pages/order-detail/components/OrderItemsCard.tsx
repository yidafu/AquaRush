import React from 'react'
import { View, Text } from '@tarojs/components'
import type { CostRow } from '@aquarush/common'
import OrderItemRow from '../../../components/OrderItemRow'
import CostRowList from '../../../components/CostRowList'
import type { OrderItem } from './constants'
import './OrderItemsCard.scss'

interface OrderItemsCardProps {
  items: OrderItem[]
  costRows: CostRow[]
}

/**
 * 商品清单 + 金额明细 —— WeUI .weui-cells 组(设计稿 02-order/order-detail.html:
 * 「商品清单 / 共 N 件」标题 + .weui-media-box 商品行 + 「金额明细」标题 + 费用行,
 * 营销优惠明细来自订单快照 —— R-MKT-02/07/08/09/10,由父组件 buildCostRows 组装)。
 * 商品行与费用明细行渲染分别复用共享 OrderItemRow / CostRowList。
 */
const OrderItemsCard: React.FC<OrderItemsCardProps> = ({ items, costRows }) => {
  const safeItems = Array.isArray(items) ? items : []
  const totalQuantity = safeItems.reduce((sum, item) => sum + (item.quantity || 0), 0)

  return (
    <View className='items-card'>
      <View className='weui-cells__title title-row'>
        <Text>商品清单</Text>
        {safeItems.length > 0 && <Text className='title-extra'>共 {totalQuantity} 件</Text>}
      </View>
      <View className='weui-cells'>
        {safeItems.length === 0 ? (
          <View className='weui-cell'><Text className='items-empty'>暂无商品信息</Text></View>
        ) : (
          safeItems.map((item, index) => (
            <OrderItemRow
              key={`${item.id}-${index}`}
              image={item.image}
              name={item.name}
              spec={item.specifications && item.specifications.length > 0 ? item.specifications.join(', ') : undefined}
              priceCents={item.priceCents || 0}
              originalPriceCents={item.hasMarketingPrice && item.originalPriceCents != null ? item.originalPriceCents : null}
              quantity={item.quantity || 0}
            />
          ))
        )}
      </View>

      {costRows.length > 0 && (
        <>
          <View className='weui-cells__title'>金额明细</View>
          <View className='weui-cells'>
            <View className='weui-cell weui-cell_vertical'>
              <CostRowList rows={costRows} />
            </View>
          </View>
        </>
      )}
    </View>
  )
}

export default OrderItemsCard
