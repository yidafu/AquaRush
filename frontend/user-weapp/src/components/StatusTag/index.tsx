import React from 'react'
import { Text } from '@tarojs/components'
import {
  ORDER_STATUS_TONE,
  getOrderStatusText,
  getStatusTone,
  type StatusTone
} from '@aquarush/common'

import './index.scss'

/**
 * 状态标签(§2.2:10% tint 底 + 状态色文字,圆角 8)—— weapp 统一实现
 *
 * tone 语义与色值来源:@aquarush/common/utils/status-tone(STATUS_TONE_COLORS),
 * 本组件把 tone 映射为 styles/common.scss 的 .status-tag--* 修饰符(CSS 变量落地,
 * 值由 src/styles/tokens.test.ts 同步守卫锁定 = common STATUS_TONE_COLORS)。
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
  size?: 'small' | 'normal'
}

/** tone → .status-tag--* 修饰符(CSS 变量落地,见 styles/common.scss) */
const TONE_MODIFIER: Record<StatusTone, string> = {
  warning: 'status-tag--pending-pay',
  info: 'status-tag--dispatch',
  success: 'status-tag--delivering',
  neutral: 'status-tag--completed',
  error: 'status-tag--cancelled',
  accent: 'status-tag--action',
}

const StatusTag: React.FC<StatusTagProps> = ({ status, tone, text, size = 'normal' }) => {
  const resolvedTone = tone ?? getStatusTone(ORDER_STATUS_TONE, status)
  const resolvedText = text ?? getOrderStatusText(status)
  if (!resolvedTone || !resolvedText) return null

  return (
    <Text
      className={`status-tag ${TONE_MODIFIER[resolvedTone]}${size === 'small' ? ' status-tag--small' : ''}`}
    >
      {resolvedText}
    </Text>
  )
}

export default StatusTag
