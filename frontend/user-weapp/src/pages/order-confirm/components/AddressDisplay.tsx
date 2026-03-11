import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtIcon } from 'taro-ui'
import { Address } from '../../../types/address'
import AddressCard from '../../../components/AddressCard'

import "taro-ui/dist/style/components/icon.scss"
import './AddressDisplay.scss'

interface AddressDisplayProps {
  selectedAddress: Address | null
  onAddressSelect: () => void
}

const AddressDisplay: React.FC<AddressDisplayProps> = ({
  selectedAddress,
  onAddressSelect
}) => {
  return (
    <View className='section address-section'>
      <View className='section-header'>
        <Text className='section-title'>收货地址</Text>
      </View>
      {selectedAddress ? (
        <View className='address-display-wrapper' onClick={onAddressSelect}>
          <AddressCard
            address={selectedAddress}
            hideActions={true}
          />
          <View className='flex items-center justify-end chevron-indicator'>
            <AtIcon value='chevron-right' size='16' className='theme-text-secondary' />
          </View>
        </View>
      ) : (
        <View className='address-empty' onClick={onAddressSelect}>
          <View className='no-address-icon'>
            <AtIcon value='map-pin' size='24' />
          </View>
          <Text className='no-address-text'>请选择收货地址</Text>
          <View className='select-address-btn'>
            <Text className='btn-text'>选择地址</Text>
            <AtIcon value='chevron-right' size='16' />
          </View>
        </View>
      )}
    </View>
  )
}

export default AddressDisplay