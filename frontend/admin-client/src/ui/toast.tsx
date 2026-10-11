/**
 * antd `message` 门面 → 自研 toast。契约: docs/compose/spec/admin-radix-ui.md [S2.6]
 *
 * 纯函数调用、可在 React 组件树外使用(src/services、lib/apollo-client):
 * 首次调用时把容器懒挂到 document.body,经 toast-core 的 subscribe 消费队列;
 * 主 App 树不需要 Provider/挂载点。
 */
/* eslint-disable react-refresh/only-export-components -- [S2.6] 要求 message 门面与内部 viewport 组件同文件导出 */
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { useSyncExternalStore, type ComponentType, type ReactElement, type ReactNode } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  CloseOutlined,
  MessageOutlined,
  WarningOutlined,
  type IconProps,
} from './icons'
import { toastStore, type ToastItem, type ToastType } from './toast-core'

export type MessageType = ToastType

/** `message.open(config)` 入参(对齐 antd ArgsProps 子集)。 */
export interface MessageConfig {
  type: ToastType
  content: ReactNode
  /** 毫秒;<= 0 常驻。缺省走 TOAST_DEFAULT_DURATION。 */
  duration?: number
  /** 指定 key 覆盖同 key 既有提示(原位更新)。 */
  key?: string
}

// 图标集(76 个,与迁移前用集一致)无 InfoCircleOutlined —— info 用 MessageOutlined 近似
const TYPE_ICONS: Record<ToastType, ComponentType<IconProps>> = {
  success: CheckCircleOutlined,
  error: CloseCircleOutlined,
  warning: WarningOutlined,
  info: MessageOutlined,
}

const enterKeyframes = stylex.keyframes({
  from: { opacity: '0', transform: 'translateY(-8px)' },
  to: { opacity: '1', transform: 'translateY(0)' },
})

// 静态值全部进 stylex;类型相关的样式差异用按 type 的静态对象区分,无动态 style
const styles = stylex.create({
  viewport: {
    position: 'fixed',
    top: tokens.s4,
    left: '0px',
    right: '0px',
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    rowGap: tokens.s2,
    zIndex: 1100,
    // 视口本身不拦截下层点击,仅提示条可点
    pointerEvents: 'none',
  },
  notice: {
    pointerEvents: 'auto',
    display: 'flex',
    alignItems: 'center',
    columnGap: tokens.s3,
    paddingTop: tokens.s2,
    paddingBottom: tokens.s2,
    paddingLeft: tokens.s4,
    paddingRight: tokens.s3,
    maxWidth: '60vw',
    backgroundColor: tokens.card,
    borderRadius: tokens.rSm,
    boxShadow: tokens.shModal,
    color: tokens.text,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    cursor: 'pointer',
    animationName: enterKeyframes,
    animationDuration: '0.2s',
    animationTimingFunction: 'ease-out',
    animationFillMode: 'both',
  },
  icon: {
    fontSize: '16px',
    flexShrink: '0',
  },
  content: {
    flexGrow: '1',
    minWidth: '0px',
    wordBreak: 'break-word',
  },
  closeBtn: {
    flexShrink: '0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '24px',
    height: '24px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderWidth: '0px',
    backgroundColor: 'transparent',
    color: tokens.textWeak,
    cursor: 'pointer',
    borderRadius: tokens.rSm,
    ':hover': {
      color: tokens.text,
    },
  },
  closeIcon: {
    fontSize: '14px',
  },
})

// 图标语义色按 type 取静态样式(不用动态 style,遵循 StyleX 约定)
const typeIconStyles = stylex.create({
  success: { color: tokens.success },
  error: { color: tokens.error },
  warning: { color: tokens.warning },
  info: { color: tokens.statusInfoFg },
})

function ToastNotice({ item }: { item: ToastItem }): ReactElement {
  const Icon = TYPE_ICONS[item.type]
  const dismiss = (): void => {
    toastStore.destroy(item.key)
  }
  return (
    <div {...stylex.props(styles.notice)} onClick={dismiss}>
      <Icon {...stylex.props(styles.icon, typeIconStyles[item.type])} />
      <span {...stylex.props(styles.content)}>{item.content}</span>
      <button
        type="button"
        aria-label="关闭提示"
        {...stylex.props(styles.closeBtn)}
        onClick={dismiss}
      >
        <CloseOutlined {...stylex.props(styles.closeIcon)} />
      </button>
    </div>
  )
}

function ToastViewport(): ReactElement {
  const items = useSyncExternalStore(toastStore.subscribe, toastStore.getSnapshot)
  return (
    <div {...stylex.props(styles.viewport)}>
      {items.map((item) => (
        <ToastNotice key={item.key} item={item} />
      ))}
    </div>
  )
}

let root: Root | null = null

/** 懒挂载:首次 message 调用时把 viewport 容器挂到 body。 */
function ensureMounted(): void {
  if (root !== null) return
  const container = document.createElement('div')
  container.setAttribute('data-aquarush-toast', '')
  document.body.appendChild(container)
  root = createRoot(container)
  root.render(<ToastViewport />)
}

function show(config: MessageConfig): string {
  ensureMounted()
  return toastStore.push(config)
}

/**
 * antd message 门面:与 `import { message } from 'antd'` 同形,调用点只改 import。
 * 纯函数、组件树外可用;返回 key 供 `message.destroy(key)` 定点关闭。
 */
export const message = {
  success: (content: ReactNode, duration?: number): string =>
    show({ type: 'success', content, duration }),
  error: (content: ReactNode, duration?: number): string =>
    show({ type: 'error', content, duration }),
  warning: (content: ReactNode, duration?: number): string =>
    show({ type: 'warning', content, duration }),
  info: (content: ReactNode, duration?: number): string =>
    show({ type: 'info', content, duration }),
  open: (config: MessageConfig): string => show(config),
  destroy: (key?: string): void => {
    toastStore.destroy(key)
  },
}
