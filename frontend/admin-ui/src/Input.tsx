/**
 * Input(antd 形状,spec [S2.3]):
 * - onChange 透传事件对象(现网页面全部 `e.target.value` 取值);
 * - prefix / suffix / addonBefore / addonAfter / allowClear / size / status /
 *   onPressEnter 子集;
 * - 附带现网实际使用的 `Input.Search`(12 处)与 `Input.Password`(4 处);
 * - `Input.TextArea` 与具名 `TextArea` 均指向 ./Textarea 实现(避免循环引用:
 *   实现放 Textarea.tsx,此处仅 re-export 与挂载)。
 */
import React from 'react'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { CloseCircleOutlined, SearchOutlined } from './icons'
import { TextArea } from './Textarea'
import type { TextAreaProps } from './Textarea'

export { TextArea }
export type { TextAreaProps }

export interface InputProps
  extends Omit<React.InputHTMLAttributes<HTMLInputElement>, 'prefix' | 'suffix' | 'size'> {
  /** antd 同义:onChange 收到事件对象(取 e.target.value) */
  onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void
  prefix?: React.ReactNode
  suffix?: React.ReactNode
  /** antd 同名:右侧字数计数器 `${len}/${maxLength}`(现网多处 Input maxLength showCount) */
  showCount?: boolean
  addonBefore?: React.ReactNode
  addonAfter?: React.ReactNode
  /** 出现条件:allowClear 且有值(点击清空并触发 onChange({target:{value:''}})) */
  allowClear?: boolean
  size?: 'small' | 'middle' | 'large'
  status?: 'error' | 'warning'
  onPressEnter?: (event: React.KeyboardEvent<HTMLInputElement>) => void
  ref?: React.Ref<HTMLInputElement>
}

