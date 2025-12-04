import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard, AtBadge, AtDivider } from 'taro-ui'
import CustomIcon from '../CustomIcon'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/badge.scss"
import "taro-ui/dist/style/components/divider.scss"
import './index.scss'

interface OrderStats {
  pendingPayment: number
  pendingDelivery: number
  delivering: number
  completed: number
  afterSales: number
}

interface OrderSectionProps {
  orderStats: OrderStats
  onOrderNavigation: (type: string) => void
}

const OrderSection: React.FC<OrderSectionProps> = ({ orderStats, onOrderNavigation }) => {
  const orderItems = [
    {
      icon: '/assets/icons/order/all.png',
      label: '全部',
      count: 0,
      type: 'pending_payment',
      color: '#ff6b35'
    },
    {
      icon: '/assets/icons/order/credit-card.png',
      label: '待付款',
      count: orderStats.pendingPayment,
      type: 'pending_payment',
      color: '#ff6b35'
    },
    {
      icon: '/assets/icons/order/shopping-bag.png',
      label: '待配送',
      count: orderStats.pendingDelivery,
      type: 'pending_delivery',
      color: '#667eea'
    },
    {
      icon: '/assets/icons/order/truck.png',
      label: '配送中',
      count: orderStats.delivering,
      type: 'delivering',
      color: '#19be6b'
    }
  ]

  return (
    <AtCard title='我的订单' className='order-section'>
      <View className='order-grid'>
        {orderItems.map((item, index) => (
          <View
            key={index}
            className='order-item'
            onClick={() => onOrderNavigation(item.type)}
          >
            <AtBadge value={item.count} maxValue={99} className='order-badge'>
              <View className='order-icon' style={{ color: item.color }}>
                <CustomIcon value={item.icon} size={20} />
              </View>
              <Text className='order-label'>{item.label}</Text>
            </AtBadge>
          </View>
        ))}
      </View>

      <AtDivider />
    </AtCard>
  )
}

export default OrderSection