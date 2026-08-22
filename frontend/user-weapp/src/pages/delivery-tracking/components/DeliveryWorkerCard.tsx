import React from 'react'
import { View, Text, Image } from '@tarojs/components'
import { AtCard, AtButton, AtIcon, AtTag } from 'taro-ui'
import type { DeliveryWorker } from '../hooks/useDeliveryTracking'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/icon.scss"
import "taro-ui/dist/style/components/tag.scss"
import './DeliveryWorkerCard.scss'

interface DeliveryWorkerCardProps {
  worker: DeliveryWorker
  estimatedDeliveryTime?: string
  onContact: () => void
}

/**
 * 配送员信息卡:头像/姓名/电话/当前位置 + 联系按钮 + 配送状态标签(预计送达)
 */
const DeliveryWorkerCard: React.FC<DeliveryWorkerCardProps> = ({ worker, estimatedDeliveryTime, onContact }) => {
  return (
    <AtCard
      title='配送员信息'
      className='delivery-worker-card'
    >
      <View className='worker-info'>
        <View className='worker-header'>
          <View className='worker-avatar'>
            <Image
              src={worker.avatar || '/assets/default-avatar.png'}
              mode='aspectFill'
              className='avatar-image'
            />
          </View>
          <View className='worker-details'>
            <Text className='worker-name'>{worker.name}</Text>
            <Text className='worker-phone'>{worker.phone}</Text>
            {worker.currentLocation && (
              <Text className='worker-location'>
                <AtIcon value='map-pin' size='12' color='var(--text-sub)' />
                {worker.currentLocation.address || '位置更新中...'}
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
          </View>
        </View>

        {/* 配送状态标签 */}
        <View className='delivery-status'>
          <AtTag
            type='primary'
            size='small'
            circle
            customStyle={{
              background: 'var(--status-delivering-bg)',
              color: 'var(--status-delivering-text)',
              border: 'none'
            }}
          >
            配送中
          </AtTag>
          <Text className='status-text'>
            预计 {estimatedDeliveryTime} 送达
          </Text>
        </View>
      </View>
    </AtCard>
  )
}

export default DeliveryWorkerCard
