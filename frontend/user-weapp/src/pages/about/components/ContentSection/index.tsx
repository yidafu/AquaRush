import React from 'react'
import { View } from '@tarojs/components'

interface ContentSectionProps {
  title: string
  children: React.ReactNode
}

/** 分区标题(about.html .weui-cells__title)+ 分区内容插槽 */
const ContentSection: React.FC<ContentSectionProps> = ({ title, children }) => {
  return (
    <View>
      <View className='weui-cells__title'>{title}</View>
      {children}
    </View>
  )
}

export default ContentSection
