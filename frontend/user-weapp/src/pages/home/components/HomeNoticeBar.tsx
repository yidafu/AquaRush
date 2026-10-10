import React from 'react'
import { View, Text } from '@tarojs/components'

import './HomeNoticeBar.scss'

interface HomeNoticeBarProps {
  title: string
  onClick: () => void
}

/**
 * 通告栏(WeUI .weui-notice 橙 tint,仅生效公告时显示,F-OPS-002)
 */
const HomeNoticeBar: React.FC<HomeNoticeBarProps> = ({ title, onClick }) => {
  return (
    <View className='weui-notice home-notice' onClick={onClick}>
      <Text className='home-notice__label'>公告</Text>
      <Text className='home-notice__title' numberOfLines={1}>{title}</Text>
    </View>
  )
}

export default HomeNoticeBar
