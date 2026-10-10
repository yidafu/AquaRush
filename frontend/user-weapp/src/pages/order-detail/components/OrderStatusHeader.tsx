import React from 'react'
import { View, Text } from '@tarojs/components'
import { OrderStatus } from '@aquarush/common'
import StatusTag from '../../../components/StatusTag'
import './OrderStatusHeader.scss'

interface OrderStatusHeaderProps {
  status?: OrderStatus
  orderNo: string
  onCopyOrderNo: () => void
}

/** 进度步骤条节点(设计稿 02-order/order-detail.html:下单 → 支付 → 派单 → 配送 → 完成) */
const PROGRESS_STEPS = ['下单', '支付', '派单', '配送', '完成']

/** 状态 → 已完成步骤数(送达即完成,无中间态 —— PRD R-ORD-11) */
const DONE_STEPS_FOR_STATUS: Record<OrderStatus, number> = {
  PENDING_PAYMENT: 1,
  PENDING_DISPATCH: 2,
  PENDING_DELIVERY: 2,
  DELIVERING: 4,
  COMPLETED: 5,
  CANCELLED: 0,
  REFUNDED: 0
}

/**
 * 订单状态头 —— WeUI .weui-cells 组 + .weui-steps 进度条(设计稿 02-order/order-detail.html:
 * 大号状态标签 .weui-tag--md + 订单号 + 进度步骤条;.done 段连线转品牌蓝)。
 * 订单号与复制入口保留在状态头;文案与 tone 来自 common order-status/status-tone。
 */
const OrderStatusHeader: React.FC<OrderStatusHeaderProps> = ({ status, orderNo, onCopyOrderNo }) => {
  if (!status) return null

  const doneCount = DONE_STEPS_FOR_STATUS[status] ?? 0
  const showSteps = status !== OrderStatus.CANCELLED && status !== OrderStatus.REFUNDED && status !== OrderStatus.PENDING_PAYMENT

  return (
    <View className='weui-cells status-header'>
      <View className='weui-cell weui-cell_vertical'>
        <View className='status-tag-row'>
          <StatusTag status={status} />
        </View>

        <View className='order-no-row'>
          <Text className='order-no'>{orderNo}</Text>
          <Text className='copy-btn weui-btn weui-btn_plain-primary weui-btn_xs' onClick={onCopyOrderNo}>
            复制
          </Text>
        </View>

        {showSteps && (
          <View className='weui-steps status-steps'>
            {PROGRESS_STEPS.map((label, index) => (
              <View key={label} className={`weui-step${index < doneCount ? ' done' : ''}`}>
                <View className='weui-step__dot' />
                <Text className='weui-step__label'>{label}</Text>
              </View>
            ))}
          </View>
        )}
      </View>
    </View>
  )
}

export default OrderStatusHeader
