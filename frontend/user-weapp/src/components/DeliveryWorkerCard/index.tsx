import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtCard, AtButton, AtIcon } from 'taro-ui'
import StatusTag from '../StatusTag'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/icon.scss"
import './index.scss'

/**
 * 配送员信息卡(配送中订单) —— 收敛 order-detail 与 delivery-tracking 两处同名实现:
 * 头像/姓名/电话/位置(+距离)+ 联系按钮 + 可选导航按钮 + 可选配送状态行(配送中标签 + 预计送达)。
 * 状态标签走共享 StatusTag(tone 色值见 common STATUS_TONE_COLORS)。
 */
interface DeliveryWorkerCardProps {
  name?: string
  phone?: string
  avatar?: string
  /** 当前位置文案 */
  location?: string
  /** 距离文案(如「500m」) */
  distance?: string
  /** 展示配送中状态行 */
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
    <AtCard
      title='配送员信息'
      className='delivery-worker-card shadow-default'
    >
      <View className='worker-header'>
        <View className='worker-avatar'>
          <Image
            src={avatar || '/assets/default-avatar.png'}
            mode='aspectFill'
            className='avatar-image'
          />
        </View>
        <View className='worker-info'>
          <Text className='worker-name'>{name || '配送员'}</Text>
          <Text className='worker-phone'>{phone || ''}</Text>
          {location && (
            <Text className='worker-location'>
              <AtIcon value='map-pin' size='12' color='var(--text-sub)' />
              {location}
              {distance ? ` · ${distance}` : ''}
            </Text>
          )}
        </View>
        <View className='worker-actions'>
          <AtButton
            type='primary'
            size='small'
            circle
            onClick={onContact}
          >
            <AtIcon value='phone' size='14' color='white' />
          </AtButton>
          {onTrack && (
            <AtButton
              type='secondary'
              size='small'
              circle
              onClick={onTrack}
            >
              <AtIcon value='navigation' size='14' color='var(--text-sub)' />
            </AtButton>
          )}
        </View>
      </View>

      {/* 配送状态标签 */}
      {showStatus && (
        <View className='delivery-status'>
          <StatusTag tone='success' text='配送中' size='small' />
          {estimatedDeliveryTime && (
            <Text className='status-text'>预计 {estimatedDeliveryTime} 送达</Text>
          )}
        </View>
      )}
    </AtCard>
  )
}

export default DeliveryWorkerCard
