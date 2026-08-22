import React from 'react'
import { View, Text } from '@tarojs/components'

import './AddressBar.scss'

interface AddressBarProps {
  addressText: string
  onAddressClick: () => void
  onSearchClick: () => void
}

/**
 * ① 顶部地址栏(44px 白底固定顶部,右侧搜索入口)
 */
const AddressBar: React.FC<AddressBarProps> = ({ addressText, onAddressClick, onSearchClick }) => {
  return (
    <View className='address-bar'>
      <View className='address-bar-left' onClick={onAddressClick}>
        <Text className='address-location-icon'>📍</Text>
        <Text className='address-text' numberOfLines={1}>{addressText}</Text>
        <Text className='address-arrow'>▾</Text>
      </View>
      <View className='address-bar-right' onClick={onSearchClick}>
        <Text className='search-icon'>🔍</Text>
      </View>
    </View>
  )
}

export default AddressBar
