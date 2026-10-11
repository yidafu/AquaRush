import React from 'react'
import { View, Text } from '@tarojs/components'
import type { OrderTimeline } from './constants'
import './OrderTimelineCard.scss'

interface OrderTimelineCardProps {
  timeline: OrderTimeline[]
}

const toLines = (content?: string): string[] => {
  if (!content) return []
  return Array.isArray(content) ? content : [content]
}

/**
 * 订单跟踪时间轴卡 —— 保持现有「事件列表」展示形式,换 WeUI 皮肤
 * (.weui-cells 纵向行:标题 + 时间,说明行;数据结构/状态色语义不变)。
 */
const OrderTimelineCard: React.FC<OrderTimelineCardProps> = ({ timeline }) => {
  if (!Array.isArray(timeline) || timeline.length === 0) {
    return null
  }

  return (
    <View className='timeline-card'>
      <View className='weui-cells__title'>订单跟踪</View>
      <View className='weui-cells'>
        {timeline.map((item, index) => {
          const lines = toLines(item.content)
          return (
            <View
              key={`${item.time}-${index}`}
              className='weui-cell weui-cell_vertical timeline-item'
            >
              <View className='timeline-head'>
                <Text className='timeline-title'>{item.title || '状态更新'}</Text>
                <Text className='timeline-time'>{item.time}</Text>
              </View>
              {lines.map((line, lineIndex) => (
                <Text key={lineIndex} className='timeline-content'>{line}</Text>
              ))}
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default OrderTimelineCard
