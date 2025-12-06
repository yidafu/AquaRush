import React from 'react'
import { View, Text } from '@tarojs/components'

interface ContentSectionProps {
  title: string
  children: React.ReactNode
}

const ContentSection: React.FC<ContentSectionProps> = ({ title, children }) => {
  return (
    <View className='mb-12'>
      <Text className='text-3xl font-bold text-primary mb-6 block relative pl-6'>{title}</Text>
      {children}
    </View>
  )
}

export default ContentSection