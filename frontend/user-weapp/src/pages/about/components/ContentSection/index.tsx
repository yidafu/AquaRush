import React from 'react'
import { View, Text } from '@tarojs/components'
import './index.scss'

interface ContentSectionProps {
  title: string
  children: React.ReactNode
}

const ContentSection: React.FC<ContentSectionProps> = ({ title, children }) => {
  return (
    <View className='section'>
      <Text className='section-title'>{title}</Text>
      {children}
    </View>
  )
}

export default ContentSection