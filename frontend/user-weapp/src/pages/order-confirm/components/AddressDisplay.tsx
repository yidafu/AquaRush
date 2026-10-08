import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtIcon } from 'taro-ui'
import { Address } from '../../../types/address'

import "taro-ui/dist/style/components/icon.scss"
import './AddressDisplay.scss'

interface AddressDisplayProps {
  selectedAddress: Address | null
  onAddressSelect: () => void
}

/**
 * 收货地址卡(确认订单页 F-ORD-001)
 * 紧凑两行自绘布局(收货人+「默认」标签+电话 / 完整地址+右箭头),
 * 不再内嵌共享 AddressCard,避免「卡中卡」与字号混合;空态引导选择地址。
 */
const AddressDisplay: React.FC<AddressDisplayProps> = ({
  selectedAddress,
  onAddressSelect
}) => {
  return (
    <View className='section address-section' onClick={onAddressSelect}>
      {selectedAddress ? (
        <View className='address-display'>
          <View className='address-display-row'>
            <View className='address-name-row'>
              <Text className='address-name'>{selectedAddress.receiverName}</Text>
              {selectedAddress.isDefault && (
                <Text className='address-default-tag'>默认</Text>
              )}
            </View>
            <Text className='address-phone'>{selectedAddress.phone}</Text>
          </View>
          <View className='address-detail-row'>
            <Text className='address-detail'>
              {selectedAddress.province} {selectedAddress.city} {selectedAddress.district} {selectedAddress.detailAddress}
              {selectedAddress.floor ? ` ${selectedAddress.floor}楼` : ''}
            </Text>
            <AtIcon value='chevron-right' size='16' className='address-chevron' />
          </View>
        </View>
      ) : (
        <View className='address-empty'>
          <View className='address-empty-icon'>
            <AtIcon value='map-pin' size='24' />
          </View>
          <Text className='address-empty-text'>请选择收货地址</Text>
          <View className='address-empty-action'>
            <Text className='address-empty-btn'>选择地址</Text>
            <AtIcon value='chevron-right' size='14' />
          </View>
        </View>
      )}
    </View>
  )
}

export default AddressDisplay
