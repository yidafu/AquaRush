import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard, AtBadge, AtDivider } from 'taro-ui'
import CustomIcon from '@/components/CustomIcon'
import Taro from '@tarojs/taro'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/badge.scss"
import "taro-ui/dist/style/components/divider.scss"

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
    <AtCard title='我的订单' className='h-auto mb-4'>
      <View className='at-row'>
        {orderItems.map((item, index) => (
          <View
            key={index}
            className='my-4 at-col at-col-3'
            onClick={() => onOrderNavigation(item.type)}
          >
            <View className='flex flex-col items-center justify-center '>
              <AtBadge value={item.count < 1 ? undefined : item.count} maxValue={99} className='order-badge'>
                <View className='flex items-center justify-center w-10 h-10 rounded-full bg-primary-light bg-opacity-10' style={{ color: item.color }}>
                  <CustomIcon value={item.icon} size={30} />
                </View>
              </AtBadge>
              <Text className='text-base font-medium text-center theme-text-primary'>{item.label}</Text>
            </View>

          </View>
        ))}
      </View>

    </AtCard>
  )
}

export default OrderSection
