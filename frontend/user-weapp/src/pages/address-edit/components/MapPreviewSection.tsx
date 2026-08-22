import React from 'react'
import { View, Text } from '@tarojs/components'
import AddressMap from '../../../components/AddressMap'

import './MapPreviewSection.scss'

interface MapPreviewSectionProps {
  detailAddress: string
  province: string
  city: string
  district: string
  latitude?: number
  longitude?: number
  onLocationChange: (location: {
    latitude: number
    longitude: number
    address: string
  }) => void
}

/**
 * 地址预览地图区块:地图自动定位 + 手动选点(onLocationChange 回传坐标)
 */
const MapPreviewSection: React.FC<MapPreviewSectionProps> = ({
  detailAddress,
  province,
  city,
  district,
  latitude,
  longitude,
  onLocationChange
}) => {
  return (
    <View className='address-map-section'>
      <Text className='section-title'>地址预览</Text>
      <AddressMap
        address={detailAddress}
        province={province}
        city={city}
        district={district}
        latitude={latitude}
        longitude={longitude}
        height='200px'
        className='address-edit-map'
        showControls={true}
        onLocationChange={onLocationChange}
      />
      <Text className='map-hint'>地图会根据地址信息自动定位，也可以手动选择位置</Text>
    </View>
  )
}

export default MapPreviewSection
