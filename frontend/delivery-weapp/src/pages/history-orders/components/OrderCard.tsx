import React, { useCallback } from 'react'
import { View, Text } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { OrderStatus, formatCentsToCurrency, formatDateTime } from '@aquarush/common'

// 订单状态映射
const STATUS_MAP: Record<string, string> = {
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUNDED: '已退款',
}

interface OrderItem {
  id: string
  orderNo: string
  quantity: number
  amount: number
  status: string
  isSelfCollect: boolean
  createdAt: string
  deliveryConfirmedAt?: string
  user?: {
    nickname: string
    phone: string
  }
  address?: {
    detailAddress: string
    receiverName: string
    phone: string
  }
  product?: {
    name: string
  }
}

interface OrderCardProps {
  order: OrderItem
}

const getStatusClassName = (status: string) => {
  switch (status) {
    case OrderStatus.COMPLETED:
      return 'completed'
    case OrderStatus.CANCELLED:
      return 'cancelled'
    case OrderStatus.REFUNDED:
      return 'refunded'
    default:
      return ''
  }
}


export const OrderCard: React.FC<OrderCardProps> = ({ order }) => {
  const handleClick = useCallback(() => {
    Taro.navigateTo({
      url: `/pages/order-detail/index?id=${order.orderNo}`
    })
  }, [order.orderNo])

  return (
    <View
      className='order-card'
      key={order.id}
      onClick={handleClick}
    >
      <View className='order-header'>
        <Text className='order-number'>{order.orderNo}</Text>
        <Text className={`status-tag ${getStatusClassName(order.status)}`}>
          {STATUS_MAP[order.status] || order.status}
        </Text>
      </View>
      <View className='order-info'>
        <View className='info-row'>
          <Text className='label'>商品:</Text>
          <Text>{order.product?.name || '-'} x{order.quantity}</Text>
        </View>
        <View className='info-row'>
          <Text className='label'>地址:</Text>
          <Text className='address'>{order.address?.detailAddress || '-'}</Text>
        </View>
        <View className='info-row'>
          <Text className='label'>客户:</Text>
          <Text>{order.user?.nickname || order.user?.phone || '-'}</Text>
        </View>
        <View className='info-row'>
          <Text className='label'>下单时间:</Text>
          <Text>{formatDateTime(order.createdAt)}</Text>
        </View>
        {order.deliveryConfirmedAt && (
          <View className='info-row'>
            <Text className='label'>完成时间:</Text>
            <Text>{formatDateTime(order.deliveryConfirmedAt)}</Text>
          </View>
        )}
        {order.isSelfCollect && (
          <View className='info-row'>
            <Text className='label'>配送方式:</Text>
            <Text>自提</Text>
          </View>
        )}
        <View className='info-row amount'>
          <Text className='label'>订单金额:</Text>
          <Text className='amount-text'>{formatCentsToCurrency(order.amount)}</Text>
        </View>
      </View>
    </View>
  )
}
