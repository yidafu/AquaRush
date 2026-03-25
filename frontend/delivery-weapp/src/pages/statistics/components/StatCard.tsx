import React from 'react'
import { View, Text } from '@tarojs/components'
import './StatCard.scss'

interface StatCardProps {
  value: string | number
  label: string
  highlight?: boolean
  onClick?: () => void
}

export const StatCard: React.FC<StatCardProps> = ({ value, label, highlight = false, onClick }) => {
  return (
    <View
      className={`stat-card ${highlight ? 'highlight' : ''}`}
      onClick={onClick}
    >
      <Text className='stat-value'>{value}</Text>
      <Text className='stat-label'>{label}</Text>
    </View>
  )
}
