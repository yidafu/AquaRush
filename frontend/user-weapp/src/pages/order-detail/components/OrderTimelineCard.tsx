import React from 'react'
import { AtCard, AtTimeline } from 'taro-ui'
import type { OrderTimeline } from './constants'

import "taro-ui/dist/style/components/card.scss"
import "taro-ui/dist/style/components/timeline.scss"
import './OrderTimelineCard.scss'

interface OrderTimelineCardProps {
  timeline: OrderTimeline[]
}

const getTimelineColor = (color?: string): 'blue' | 'green' | 'red' | 'yellow' => {
  if (!color) return 'blue'

  const colorMap: Record<string, 'blue' | 'green' | 'red' | 'yellow'> = {
    'var(--brand-primary)': 'blue',
    'var(--success)': 'green',
    'var(--action)': 'red',
    'var(--warning)': 'yellow',
    // Legacy color mappings for backward compatibility
    '#667eea': 'blue',
    '#19be6b': 'green',
    '#ff6b35': 'red',
    '#ffc107': 'yellow'
  }

  return colorMap[color] || 'blue'
}

/**
 * 订单跟踪时间轴卡(AtTimeline,状态色映射见 getTimelineColor)
 */
const OrderTimelineCard: React.FC<OrderTimelineCardProps> = ({ timeline }) => {
  if (!Array.isArray(timeline) || timeline.length === 0) {
    return null
  }

  return (
    <AtCard
      title='订单跟踪'
      className='timeline-card shadow-default'
    >
      <AtTimeline
        items={timeline.map(item => ({
          title: item.title || '状态更新',
          content: Array.isArray(item.content) ? item.content : [item.content || ''],
          icon: item.icon || 'clock',
          color: getTimelineColor(item.color)
        }))}
      />
    </AtCard>
  )
}

export default OrderTimelineCard
