import React from 'react'
import { View } from '@tarojs/components'
import { Map } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'

/** 配送地图区块样式(原 TrackingMap.scss 迁移;750 基准 rpx,weui 平台变量保留字符串引用) */
const styles = stylex.create({
  container: {
    position: 'relative',
    height: '60vh',
    minHeight: '600rpx',
    width: '100%'
  },
  map: {
    width: '100%',
    height: '100%'
  },
  ctrl: {
    position: 'absolute',
    top: '32rpx',
    right: '32rpx',
    display: 'flex',
    flexDirection: 'column',
    rowGap: '16rpx',
    zIndex: 100
  },
  ctrlBtn: {
    width: '80rpx',
    height: '80rpx',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    // 原 scss 阴影值 tokens 无对应项(黑 15% 抬升阴影),按原值保留
    boxShadow: '0 4rpx 12rpx rgba(0, 0, 0, 0.15)',
    // 品牌底上的前景白:与 weui.scss(.weui-btn_primary 等)一致的前景白
    backgroundColor: 'var(--weui-BRAND)',
    color: tokens.onBrand,
    borderRadius: '50%',
    fontSize: '40rpx',
    lineHeight: 1
  },
  ctrlBtnRefreshing: {
    opacity: 0.7
  }
})

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
    <View {...stylex.props(styles.container)}>
      <Map
        {...stylex.props(styles.map)}
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
      <View {...stylex.props(styles.ctrl)}>
        <View
          {...props(styles.ctrlBtn, refreshing && styles.ctrlBtnRefreshing)}
          onClick={onRefresh}
        >
          ⟳
        </View>
        <View {...stylex.props(styles.ctrlBtn)} onClick={onShowRoute}>
          ↗
        </View>
      </View>
    </View>
  )
}

export default TrackingMap
