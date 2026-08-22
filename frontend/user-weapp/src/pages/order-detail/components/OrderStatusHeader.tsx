import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtTag, AtIcon, AtProgress } from 'taro-ui'
import { STATUS_MAP } from './constants'
import type { OrderStatusKey } from './constants'

import "taro-ui/dist/style/components/tag.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/progress.scss"
import './OrderStatusHeader.scss'

interface OrderStatusHeaderProps {
  status: OrderStatusKey
  orderNo: string
  onCopyOrderNo: () => void
}

/**
 * 订单状态头部(浅 tint 背景,按状态色,§6):
 * 状态标签 + 订单号(可复制) + 配送进度条(已取消/待付款不展示进度)
 */
const OrderStatusHeader: React.FC<OrderStatusHeaderProps> = ({ status, orderNo, onCopyOrderNo }) => {
  const statusInfo = STATUS_MAP[status] || STATUS_MAP.cancelled

  return (
    <View className='status-header' style={{ background: statusInfo.bg }}>
      <View className='status-info'>
        <AtTag
          type='primary'
          size='large'
          circle
          customStyle={{
            background: statusInfo.color,
            color: 'white',
            border: 'none'
          }}
        >
          {statusInfo.text}
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
      {status !== 'cancelled' && status !== 'pending_payment' && (
        <View className='delivery-progress'>
          <AtProgress
            percent={statusInfo.step * 25}
            strokeWidth={4}
            color={statusInfo.color}
            isShowPercent={false}
          />
          <View className='progress-steps'>
            <View className={`step-item ${statusInfo.step >= 1 ? 'active' : ''}`}>
              <View className='step-dot' />
              <Text className='step-text'>下单</Text>
            </View>
            <View className={`step-item ${statusInfo.step >= 2 ? 'active' : ''}`}>
              <View className='step-dot' />
              <Text className='step-text'>付款</Text>
            </View>
            <View className={`step-item ${statusInfo.step >= 3 ? 'active' : ''}`}>
              <View className='step-dot' />
              <Text className='step-text'>配送</Text>
            </View>
            <View className={`step-item ${statusInfo.step >= 4 ? 'active' : ''}`}>
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
