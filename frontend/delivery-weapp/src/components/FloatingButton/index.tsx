import React from 'react'
import { View, Text, ITouchEvent } from '@tarojs/components'
import './index.scss'

interface FloatingButtonProps {
  onClick?: (event: ITouchEvent) => void
  children?: React.ReactNode
}

export const FloatingButton: React.FC<FloatingButtonProps> = ({ onClick, children }) => {
  return (
    <View className='floating-button' onClick={onClick}>
      <Text className='floating-button__icon'>{children}</Text>
    </View>
  )
}