const styles = stylex.create({
  wrapper: {
    display: 'inline-flex',
    flexDirection: 'row',
    flexWrap: 'nowrap',
    alignItems: 'stretch',
    width: '100%',
    color: tokens.text,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    verticalAlign: 'middle',
  },
  addon: {
    display: 'flex',
    alignItems: 'center',
    flexShrink: '0',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '10px',
    paddingRight: '10px',
    backgroundColor: tokens.bg,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    color: tokens.textSub,
    fontSize: tokens.fsAux,
    whiteSpace: 'nowrap',
    borderTopLeftRadius: '0px',
    borderTopRightRadius: '0px',
    borderBottomRightRadius: '0px',
    borderBottomLeftRadius: '0px',
  },
  addonBeforeR: {
    borderTopLeftRadius: tokens.rSm,
    borderBottomLeftRadius: tokens.rSm,
  },
  addonAfterR: {
    borderTopRightRadius: tokens.rSm,
    borderBottomRightRadius: tokens.rSm,
  },
  box: {
    position: 'relative',
    display: 'flex',
    flexDirection: 'row',
    flexWrap: 'nowrap',
    alignItems: 'center',
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '10px',
    paddingRight: '10px',
    backgroundColor: tokens.card,
    borderStyle: 'solid',
    borderWidth: '1px',
    borderColor: tokens.borderStrong,
    borderTopLeftRadius: tokens.rSm,
    borderTopRightRadius: tokens.rSm,
    borderBottomRightRadius: tokens.rSm,
    borderBottomLeftRadius: tokens.rSm,
    transitionProperty: 'border-color, box-shadow',
    transitionDuration: '0.2s',
    transitionTimingFunction: 'ease',
    ':focus-within': {
      borderColor: tokens.brand,
      boxShadow: '0 0 0 2px rgba(58, 155, 217, 0.16)', // 品牌蓝 focus 环
    },
  },
  boxNoBefore: {
    borderTopLeftRadius: tokens.rSm,
    borderBottomLeftRadius: tokens.rSm,
  },
  boxWithBefore: {
    borderTopLeftRadius: '0px',
    borderBottomLeftRadius: '0px',
    borderLeftWidth: '0px',
  },
  boxWithAfter: {
    borderTopRightRadius: '0px',
    borderBottomRightRadius: '0px',
    borderRightWidth: '0px',
  },
  boxSmall: {
    minHeight: '24px',
    paddingLeft: '6px',
    paddingRight: '6px',
  },
  boxMiddle: {
    minHeight: '32px',
  },
  boxLarge: {
    minHeight: '40px',
    paddingLeft: '14px',
    paddingRight: '14px',
  },
  boxError: {
    borderColor: tokens.error,
  },
  boxWarning: {
    borderColor: tokens.warning,
  },
  input: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '0px',
    paddingRight: '0px',
    borderStyle: 'none',
    borderWidth: '0px',
    outlineStyle: 'none',
    backgroundColor: 'transparent',
    color: 'inherit',
    fontFamily: 'inherit',
    fontSize: 'inherit',
    lineHeight: 'inherit',
    '::placeholder': {
      color: tokens.textWeak,
    },
    ':disabled': {
      cursor: 'not-allowed',
      color: tokens.textWeak,
    },
  },
  count: {
    flexShrink: '0',
    // 设计 token 无计数器专用色,antd 次级文本灰(字面量 + 注释)
    color: 'rgba(0, 0, 0, 0.45)',
    fontSize: '14px',
    lineHeight: '22px',
    whiteSpace: 'nowrap',
    paddingLeft: '4px',
  },
  affix: {
    display: 'flex',
    alignItems: 'center',
    flexShrink: '0',
    color: tokens.textWeak,
    fontSize: '14px',
  },
  affixWithPrefix: {
    marginLeft: '6px',
  },
  clearBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: '0',
    width: '16px',
    height: '16px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '4px',
    marginRight: '0px',
    borderStyle: 'none',
    backgroundColor: 'transparent',
    color: tokens.textWeak,
    cursor: 'pointer',
    ':hover': {
      color: tokens.textSub,
    },
  },
  searchBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: '0',
    width: '28px',
    height: '26px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '4px',
    marginRight: '0px',
    borderStyle: 'none',
    backgroundColor: 'transparent',
    color: tokens.textSub,
    cursor: 'pointer',
    ':hover': {
      color: tokens.brand,
    },
  },
  enterButton: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: '0',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '12px',
    paddingRight: '12px',
    borderStyle: 'none',
    backgroundColor: tokens.brand,
    color: tokens.onBrand,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    cursor: 'pointer',
    ':hover': {
      backgroundColor: tokens.brandDark,
    },
  },
  eyeBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    flexShrink: '0',
    width: '20px',
    height: '20px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginTop: '0px',
    marginBottom: '0px',
    marginLeft: '4px',
    marginRight: '0px',
    borderStyle: 'none',
    backgroundColor: 'transparent',
    color: tokens.textWeak,
    cursor: 'pointer',
    ':hover': {
      color: tokens.textSub,
    },
  },
})

function buildClearEvent(next: string): React.ChangeEvent<HTMLInputElement> {
  return { target: { value: next } } as React.ChangeEvent<HTMLInputElement>
}

