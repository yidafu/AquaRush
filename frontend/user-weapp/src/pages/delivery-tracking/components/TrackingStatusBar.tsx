import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import { props } from '@aquarush/common/stylex'

/** 提示条样式(原 TrackingStatusBar.scss:仅补与地图的间距) */
const styles = stylex.create({
  notice: {
    marginTop: '24rpx',
    marginRight: '24rpx',
    marginBottom: '0',
    marginLeft: '24rpx'
  }
})

/**
 * 实时跟踪提示条 —— WeUI .weui-notice--brand(设计稿 02-order/delivery-tracking.html:
 * 「实时跟踪中,每 10 秒更新位置」)
 */
const TrackingStatusBar: React.FC = () => {
  return (
    <View {...props(styles.notice, 'weui-notice', 'weui-notice--brand')}>
      <Text>实时跟踪中，每10秒更新位置</Text>
    </View>
  )
}

export default TrackingStatusBar
