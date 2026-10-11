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

/**
 * 「我的订单」(my.html:.weui-panel + .weui-grids 五列):
 * 全部/待付款/待配送/配送中/已完成,.weui-badge 角标(>99 显示 99+)。
 */
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
      type: 'pendingPay'
    },
    {
      icon: '/assets/icons/order/shopping-bag',
      label: '待配送',
      count: orderStats.pendingDelivery,
      type: 'pendingDispatch'
    },
    {
      icon: '/assets/icons/order/truck',
      label: '配送中',
      count: orderStats.delivering,
      type: 'delivering'
    },
    {
      icon: '/assets/icons/order/check-circle',
      label: '已完成',
      count: orderStats.completed,
      type: 'completed'
    }
  ]

  return (
    <>
      <View className='weui-cells__title'>我的订单</View>
      <View className='weui-panel'>
        <View className='weui-panel__hd'>
          <View />
          <Text
            className='order-panel-link'
            onClick={() => onOrderNavigation('all')}
          >
            查看全部
          </Text>
        </View>
        <View className='weui-grids order-grids'>
          {orderItems.map((item) => (
            <View
              key={item.type}
              className='weui-grid'
              onClick={() => onOrderNavigation(item.type)}
            >
              <View className='weui-grid__icon order-grid-icon'>
                <CustomIcon value={item.icon} size={26} />
                {item.count > 0 && (
                  <View className='weui-badge order-grid-badge'>
                    <Text>{item.count > 99 ? '99+' : item.count}</Text>
                  </View>
                )}
              </View>
              <Text className='weui-grid__label'>{item.label}</Text>
            </View>
          ))}
        </View>
      </View>
    </>
  )
}

export default OrderSection
