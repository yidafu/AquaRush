import React from 'react'
import { Text } from '@tarojs/components'
import {
  ORDER_STATUS_TONE,
  getOrderStatusText,
  getStatusTone,
  type StatusTone
} from '@aquarush/common'

/**
 * 状态标签 —— WeUI 2.x 视觉(.weui-tag 体系,设计稿 02-order/order-list.html 状态标签口径)
 *
 * tone 语义与色值来源:@aquarush/common/utils/status-tone(STATUS_TONE_COLORS,
 * 单一来源);本组件把 tone 映射为 weui.scss 的 .weui-tag--* 修饰符:
 *   warning→--pay / info→--assign / success→--deliver / neutral→--done / error→--cancel
 * (accent 为消费者端强调色,weapp 订单状态不命中,映射 --red 兜底)。
 *
 * 用法:
 * - <StatusTag status={order.status} />                     // 订单状态(自动取文案+tone)
 * - <StatusTag tone='success' text='已处理' />              // 其他域(反馈/券等)显式传
 * - <StatusTag status={record.status} text={...} tone={...} /> // 文案/颜色覆盖
 */
interface StatusTagProps {
  /** 后端状态枚举键(自动派生文案与 tone;如 order.status) */
  status?: string
  /** 显式 tone(优先于 status 推导) */
  tone?: StatusTone
  /** 显式文案(优先于 status 推导) */
  text?: string
  /** small=列表标签(weui-tag 36px 高);normal=状态头大号(weui-tag--md 44px) */
  size?: 'small' | 'normal'
}

/** tone → .weui-tag--* 修饰符(weui.scss,设计稿 02-order 口径) */
const TONE_MODIFIER: Record<StatusTone, string> = {
  warning: 'weui-tag--pay',
  info: 'weui-tag--assign',
  success: 'weui-tag--deliver',
  neutral: 'weui-tag--done',
  error: 'weui-tag--cancel',
  accent: 'weui-tag--red',
}

const StatusTag: React.FC<StatusTagProps> = ({ status, tone, text, size = 'normal' }) => {
  const resolvedTone = tone ?? getStatusTone(ORDER_STATUS_TONE, status)
  const resolvedText = text ?? getOrderStatusText(status)
  if (!resolvedTone || !resolvedText) return null

  return (
    <Text
      className={`weui-tag ${TONE_MODIFIER[resolvedTone]}${size === 'normal' ? ' weui-tag--md' : ''}`}
    >
      {resolvedText}
    </Text>
  )
}

export default StatusTag
