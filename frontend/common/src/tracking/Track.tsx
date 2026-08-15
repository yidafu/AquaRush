import React from 'react'
import { getTracker, type UserActionTracker } from './tracker'

/**
 * 声明式埋点注入组件(平台无关:weapp / 浏览器 / RN)
 *
 * 用法(业务零感知,handler 写在子元素上):
 *   <Track target="buy-confirm" props={{ productId: id }}>
 *     <Button onClick={handleConfirm}>确认购买</Button>
 *   </Track>
 *
 * 机制:通过 React.cloneElement 把组合后的 handler 注入子元素的事件属性
 * (onPress / onClick / onSubmit / onConfirm 中第一个已存在的),先上报、后转发
 * 子元素原有 handler;RN 嵌套可点击组件不再有"外层抢占 responder"问题。
 *
 * 业务代码通常不直接写 <Track>,而是由 babel-plugin-aquarush-track 把
 * track-* props 编译成此组件。
 */
export interface TrackProps {
  /** 动作类型,缺省 CLICK */
  action?: string
  /** 必填:操作对象标识(如 add-to-cart、order-submit) */
  target: string
  /** 附加属性(合并进 properties) */
  props?: Record<string, unknown>
  /** 元素标识;缺省用 target */
  elementId?: string
  /** 元素类型(如 button/link/item);缺省 'element' */
  elementType?: string
  /** 元素文本(如按钮文案) */
  elementText?: string
  /** 禁用时跳过上报,但仍转发事件 */
  disabled?: boolean
  /** 追踪器;缺省使用 initTracking 创建的全局单例 */
  tracker?: UserActionTracker
  /** 必须为单个元素(注入事件属性的对象) */
  children: React.ReactElement
}

const EVENT_PROPS = ['onPress', 'onClick', 'onSubmit', 'onConfirm'] as const
type EventProp = (typeof EVENT_PROPS)[number]

const Track: React.FC<TrackProps> = ({
  action = 'CLICK',
  target,
  props: extraProps,
  elementId,
  elementType,
  elementText,
  disabled = false,
  tracker,
  children,
}) => {
  if (!React.isValidElement(children)) {
    console.warn('[tracking] Track requires a single element child, skipped:', target)
    return children as React.ReactElement
  }

  const childProps = children.props as Record<string, unknown>
  const eventProp = (EVENT_PROPS.find((p) => p in childProps) ?? 'onClick') as EventProp
  const original = childProps[eventProp] as ((e: unknown) => void) | undefined

  const composed = (e: unknown) => {
    if (!disabled) {
      const t = tracker ?? getTracker()
      t.track(action, target, {
        elementId: elementId ?? target,
        elementType: elementType ?? 'element',
        elementText,
        properties: extraProps,
      })
    }
    original?.(e)
  }

  return React.cloneElement(children, { [eventProp]: composed })
}

export default Track
