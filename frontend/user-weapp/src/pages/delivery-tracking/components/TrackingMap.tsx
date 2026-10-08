import React from 'react'
import { View } from '@tarojs/components'
import { Map } from '@tarojs/components'
import { AtButton, AtIcon } from 'taro-ui'

import "taro-ui/dist/style/components/button.scss"
import "taro-ui/dist/style/components/icon.scss"
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
 * 配送地图区块:地图 + 右上角控制按钮(刷新位置/查看路线)
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
      />

      {/* 地图控制按钮 */}
      <View className='map-controls'>
        <AtButton
          type='primary'
          size='small'
          circle
          onClick={onRefresh}
          loading={refreshing}
          className='control-button'
        >
          <AtIcon value='refresh' size='16' color='white' />
        </AtButton>
        <AtButton
          type='primary'
          size='small'
          circle
          onClick={onShowRoute}
          className='control-button'
        >
          <AtIcon value='navigation' size='16' color='white' />
        </AtButton>
      </View>
    </View>
  )
}

export default TrackingMap
