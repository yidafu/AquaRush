import React, { useCallback } from 'react'
import { View, Text, Button } from '@tarojs/components'
import Taro from '@tarojs/taro'
import { OrderStatus, formatCurrency } from '@aquarush/common'
import './index.scss'

// 通用的信息行组件
interface InfoRowProps {
  label: string
  value?: string | number | React.ReactNode
  className?: string
  valueClassName?: string
}

const InfoRow: React.FC<InfoRowProps> = ({
  label,
  value,
  className = '',
  valueClassName = '',
}) => {
  return (
    <View className={`info-row ${className}`}>
      <Text className='label'>{label}</Text>
      {typeof value === 'string' || typeof value === 'number' ? (
        <Text className={valueClassName}>{value}</Text>
      ) : (
        value
      )}
    </View>
  )
}

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
        <View>
          {type === 'history' && order.status && (
            <Text className={`status-tag ${getStatusClassName(order.status)} `}>
              {STATUS_MAP[order.status] || order.status}
            </Text>
          )}
          {order.isSelfCollect && (
            <Text className='self-collect-tag'>自收</Text>
          )}
        </View>

      </View>

      <View className='order-info'>
        <InfoRow label='客户：' value={order.address?.receiverName || order.user?.nickname || '未知'} />
        <InfoRow label='电话：' value={order.address?.phone || order.user?.phone || '未知'} />
        <InfoRow label='地址：' value={order.address?.detailAddress || '未知'} valueClassName='address' />
        <InfoRow label='商品：' value={`${order.product?.name || '未知'} x ${order.quantity}`} />
        <InfoRow label='下单时间：' value={formatDateTime(order.createdAt)} />
        {type === 'history' && order.deliveryConfirmedAt && (
          <InfoRow label='完成时间：' value={formatDateTime(order.deliveryConfirmedAt)} />
        )}

        <InfoRow label='金额：' value={formatCurrency(order.amount)} valueClassName='amount-text' className='amount' />
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
