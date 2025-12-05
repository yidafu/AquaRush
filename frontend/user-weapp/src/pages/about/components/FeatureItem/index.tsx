import React from 'react'
import { View, Text } from '@tarojs/components'
import './index.scss'

interface FeatureItemProps {
  icon: string
  title: string
  description: string
}

const FeatureItem: React.FC<FeatureItemProps> = ({ icon, title, description }) => {
  return (
    <View className='feature-item'>
      <View className='feature-icon'>{icon}</View>
      <View className='feature-content'>
        <Text className='feature-title'>{title}</Text>
        <Text className='feature-desc'>{description}</Text>
      </View>
    </View>
  )
}

export default FeatureItem