function InputImpl(props: InputProps): React.ReactElement {
  const {
    value,
    defaultValue,
    onChange,
    onKeyDown,
    onPressEnter,
    prefix,
    suffix,
    showCount = false,
    addonBefore,
    addonAfter,
    allowClear,
    size = 'middle',
    status,
    className,
    style,
    disabled,
    maxLength,
    ref,
    ...rest
  } = props

  // 恒传字符串 value → native input 永远受控,避免 uncontrolled 警告;
  // defaultValue 场景(现网 Input.Search 一处)走本地 state,页面不回写也可输入。
  const externallyControlled = value !== undefined && value !== null
  const [localText, setLocalText] = React.useState(() => String(defaultValue ?? ''))
  const [hovered, setHovered] = React.useState(false)
  const [focused, setFocused] = React.useState(false)

  const text = externallyControlled ? String(value) : defaultValue !== undefined ? localText : ''
  const showClear = allowClear === true && text.length > 0 && disabled !== true && (hovered || focused)

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    if (!externallyControlled && defaultValue !== undefined) setLocalText(event.target.value)
    onChange?.(event)
  }

  const handleClear = (): void => {
    if (!externallyControlled && defaultValue !== undefined) setLocalText('')
    onChange?.(buildClearEvent(''))
  }

  const handleKeyDown = (event: React.KeyboardEvent<HTMLInputElement>): void => {
    onKeyDown?.(event)
    if (event.key === 'Enter') onPressEnter?.(event)
  }

  const sizeClass = size === 'small' ? styles.boxSmall : size === 'large' ? styles.boxLarge : styles.boxMiddle
  const statusClass =
    status === 'error' ? styles.boxError : status === 'warning' ? styles.boxWarning : null
  const boxBase = addonBefore !== undefined ? styles.boxWithBefore : styles.boxNoBefore
  const boxProps = stylex.props(
    styles.box,
    boxBase,
    sizeClass,
    ...(statusClass ? [statusClass] : []),
    ...(addonAfter !== undefined ? [styles.boxWithAfter] : []),
  )
  const wrapperProps = stylex.props(styles.wrapper)
  const mergedClass = [wrapperProps.className, className].filter(Boolean).join(' ') || undefined

  const affixedPrefix =
    prefix !== undefined ? (
      <span {...stylex.props(styles.affix, styles.affixWithPrefix)}>{prefix}</span>
    ) : null

  const clearNode = showClear ? (
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
  ) : null

  return (
    <span
      className={mergedClass}
      style={{ ...wrapperProps.style, ...style }}
      onMouseEnter={() => setHovered(true)}
      onMouseLeave={() => setHovered(false)}
    >
      {addonBefore !== undefined && (
        <span {...stylex.props(styles.addon, styles.addonBeforeR)}>{addonBefore}</span>
      )}
      <span className={boxProps.className} style={boxProps.style} onFocus={() => setFocused(true)} onBlur={() => setFocused(false)}>
        {affixedPrefix}
        <input
          {...rest}
          ref={ref}
          value={text}
          disabled={disabled}
          maxLength={maxLength}
          onChange={handleChange}
          onKeyDown={handleKeyDown}
          className={stylex.props(styles.input).className}
        />
        {clearNode}
        {suffix !== undefined && <span {...stylex.props(styles.affix)}>{suffix}</span>}
        {showCount && (
          <span {...stylex.props(styles.count)}>
            {text.length}
            {maxLength !== undefined && maxLength !== null ? `/${maxLength}` : ''}
          </span>
        )}
      </span>
      {addonAfter !== undefined && (
        <span {...stylex.props(styles.addon, styles.addonAfterR)}>{addonAfter}</span>
      )}
    </span>
  )
}

// ---------------------------------------------------------------------------
// Input.Password(现网 4 处:AdminForm / DeliveryWorkerForm / ResetPasswordModal / login)
// ---------------------------------------------------------------------------

/** 简易「眼睛划线」内联图形:图标集无 EyeInvisible,仅密码可见态使用 */
function EyeInvisibleIcon(): React.ReactElement {
  return (
    <svg
      viewBox="0 0 24 24"
      width="1em"
      height="1em"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M2 12s3.6-6.5 10-6.5c1.6 0 3 .4 4.2 1M22 12s-3.6 6.5-10 6.5c-1.7 0-3.2-.4-4.4-1.1" />
      <path d="M9.9 9.9a3 3 0 0 0 4.2 4.2" />
      <path d="M3 3l18 18" />
    </svg>
  )
}

export interface InputPasswordProps extends Omit<InputProps, 'type' | 'onChange'> {
  /** antd 同义:可见态切换由组件内部管理 */
  visibilityToggle?: boolean
  onChange?: (event: React.ChangeEvent<HTMLInputElement>) => void
}

