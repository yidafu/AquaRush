/**
 * Textarea(antd Input.TextArea 形状):受控 value + onChange 透传事件对象
 * (现网 JsonEditor / ProductFormFields / FeedbackDetailModal 均按事件取值),
 * rows / maxLength / showCount / allowClear 子集见 spec [S2.3]。
 */
import React from 'react'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { CloseCircleOutlined } from './icons'

export interface TextAreaProps
  extends Omit<React.TextareaHTMLAttributes<HTMLTextAreaElement>, 'onChange' | 'size'> {
  /** antd 同义:onChange 收到的是事件(取 e.target.value) */
  onChange?: (event: React.ChangeEvent<HTMLTextAreaElement>) => void
  /** 字数统计(showCount && maxLength 时在右下角展示 n/maxLength) */
  showCount?: boolean
  allowClear?: boolean
  status?: 'error' | 'warning'
  /** 经 Form.Item 克隆注入或页面直传 */
  size?: 'small' | 'middle' | 'large'
  ref?: React.Ref<HTMLTextAreaElement>
}

const styles = stylex.create({
  wrapper: {
    display: 'block',
    position: 'relative',
    width: '100%',
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    borderRadius: tokens.rSm,
    transitionProperty: 'border-color, box-shadow',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
    ':focus-within': {
      borderColor: tokens.brand,
      boxShadow: '0 0 0 2px rgba(58, 155, 217, 0.16)', // 品牌蓝 focus 环(设计规范「focus 品牌蓝描边」)
    },
  },
  wrapperSmall: {
    // 小号:rows 驱动高度,仅收紧左右内边距
    paddingLeft: '6px',
    paddingRight: '6px',
  },
  wrapperMiddle: {
    paddingLeft: '10px',
    paddingRight: '10px',
  },
  wrapperLarge: {
    paddingLeft: '14px',
    paddingRight: '14px',
  },
  wrapperError: {
    borderColor: tokens.error,
  },
  wrapperWarning: {
    borderColor: tokens.warning,
  },
  area: {
    display: 'block',
    width: '100%',
    paddingTop: '6px',
    paddingBottom: '6px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderStyle: 'none',
    borderWidth: '0px',
    outlineStyle: 'none',
    resize: 'vertical',
    backgroundColor: 'transparent',
    color: tokens.text,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    '::placeholder': {
      color: tokens.textWeak,
    },
    ':disabled': {
      cursor: 'not-allowed',
      color: tokens.textWeak,
    },
  },
  clearBtn: {
    position: 'absolute',
    top: '6px',
    right: '6px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '16px',
    height: '16px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '0px',
    marginRight: '0px',
    borderStyle: 'none',
    backgroundColor: 'transparent',
    color: tokens.textWeak,
    cursor: 'pointer',
    ':hover': {
      color: tokens.textSub,
    },
  },
  count: {
    display: 'block',
    marginTop: '2px',
    textAlign: 'right',
    color: tokens.textWeak,
    fontFamily: tokens.fontNum,
    fontSize: tokens.fsNote,
    lineHeight: tokens.lhNote,
  },
})

function buildClearEvent(next: string): React.ChangeEvent<HTMLTextAreaElement> {
  return { target: { value: next } } as React.ChangeEvent<HTMLTextAreaElement>
}

export function TextArea(props: TextAreaProps): React.ReactElement {
  const {
    value,
    defaultValue,
    onChange,
    showCount,
    allowClear,
    status,
    size = 'middle',
    className,
    style,
    disabled,
    rows = 3,
    maxLength,
    ref,
    ...rest
  } = props

  // 恒为受控渲染(value 始终传字符串),避免 uncontrolled→controlled 警告;
  // 仅带 defaultValue 的用法走本地 state(页面无 value+onChange 对时可输入)。
  const externallyControlled = value !== undefined && value !== null
  const [localText, setLocalText] = React.useState(() => String(defaultValue ?? ''))
  const [hovered, setHovered] = React.useState(false)
  const [focused, setFocused] = React.useState(false)

  const text = externallyControlled
    ? String(value)
    : defaultValue !== undefined
      ? localText
      : ''
  const showClear = allowClear === true && text.length > 0 && disabled !== true && (hovered || focused)

  const handleChange = (event: React.ChangeEvent<HTMLTextAreaElement>): void => {
    if (!externallyControlled && defaultValue !== undefined) setLocalText(event.target.value)
    onChange?.(event)
  }

  const handleClear = (): void => {
    if (!externallyControlled && defaultValue !== undefined) setLocalText('')
    onChange?.(buildClearEvent(''))
  }

  const sizeClass =
    size === 'small' ? styles.wrapperSmall : size === 'large' ? styles.wrapperLarge : styles.wrapperMiddle
  const statusClass =
    status === 'error' ? styles.wrapperError : status === 'warning' ? styles.wrapperWarning : null
  const wrapperProps = statusClass
    ? stylex.props(styles.wrapper, sizeClass, statusClass)
    : stylex.props(styles.wrapper, sizeClass)
  const mergedClass = [wrapperProps.className, className].filter(Boolean).join(' ') || undefined

  return (
    <div
      className={mergedClass}
      style={{ ...wrapperProps.style, ...style }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
      onFocus={() => setFocused(true)}
      onBlur={() => setFocused(false)}
    >
      <textarea
        {...rest}
        ref={ref}
        rows={rows}
        maxLength={maxLength}
        value={text}
        disabled={disabled}
        onChange={handleChange}
        className={stylex.props(styles.area).className}
      />
      {showClear && (
        <button
          type="button"
          aria-label="清空"
          tabIndex={-1}
          onMouseDown={(event) => event.preventDefault()}
          onClick={handleClear}
          {...stylex.props(styles.clearBtn)}
        >
          <CloseCircleOutlined />
        </button>
      )}
      {showCount && maxLength !== undefined && (
        <span {...stylex.props(styles.count)}>
          {text.length}/{maxLength}
        </span>
      )}
    </div>
  )
}

TextArea.displayName = 'TextArea'
