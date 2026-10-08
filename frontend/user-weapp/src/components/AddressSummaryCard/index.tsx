import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtCard } from 'taro-ui'

import "taro-ui/dist/style/components/card.scss"
import './index.scss'

/**
 * 只读收货/配送地址卡 —— 收敛 order-detail OrderAddressCard 与 delivery-tracking
 * DeliveryAddressCard 两处近重复实现(收货人/电话 + 完整地址;title 区分「收货地址/配送地址」)。
 * 共享 CRUD 版 AddressCard(选择/编辑/删除)是另一变体,不合并。
 */
interface AddressSummaryCardProps {
  title?: string
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detailAddress: string
}

const AddressSummaryCard: React.FC<AddressSummaryCardProps> = ({
  title = '收货地址',
  receiverName,
  phone,
  province,
  city,
  district,
  detailAddress
}) => {
  return (
    <AtCard
      title={title}
      className='address-summary-card shadow-default'
    >
      <View className='address-content'>
        <View className='address-header'>
          <Text className='receiver-name'>{receiverName}</Text>
          <Text className='receiver-phone'>{phone}</Text>
        </View>
        <Text className='address-detail'>
          {province} {city} {district} {detailAddress}
        </Text>
      </View>
    </AtCard>
  )
}

export default AddressSummaryCard
