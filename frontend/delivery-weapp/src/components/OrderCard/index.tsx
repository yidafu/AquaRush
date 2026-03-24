import React from 'react'
import { View, Text, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { formatCurrency } from '@aquarush/common'
import './index.scss'

interface OrderCardProps {
  order: {
    id: number
    orderNo: string
    quantity: number
    amount: number
    isSelfCollect?: boolean
    address?: {
      receiverName?: string
      phone?: string
      detailAddress?: string
    }
    user?: {
      nickname?: string
      phone?: string
    }
    product?: {
      name?: string
    }
  }
  type: 'pending' | 'assigned' | 'delivering'
  onAccept?: (orderId: number) => void
  onStartDelivery?: (orderId: number) => void
  onViewDetail?: (orderId: number) => void
}

const OrderCard: React.FC<OrderCardProps> = ({
  order,
  type,
  onAccept,
  onStartDelivery,
  onViewDetail,
}) => {
  const address = order.address || {}
  const user = order.user || {}

  const handleAccept = () => {
    onAccept?.(order.id)
  }

  const handleStartDelivery = () => {
    onStartDelivery?.(order.id)
  }

  const handleViewDetail = () => {
    if (onViewDetail) {
      onViewDetail(order.id)
    } else {
      Taro.navigateTo({ url: `/pages/order-detail/index?id=${order.orderNo}` })
    }
  }

  return (
    <View className='order-card' onClick={handleViewDetail}>
      <View className='order-header'>
        <Text className='order-number'>{order.orderNo}</Text>
        {order.isSelfCollect && (
          <Text className='self-collect-tag'>自收</Text>
        )}
      </View>

      <View className='order-info'>
        <View className='info-row'>
          <Text className='label'>客户：</Text>
          <Text>{address.receiverName || user.nickname || '未知'}</Text>
        </View>
        <View className='info-row'>
          <Text className='label'>电话：</Text>
          <Text>{address.phone || user.phone || '未知'}</Text>
        </View>
        <View className='info-row'>
          <Text className='label'>地址：</Text>
          <Text className='address'>{address.detailAddress || '未知'}</Text>
        </View>
        <View className='info-row'>
          <Text className='label'>商品：</Text>
          <Text>{order.product?.name || '未知'} x {order.quantity}</Text>
        </View>
        <View className='info-row amount'>
          <Text className='label'>金额：</Text>
          <Text className='amount-text'>{formatCurrency(order.amount)}</Text>
        </View>
      </View>

      <View className='order-actions'>
        {type === 'pending' && (
          <Button
            className='action-btn accept-btn'
            onClick={(e) => {
              e.stopPropagation()
              handleAccept()
            }}
          >
            接单
          </Button>
        )}
        {type === 'assigned' && (
          <Button
            className='action-btn start-btn'
            onClick={(e) => {
              e.stopPropagation()
              handleStartDelivery()
            }}
          >
            开始配送
          </Button>
        )}
        {type === 'delivering' && (
          <Button
            className='action-btn detail-btn'
            onClick={(e) => {
              e.stopPropagation()
              handleViewDetail()
            }}
          >
            去确认
          </Button>
        )}
      </View>
    </View>
  )
}

export default OrderCard
