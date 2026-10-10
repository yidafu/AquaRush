import React from 'react'
import { View, Text } from '@tarojs/components'
import './TrackingStatusBar.scss'

/**
 * 实时跟踪提示条 —— WeUI .weui-notice--brand(设计稿 02-order/delivery-tracking.html:
 * 「实时跟踪中,每 10 秒更新位置」)
 */
const TrackingStatusBar: React.FC = () => {
  return (
    <View className='tracking-notice weui-notice weui-notice--brand'>
      <Text>实时跟踪中，每10秒更新位置</Text>
    </View>
  )
}

export default TrackingStatusBar
