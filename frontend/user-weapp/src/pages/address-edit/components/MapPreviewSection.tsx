import React from 'react'
import { View } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import AddressMap from '../../../components/AddressMap'

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

// 地图选点(原 MapPreviewSection.scss;长度保持 scss 来源 750 基准 rpx)
const styles = stylex.create({
  mapWrap: {
    paddingBottom: '16rpx',
  },
  // 地图圆角裁剪(原 .address-edit-map,原挂在 AddressMap 根节点上)
  mapFrame: {
    borderRadius: 'var(--weui-radius)',
    overflow: 'hidden',
  },
})

/**
 * 地址预览地图区块(address-edit.html「地图选点」:weui-cells__title + 地图 + tips):
 * 地图自动定位 + 手动选点(onLocationChange 回传坐标)
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
    <View>
      <View className='weui-cells__title'>地图选点</View>
      <View {...props('mp-pad-16', styles.mapWrap)}>
        <View {...stylex.props(styles.mapFrame)}>
          <AddressMap
            address={detailAddress}
            province={province}
            city={city}
            district={district}
            latitude={latitude}
            longitude={longitude}
            height='200px'
            showControls={true}
            onLocationChange={onLocationChange}
          />
        </View>
      </View>
      <View className='weui-cells__tips'>地图会根据地址信息自动定位,也可以手动选择位置;确认后自动回填详细地址与经纬度。</View>
    </View>
  )
}

export default MapPreviewSection
