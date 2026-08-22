import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import type { DeliveryOrder } from '../hooks/useDeliveryTracking'

import "taro-ui/dist/style/components/card.scss"
import './DeliveryAddressCard.scss'

interface DeliveryAddressCardProps {
  deliveryAddress: DeliveryOrder['deliveryAddress']
}

/**
 * 配送地址卡:收货人/电话 + 完整地址
 */
const DeliveryAddressCard: React.FC<DeliveryAddressCardProps> = ({ deliveryAddress }) => {
  return (
    <AtCard
      title='配送地址'
      className='address-card'
    >
      <View className='address-content'>
        <View className='address-header'>
          <Text className='receiver-name'>{deliveryAddress.receiverName}</Text>
          <Text className='receiver-phone'>{deliveryAddress.phone}</Text>
        </View>
        <Text className='address-detail'>
          {deliveryAddress.province} {deliveryAddress.city} {deliveryAddress.district} {deliveryAddress.detailAddress}
        </Text>
      </View>
    </AtCard>
  )
}

export default DeliveryAddressCard
