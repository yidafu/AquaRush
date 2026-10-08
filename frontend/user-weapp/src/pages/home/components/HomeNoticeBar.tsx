import React from 'react'
import { View, Text } from '@tarojs/components'

import './HomeNoticeBar.scss'

interface HomeNoticeBarProps {
  title: string
  onClick: () => void
}

/**
 * 顶部公告栏(浅蓝 tint 底,仅生效公告时显示,F-OPS-002)
 */
const HomeNoticeBar: React.FC<HomeNoticeBarProps> = ({ title, onClick }) => {
  return (
    <View className='notice-bar' onClick={onClick}>
      <Text className='notice-bar-label'>公告</Text>
      <Text className='notice-bar-title' numberOfLines={1}>{title}</Text>
    </View>
  )
}

export default HomeNoticeBar
