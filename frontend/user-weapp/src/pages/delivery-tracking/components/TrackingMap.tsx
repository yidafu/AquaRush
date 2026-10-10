import React from 'react'
import { View } from '@tarojs/components'
import { Map } from '@tarojs/components'
import './TrackingMap.scss'

interface TrackingMapProps {
  latitude: number
  longitude: number
  markers: any[]
  refreshing: boolean
  onRefresh: () => void
  onShowRoute: () => void
}

/**
 * 配送地图区块(设计稿 02-order/delivery-tracking.html):地图 + 右上角圆形控制按钮
 * (⟳ 刷新位置 / ↗ 查看路线,原型 .map-ctrl 同款字形)
 */
const TrackingMap: React.FC<TrackingMapProps> = ({
  latitude,
  longitude,
  markers,
  refreshing,
  onRefresh,
  onShowRoute
}) => {
  return (
    <View className='map-container'>
      <Map
        className='delivery-map'
        latitude={latitude}
        longitude={longitude}
        scale={14}
        markers={markers}
        showLocation={true}
        showScale={true}
        showCompass={true}
        enableScroll={true}
        enableZoom={true}
        enableRotate={false}
        enable3D={false}
        onError={() => {}}
      />

      {/* 地图控制按钮 */}
      <View className='map-ctrl'>
        <View
          className={`ctrl-btn${refreshing ? ' refreshing' : ''}`}
          onClick={onRefresh}
        >
          ⟳
        </View>
        <View className='ctrl-btn' onClick={onShowRoute}>
          ↗
        </View>
      </View>
    </View>
  )
}

export default TrackingMap
