import React from 'react'
import { View } from '@tarojs/components'

interface FeatureItemProps {
  icon: string
  title: string
  description: string
}

/** 品牌承诺行(about.html .weui-media-box:品牌蓝图标 + 标题 + 描述) */
const FeatureItem: React.FC<FeatureItemProps> = ({ icon, title, description }) => {
  return (
    <View className='weui-media-box feature-item'>
      <View className='weui-media-box__hd'>
        <View className='weui-cell__icon weui-cell__icon--brand feature-icon'>{icon}</View>
      </View>
      <View className='weui-media-box__bd'>
        <View className='weui-media-box__title'>{title}</View>
        <View className='weui-media-box__desc'>{description}</View>
      </View>
    </View>
  )
}

export default FeatureItem
