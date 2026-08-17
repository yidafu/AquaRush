import React from 'react'
import { View, Text } from '@tarojs/components'
import CustomIcon from '@/components/CustomIcon'

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
      icon: '/assets/icons/order/all',
      label: '全部',
      count: 0,
      type: 'all'
    },
    {
      icon: '/assets/icons/order/credit-card',
      label: '待付款',
      count: orderStats.pendingPayment,
      type: 'pending_payment'
    },
    {
      icon: '/assets/icons/order/shopping-bag',
      label: '待配送',
      count: orderStats.pendingDelivery,
      type: 'pending_delivery'
    },
    {
      icon: '/assets/icons/order/truck',
      label: '配送中',
      count: orderStats.delivering,
      type: 'delivering'
    }
  ]

  return (
    <View className='order-card page-card'>
      <View className='section-header'>
        <Text className='section-title'>我的订单</Text>
        <Text
          className='section-extra'
          onClick={() => onOrderNavigation('all')}
        >
          查看全部 ›
        </Text>
      </View>

      <View className='order-grid'>
        {orderItems.map((item) => (
          <View
            key={item.type}
            className='order-item'
            onClick={() => onOrderNavigation(item.type)}
          >
            <View className='order-icon'>
              <CustomIcon value={item.icon} size={44} />
              {item.count > 0 && (
                <View className='order-badge'>
                  <Text className='order-badge-text'>
                    {item.count > 99 ? '99+' : item.count}
                  </Text>
                </View>
              )}
            </View>
            <Text className='order-label'>{item.label}</Text>
          </View>
        ))}
      </View>
    </View>
  )
}

export default OrderSection
