import React from 'react'
import { View, Text } from '@tarojs/components'

interface FeatureItemProps {
  icon: string
  title: string
  description: string
}

const FeatureItem: React.FC<FeatureItemProps> = ({ icon, title, description }) => {
  return (
    <View className='flex items-start mb-8'>
      <View className='feature-icon'>{icon}</View>
      <View className='feature-content'>
        <Text className='feature-title'>{title}</Text>
        <Text className='feature-desc'>{description}</Text>
      </View>
    </View>
  )
}

export default FeatureItem