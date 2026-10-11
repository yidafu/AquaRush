import React from 'react'
import { View, Text } from '@tarojs/components'
import * as stylex from '@stylexjs/stylex'
import type { OrderTimeline } from './constants'

interface OrderTimelineCardProps {
  timeline: OrderTimeline[]
}

// 订单跟踪时间轴:WeUI cells 骨架由 weui.scss 提供,仅补事件行排版
const styles = stylex.create({
  timelineHead: {
    display: 'flex',
    alignItems: 'baseline',
    justifyContent: 'space-between',
    columnGap: '24rpx',
  },
  timelineTitle: {
    fontSize: 'var(--weui-fs-cell)',
    fontWeight: 500,
    color: 'var(--weui-FG-0)',
  },
  timelineTime: {
    flexGrow: 0,
    flexShrink: 0,
    flexBasis: 'auto',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-2)',
    fontVariantNumeric: 'tabular-nums',
  },
  timelineContent: {
    display: 'block',
    marginTop: '8rpx',
    fontSize: 'var(--weui-fs-tips)',
    color: 'var(--weui-FG-1)',
    lineHeight: 1.5,
  },
})

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
    <View>
      <View className='weui-cells__title'>订单跟踪</View>
      <View className='weui-cells'>
        {timeline.map((item, index) => {
          const lines = toLines(item.content)
          return (
            <View
              key={`${item.time}-${index}`}
              className='weui-cell weui-cell_vertical'
            >
              <View {...stylex.props(styles.timelineHead)}>
                <Text {...stylex.props(styles.timelineTitle)}>{item.title || '状态更新'}</Text>
                <Text {...stylex.props(styles.timelineTime)}>{item.time}</Text>
              </View>
              {lines.map((line, lineIndex) => (
                <Text key={lineIndex} {...stylex.props(styles.timelineContent)}>{line}</Text>
              ))}
            </View>
          )
        })}
      </View>
    </View>
  )
}

export default OrderTimelineCard
