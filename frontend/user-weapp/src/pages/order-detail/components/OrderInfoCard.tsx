import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import { DeliverySlotVo } from '@aquarush/common'
import { PAYMENT_METHOD_MAP, DELIVERY_TIME_SLOT_MAP } from './constants'
import type { OrderDetail } from './constants'
import { deliverySlotLabel } from '../../../utils/deliverySlots'

import "taro-ui/dist/style/components/card.scss"
import './OrderInfoCard.scss'

interface OrderInfoCardProps {
  order: OrderDetail
  // 配送时段配置(F-SYS-002:全量含已停用,历史订单时段文案按配置渲染,兜底见 DELIVERY_TIME_SLOT_MAP)
  deliverySlots: DeliverySlotVo[]
}

/**
 * 订单信息卡:支付方式/下单时间/配送时段(配置优先,兜底枚举文案)/支付时间/完成时间/订单备注
 */
const OrderInfoCard: React.FC<OrderInfoCardProps> = ({ order, deliverySlots }) => {
  return (
    <AtCard
      title='订单信息'
      className='info-card shadow-default'
    >
      <View className='info-item'>
        <Text className='info-label'>支付方式</Text>
        <Text className='info-value'>
          {PAYMENT_METHOD_MAP[order.paymentMethod] || order.paymentMethod}
          {/* 核销水票时展示水票支付(F-MKT-007,R-MKT-08:被核销桶按水票支付) */}
          {(order.ticketRedeemCount ?? 0) > 0 && ` + 水票${order.ticketRedeemCount}张`}
        </Text>
      </View>
      <View className='info-item'>
        <Text className='info-label'>下单时间</Text>
        <Text className='info-value'>{order.createTime}</Text>
      </View>
      {order.deliveryTimeSlot && (
        <View className='info-item'>
          <Text className='info-label'>配送时段</Text>
          <Text className='info-value'>
            {deliverySlotLabel(deliverySlots, order.deliveryTimeSlot) ||
              DELIVERY_TIME_SLOT_MAP[order.deliveryTimeSlot] ||
              order.deliveryTimeSlot}
          </Text>
        </View>
      )}
      {order.paymentTime && (
        <View className='info-item'>
          <Text className='info-label'>支付时间</Text>
          <Text className='info-value'>{order.paymentTime}</Text>
        </View>
      )}
      {order.completedTime && (
        <View className='info-item'>
          <Text className='info-label'>完成时间</Text>
          <Text className='info-value'>{order.completedTime}</Text>
        </View>
      )}
      {order.remark && (
        <View className='info-item'>
          <Text className='info-label'>订单备注</Text>
          <Text className='info-value'>{order.remark}</Text>
        </View>
      )}
    </AtCard>
  )
}

export default OrderInfoCard
