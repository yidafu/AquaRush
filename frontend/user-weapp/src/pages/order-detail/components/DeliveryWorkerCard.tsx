import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtCard, AtButton, AtIcon } from 'taro-ui'
import type { DeliveryInfo } from './constants'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/icon.scss"
import './DeliveryWorkerCard.scss'

interface DeliveryWorkerCardProps {
  deliveryInfo: DeliveryInfo
  onContact: () => void
  onTrack: () => void
}

/**
 * 配送员信息卡(配送中订单):头像/姓名/电话/当前位置 + 联系/导航按钮
 */
const DeliveryWorkerCard: React.FC<DeliveryWorkerCardProps> = ({ deliveryInfo, onContact, onTrack }) => {
  return (
    <AtCard
      title='配送员信息'
      className='delivery-info-card shadow-default'
    >
      <View className='delivery-worker'>
        <View className='worker-avatar'>
          <Image
            src={deliveryInfo.deliveryWorkerAvatar || '/assets/default-avatar.png'}
            mode='aspectFill'
            className='avatar-image'
          />
        </View>
        <View className='worker-info'>
          <Text className='worker-name'>{deliveryInfo.deliveryWorkerName}</Text>
          <Text className='worker-phone'>{deliveryInfo.deliveryWorkerPhone}</Text>
          <Text className='delivery-location'>
            <AtIcon value='map-pin' size='12' color='var(--text-sub)' />
            {deliveryInfo.currentLocation} · {deliveryInfo.deliveryDistance}
          </Text>
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
          <AtButton
            type='secondary'
            size='small'
            circle
            onClick={onTrack}
          >
            <AtIcon value='navigation' size='14' color='var(--text-sub)' />
          </AtButton>
        </View>
      </View>
    </AtCard>
  )
}

export default DeliveryWorkerCard
