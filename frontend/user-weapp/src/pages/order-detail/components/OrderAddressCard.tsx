import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'
import type { OrderDetailAddress } from './constants'

import "taro-ui/dist/style/components/card.scss"
import './OrderAddressCard.scss'

interface OrderAddressCardProps {
  address: OrderDetailAddress
}

/**
 * 收货地址卡:收货人/电话 + 完整地址(省市区 + 详细地址)
 */
const OrderAddressCard: React.FC<OrderAddressCardProps> = ({ address }) => {
  return (
    <AtCard
      title='收货地址'
      className='address-card shadow-default'
    >
      <View className='address-content'>
        <View className='address-header'>
          <Text className='receiver-name'>{address.receiverName}</Text>
          <Text className='receiver-phone'>{address.phone}</Text>
        </View>
        <Text className='address-detail'>
          {address.province} {address.city} {address.district} {address.detailAddress}
        </Text>
      </View>
    </AtCard>
  )
}

export default OrderAddressCard
