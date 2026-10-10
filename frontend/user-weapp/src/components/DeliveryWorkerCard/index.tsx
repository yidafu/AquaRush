import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import StatusTag from '../StatusTag'
import './index.scss'

/**
 * 配送员信息卡(配送中订单)—— WeUI 2.x 视觉(.weui-cells 单元格:
 * 头像 + 姓名 + 配送中标签(.weui-tag--deliver)+ 电话/位置行 + 右侧操作按钮,
 * 设计稿 02-order/order-detail.html 配送员信息卡口径)。
 * 收敛 order-detail 与 delivery-tracking 两处同名实现;状态标签走共享 StatusTag
 * (tone 色值见 common STATUS_TONE_COLORS)。
 */
interface DeliveryWorkerCardProps {
  name?: string
  phone?: string
  avatar?: string
  /** 当前位置文案 */
  location?: string
  /** 距离文案(如「500m」) */
  distance?: string
  /** 展示配送中状态行(姓名旁标签 + 预计送达行) */
  showStatus?: boolean
  /** 预计送达文案 */
  estimatedDeliveryTime?: string
  onContact: () => void
  onTrack?: () => void
}

const DeliveryWorkerCard: React.FC<DeliveryWorkerCardProps> = ({
  name,
  phone,
  avatar,
  location,
  distance,
  showStatus = false,
  estimatedDeliveryTime,
  onContact,
  onTrack
}) => {
  return (
    <View className='worker-card'>
      <View className='weui-cells'>
        <View className='weui-cell'>
          <View className='weui-cell__hd'>
            <Image
              src={avatar || '/assets/default-avatar.png'}
              mode='aspectFill'
              className='worker-avatar'
            />
          </View>
          <View className='weui-cell__bd'>
            <View className='worker-name-row'>
              <Text className='worker-name'>{name || '配送员'}</Text>
              {showStatus && <StatusTag tone='success' text='配送中' size='small' />}
            </View>
            <Text className='worker-meta'>
              {phone ? `电话 ${phone}` : ''}
              {location ? ` · ${location}` : ''}
              {distance ? ` · 距你约 ${distance}` : ''}
            </Text>
          </View>
          <View className='weui-cell__ft'>
            {onTrack && (
              <View
                className='weui-btn weui-btn_plain-default weui-btn_xs'
                onClick={onTrack}
              >
                查看跟踪
              </View>
            )}
            {onContact && (
              <View
                className='weui-btn weui-btn_plain-primary weui-btn_xs'
                onClick={onContact}
              >
                联系
              </View>
            )}
          </View>
        </View>

        {showStatus && estimatedDeliveryTime && (
          <View className='weui-cell'>
            <View className='weui-cell__bd'>预计送达</View>
            <View className='weui-cell__ft worker-eta'>{estimatedDeliveryTime}</View>
          </View>
        )}
      </View>
    </View>
  )
}

export default DeliveryWorkerCard
