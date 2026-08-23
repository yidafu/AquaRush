import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import type { CostRow } from '@aquarush/common'
import OrderItemRow from '../../../components/OrderItemRow'
import CostRowList from '../../../components/CostRowList'
import type { OrderItem } from './constants'

import "taro-ui/dist/style/components/card.scss"
import './OrderItemsCard.scss'

interface OrderItemsCardProps {
  items: OrderItem[]
  costRows: CostRow[]
}

/**
 * 商品信息卡(F-ORD-004 多商品):商品行(命中营销价时划线目录价,AC-ORD-003-09 —— R-MKT-01/02)
 * + 费用明细行(F-ORD-003:商品金额/水票抵扣/满减/首单立减/优惠券/赠品/配送费/实付,
 *   营销优惠明细来自订单快照 —— R-MKT-02/07/08/09/10,由父组件 buildCostRows 组装)
 * 商品行与费用明细行渲染分别复用共享 OrderItemRow / CostRowList。
 */
const OrderItemsCard: React.FC<OrderItemsCardProps> = ({ items, costRows }) => {
  const safeItems = Array.isArray(items) ? items : []

  return (
    <AtCard
      title='商品信息'
      className='items-card shadow-default'
    >
      {safeItems.length === 0 ? (
        <View className='order-items'><Text>暂无商品信息</Text></View>
      ) : (
        <View className='order-items'>
          {safeItems.map((item, index) => (
            <OrderItemRow
              key={`${item.id}-${index}`}
              image={item.image}
              name={item.name}
              spec={item.specifications && item.specifications.length > 0 ? item.specifications.join(', ') : undefined}
              priceCents={item.priceCents || 0}
              originalPriceCents={item.hasMarketingPrice && item.originalPriceCents != null ? item.originalPriceCents : null}
              quantity={item.quantity || 0}
            />
          ))}
        </View>
      )}

      <View className='order-summary'>
        <CostRowList rows={costRows} />
      </View>
    </AtCard>
  )
}

export default OrderItemsCard
