import React, { useCallback } from 'react'
import { View, Text, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { OrderStatus, formatCurrency } from '@aquarush/common'
import './index.scss'

// 订单状态映射
const STATUS_MAP: Record<string, string> = {
  [OrderStatus.COMPLETED]: '已完成',
  [OrderStatus.CANCELLED]: '已取消',
  [OrderStatus.REFUNDED]: '已退款',
}

// 统一的订单数据类型
interface OrderData {
  id: string | number
  orderNo: string
  quantity: number
  amount: number
  status?: string
  isSelfCollect?: boolean
  createdAt?: string
  deliveryConfirmedAt?: string
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

type OrderCardType = 'pending' | 'assigned' | 'delivering' | 'history'

interface OrderCardProps {
  order: OrderData
  type?: OrderCardType
  onAccept?: (orderId: number) => void
  onStartDelivery?: (orderId: number) => void
  onViewDetail?: (orderId: number | string) => void
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

const formatDateTime = (dateStr?: string) => {
  if (!dateStr) return '-'
  const date = new Date(dateStr)
  const year = date.getFullYear()
  const month = String(date.getMonth() + 1).padStart(2, '0')
  const day = String(date.getDate()).padStart(2, '0')
  const hour = String(date.getHours()).padStart(2, '0')
  const minute = String(date.getMinutes()).padStart(2, '0')
  return `${year}-${month}-${day} ${hour}:${minute}`
}

const OrderCard: React.FC<OrderCardProps> = ({
  order,
  type = 'history',
  onAccept,
  onStartDelivery,
  onViewDetail,
}) => {
  const address = order.address || {}
  const user = order.user || {}

  const handleClick = useCallback(() => {
    if (onViewDetail) {
      onViewDetail(order.id)
    } else {
      Taro.navigateTo({ url: `/pages/order-detail/index?id=${order.orderNo}` })
    }
  }, [order.id, order.orderNo, onViewDetail])

  const handleAccept = (e: any) => {
    e.stopPropagation()
    onAccept?.(order.id as number)
  }

  const handleStartDelivery = (e: any) => {
    e.stopPropagation()
    onStartDelivery?.(order.id as number)
  }

  const handleViewDetail = (e: any) => {
    e.stopPropagation()
    handleClick()
  }

  return (
    <View className='order-card' onClick={handleClick}>
      <View className='order-header'>
        <Text className='order-number'>{order.orderNo}</Text>
        {type === 'history' && order.status && (
          <Text className={`status-tag ${getStatusClassName(order.status)}`}>
            {STATUS_MAP[order.status] || order.status}
          </Text>
        )}
        {type !== 'history' && order.isSelfCollect && (
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
        {type === 'history' && order.createdAt && (
          <View className='info-row'>
            <Text className='label'>下单时间：</Text>
            <Text>{formatDateTime(order.createdAt)}</Text>
          </View>
        )}
        {type === 'history' && order.deliveryConfirmedAt && (
          <View className='info-row'>
            <Text className='label'>完成时间：</Text>
            <Text>{formatDateTime(order.deliveryConfirmedAt)}</Text>
          </View>
        )}
        {type === 'history' && order.isSelfCollect && (
          <View className='info-row'>
            <Text className='label'>配送方式：</Text>
            <Text>自提</Text>
          </View>
        )}
      </View>

      {type !== 'history' && (
        <View className='order-actions'>
          {type === 'pending' && (
            <Button className='action-btn accept-btn' onClick={handleAccept}>
              接单
            </Button>
          )}
          {type === 'assigned' && (
            <Button className='action-btn start-btn' onClick={handleStartDelivery}>
              开始配送
            </Button>
          )}
          {type === 'delivering' && (
            <Button className='action-btn detail-btn' onClick={handleViewDetail}>
              去确认
            </Button>
          )}
        </View>
      )}
    </View>
  )
}

export default OrderCard
