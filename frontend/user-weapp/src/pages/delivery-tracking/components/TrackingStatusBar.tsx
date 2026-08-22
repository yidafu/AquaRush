import React from 'react'
import { View, Text } from '@tarojs/components'
import { AtIcon } from 'taro-ui'

import "taro-ui/dist/style/components/icon.scss"
import './TrackingStatusBar.scss'

/**
 * 实时跟踪提示条:每 10 秒更新配送员位置
 */
const TrackingStatusBar: React.FC = () => {
  return (
    <View className='tracking-info'>
      <View className='tracking-status'>
        <AtIcon value='clock' size='16' color='var(--brand-primary)' />
        <Text className='tracking-text'>实时跟踪中，每10秒更新位置</Text>
      </View>
    </View>
  )
}

export default TrackingStatusBar