export function InputPassword(props: InputPasswordProps): React.ReactElement {
  const { visibilityToggle = true, suffix, ...rest } = props
  const [visible, setVisible] = React.useState(false)

  const eyeNode = visibilityToggle ? (
    <button
      type="button"
      aria-label={visible ? '隐藏密码' : '显示密码'}
      tabIndex={-1}
      onMouseDown={(event) => event.preventDefault()}
      onClick={() => setVisible((prev) => !prev)}
      {...stylex.props(styles.eyeBtn)}
    >
      {visible ? <EyeInvisibleIcon /> : <EyeOutlinedInline />}
    </button>
  ) : null

  return (
    <Input
      {...rest}
      type={visible ? 'text' : 'password'}
      suffix={
        suffix !== undefined ? (
          <>
            {suffix}
            {eyeNode}
          </>
        ) : (
          eyeNode
        )
      }
    />
  )
}

function EyeOutlinedInline(): React.ReactElement {
  return (
    <svg
      viewBox="0 0 24 24"
      width="1em"
      height="1em"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.8"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
      focusable="false"
    >
      <path d="M2 12s3.6-6.5 10-6.5S22 12 22 12s-3.6 6.5-10 6.5S2 12 2 12z" />
      <circle cx="12" cy="12" r="3" />
    </svg>
  )
}

// ---------------------------------------------------------------------------
// Input.Search(现网 12 处工具栏搜索:回车 / 图标 / enterButton 触发 onSearch)
// ---------------------------------------------------------------------------

export interface InputSearchProps extends Omit<InputProps, 'onPressEnter' | 'value' | 'defaultValue'> {
  value?: InputProps['value']
  defaultValue?: InputProps['defaultValue']
  /** 回车或点击搜索图标 / 按钮时回调(antd 语义,不是每次输入) */
  onSearch?: (value: string) => void
  /** true = 显式搜索按钮;字符串 = 自定义按钮文案 */
  enterButton?: boolean | React.ReactNode
  loading?: boolean
}

export function InputSearch(props: InputSearchProps): React.ReactElement {
  const { onSearch, enterButton, loading, value, defaultValue, onChange, ...rest } = props
  const externallyControlled = value !== undefined && value !== null
  const [localText, setLocalText] = React.useState(() => String(defaultValue ?? ''))
  const text = externallyControlled ? String(value) : defaultValue !== undefined ? localText : ''

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    if (!externallyControlled && defaultValue !== undefined) setLocalText(event.target.value)
    else if (!externallyControlled) setLocalText(event.target.value)
    onChange?.(event)
  }

  const fire = (): void => onSearch?.(text)
  const withEnter = (event: React.KeyboardEvent<HTMLInputElement>): void => {
    if (event.key === 'Enter') fire()
  }

  const buttonNode =
    enterButton !== undefined && enterButton !== false ? (
      <button type="button" onClick={fire} {...stylex.props(styles.enterButton)}>
        {typeof enterButton === 'string' ? enterButton : '搜索'}
      </button>
    ) : (
      <button type="button" aria-label="搜索" onClick={fire} {...stylex.props(styles.searchBtn)}>
        <SearchOutlined />
      </button>
    )

  const suffixNode = (
    <>
      {loading === true && <SearchOutlined spin />}
      {buttonNode}
    </>
  )

  return (
    <Input
      {...rest}
      value={value}
      defaultValue={defaultValue}
      onChange={handleChange}
      onPressEnter={withEnter}
      suffix={suffixNode}
      allowClear={rest.allowClear}
    />
  )
}

// ---------------------------------------------------------------------------
// 挂载 antd 同名静态成员:Input.TextArea / Input.Password / Input.Search
// ---------------------------------------------------------------------------

export const Input = Object.assign(InputImpl, {
  displayName: 'Input',
  TextArea,
  Password: InputPassword,
  Search: InputSearch,
})
