import React from 'react'
import { View, Text } from '@tarojs/components'
import CustomIcon from '@/components/CustomIcon'

import './AddressBar.scss'

interface AddressBarProps {
  addressText: string
  onAddressClick: () => void
}

/**
 * 渐变 hero 内定位行(home.html .hero-loc):
 * 图钉图标 + 收货地址 + 「切换」;点击进地址列表选择。
 */
const AddressBar: React.FC<AddressBarProps> = ({ addressText, onAddressClick }) => {
  return (
    <View className='address-bar' onClick={onAddressClick}>
      <View className='address-pin'>
        <CustomIcon value='/assets/icons/service/map-pin' size={16} />
      </View>
      <Text className='address-text' numberOfLines={1}>{addressText}</Text>
      <Text className='address-more'>切换 ▾</Text>
    </View>
  )
}

export default AddressBar
