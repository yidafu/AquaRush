import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtTag, AtIcon, AtProgress } from 'taro-ui'
import {
  ORDER_STATUS_TONE,
  OrderStatus,
  getOrderStatusText,
  getStatusTone,
  type StatusTone
} from '@aquarush/common'

import "taro-ui/dist/style/components/tag.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/progress.scss"
import './OrderStatusHeader.scss'

interface OrderStatusHeaderProps {
  status?: OrderStatus
  orderNo: string
  onCopyOrderNo: () => void
}

/** tone → 页面 CSS 变量色对(值由 src/styles/tokens.test.ts 锁定 = common STATUS_TONE_COLORS) */
const TONE_VAR: Record<StatusTone, { color: string; bg: string }> = {
  warning: { color: 'var(--status-pending-pay-text)', bg: 'var(--status-pending-pay-bg)' },
  info: { color: 'var(--status-dispatch-text)', bg: 'var(--status-dispatch-bg)' },
  success: { color: 'var(--status-delivering-text)', bg: 'var(--status-delivering-bg)' },
  neutral: { color: 'var(--status-completed-text)', bg: 'var(--status-completed-bg)' },
  error: { color: 'var(--status-cancelled-text)', bg: 'var(--status-cancelled-bg)' },
  accent: { color: 'var(--action)', bg: 'rgba(255, 84, 54, 0.10)' }
}

/** 进度语义(下单1/付款2/配送3/完成4;待派单与待配送同为 2,取消为 0) */
const STEP_FOR_STATUS: Record<OrderStatus, number> = {
  PENDING_PAYMENT: 1,
  PENDING_DISPATCH: 2,
  PENDING_DELIVERY: 2,
  DELIVERING: 3,
  COMPLETED: 4,
  CANCELLED: 0,
  REFUNDED: 0
}

/**
 * 订单状态头部(浅 tint 背景,按状态色,§6):
 * 状态标签 + 订单号(可复制) + 配送进度条(已取消/待付款不展示进度)
 * 文案与 tone 来自 common order-status/status-tone(标签颜色单一来源)。
 */
const OrderStatusHeader: React.FC<OrderStatusHeaderProps> = ({ status, orderNo, onCopyOrderNo }) => {
  if (!status) return null

  const text = getOrderStatusText(status) ?? status
  const tone = getStatusTone(ORDER_STATUS_TONE, status) ?? 'neutral'
  const step = STEP_FOR_STATUS[status] ?? 0
  const { color, bg } = TONE_VAR[tone]

  return (
    <View className='status-header' style={{ background: bg }}>
      <View className='status-info'>
        <AtTag
          type='primary'
          size='large'
          circle
          customStyle={{
            background: color,
            color: 'white',
            border: 'none'
          }}
        >
          {text}
        </AtTag>
        <Text className='order-no'>
          订单号：{orderNo}
          <AtIcon
            value='copy'
            size='14'
            color='var(--brand-primary)'
            onClick={onCopyOrderNo}
          />
        </Text>
      </View>

      {/* 配送进度 */}
      {status !== OrderStatus.CANCELLED && status !== OrderStatus.REFUNDED && status !== OrderStatus.PENDING_PAYMENT && (
        <View className='delivery-progress'>
          <AtProgress
            percent={step * 25}
            strokeWidth={4}
            color={color}
            isShowPercent={false}
          />
          <View className='progress-steps'>
            <View className={`step-item ${step >= 1 ? 'active' : ''}`}>
              <View className='step-dot' />
              <Text className='step-text'>下单</Text>
            </View>
            <View className={`step-item ${step >= 2 ? 'active' : ''}`}>
              <View className='step-dot' />
              <Text className='step-text'>付款</Text>
            </View>
            <View className={`step-item ${step >= 3 ? 'active' : ''}`}>
              <View className='step-dot' />
              <Text className='step-text'>配送</Text>
            </View>
            <View className={`step-item ${step >= 4 ? 'active' : ''}`}>
              <View className='step-dot' />
              <Text className='step-text'>完成</Text>
            </View>
          </View>
        </View>
      )}
    </View>
  )
}

export default OrderStatusHeader
