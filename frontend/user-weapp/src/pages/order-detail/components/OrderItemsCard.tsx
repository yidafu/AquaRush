import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import { Money } from '@aquarush/common'
import type { OrderItem } from './constants'
import type { CostRow } from '../../../utils/orderCost'

import "taro-ui/dist/style/components/card.scss"
import './OrderItemsCard.scss'

interface OrderItemsCardProps {
  items: OrderItem[]
  costRows: CostRow[]
}

/**
 * 商品信息卡(F-ORD-004 多商品):商品行(命中营销价时划线目录价,AC-ORD-003-09 —— R-MKT-01/02)
 * + 费用明细行(F-ORD-003:商品金额/水票抵扣/满减/首单立减/优惠券/赠品/配送费/实付,
 *   营销优惠明细来自订单快照 —— R-MKT-02/07/08/09/10,由父组件 buildOrderDetailCostRows 组装)
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
            <View key={`${item.id}-${index}`} className='order-item'>
              <Image
                src={item.image || '/assets/default-product.png'}
                mode='aspectFill'
                className='item-image'
              />
              <View className='item-info'>
                <Text className='item-name'>{item.name || '商品名称'}</Text>
                {item.specifications && Array.isArray(item.specifications) && item.specifications.length > 0 && (
                  <Text className='item-spec'>
                    规格：{item.specifications.join(', ')}
                  </Text>
                )}
                <View className='item-bottom'>
                  <View className='item-price-row'>
                    <Text className='item-price'><Money cents={item.priceCents || 0} /></Text>
                    {/* 命中营销价时划线目录价(AC-ORD-003-09 —— R-MKT-01/02) */}
                    {item.hasMarketingPrice && item.originalPriceCents != null && (
                      <Text className='item-original-price'><Money cents={item.originalPriceCents} /></Text>
                    )}
                  </View>
                  <Text className='item-quantity'>x{item.quantity || 0}</Text>
                </View>
              </View>
            </View>
          ))}
        </View>
      )}

      <View className='order-summary'>
        {costRows.map(row => (
          <View key={row.key} className={`summary-item${row.total ? ' total' : ''}`}>
            <Text className='summary-label'>{row.label}</Text>
            <Text
              className={`summary-value${row.discount ? ' summary-discount' : ''}${row.total ? ' total-amount' : ''}`}
            >
              {row.value}
            </Text>
          </View>
        ))}
      </View>
    </AtCard>
  )
}

export default OrderItemsCard
