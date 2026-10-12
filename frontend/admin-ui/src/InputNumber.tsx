/**
 * InputNumber(antd 形状,spec [S2.3] / [S2.4]):
 * - value: number | null,onChange 回调裸值 `onChange(value: number | null)`,
 *   表单态里始终保持 number 类型(RHF 侧不做字符串化);
 * - min / max / step / precision / addonBefore / addonAfter 子集(现网 71 处,
 *   precision 57 处、addonAfter 10 处);
 * - min/max 不做输入钳制(与 antd 一致,越界交由 rules `{type:'number',min,max}`
 *   校验),步进按钮的结果会收敛到区间。
 */
import React from 'react'
import * as stylex from '@stylexjs/stylex'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { ArrowDownOutlined, ArrowUpOutlined } from './icons'

export interface InputNumberProps {
  value?: number | null
  onChange?: (value: number | null) => void
  min?: number
  max?: number
  step?: number
  /** 小数位:blur / 步进时按该位数收敛显示与提交值 */
  precision?: number
  placeholder?: string
  disabled?: boolean
  readOnly?: boolean
  addonBefore?: React.ReactNode
  addonAfter?: React.ReactNode
  name?: string
  id?: string
  className?: string
  style?: React.CSSProperties
  onBlur?: (event: React.FocusEvent<HTMLInputElement>) => void
  onFocus?: (event: React.FocusEvent<HTMLInputElement>) => void
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
    borderTopLeftRadius: '0px',
    borderTopRightRadius: '0px',
    borderBottomRightRadius: '0px',
    borderBottomLeftRadius: '0px',
    color: tokens.textSub,
    fontSize: tokens.fsAux,
    whiteSpace: 'nowrap',
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
    minHeight: '32px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '10px',
    paddingRight: '0px',
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
  input: {
    flexGrow: '1',
    flexShrink: '1',
    flexBasis: '0%',
    minWidth: '0px',
    paddingTop: '4px',
    paddingBottom: '4px',
    paddingLeft: '0px',
    paddingRight: '4px',
    borderStyle: 'none',
    borderWidth: '0px',
    outlineStyle: 'none',
    backgroundColor: 'transparent',
    color: tokens.text,
    fontFamily: tokens.fontNum, // 数字用等宽字族(设计规范 fontNum)
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    textAlign: 'right',
    ':disabled': {
      cursor: 'not-allowed',
      color: tokens.textWeak,
    },
    '::placeholder': {
      color: tokens.textWeak,
      fontFamily: tokens.font,
    },
  },
  stepGroup: {
    display: 'flex',
    flexDirection: 'column',
    flexShrink: '0',
    alignItems: 'stretch',
    justifyContent: 'center',
    width: '18px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '2px',
  },
  stepBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    height: '15px',
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
    fontSize: '10px',
    cursor: 'pointer',
    ':hover': {
      color: tokens.brand,
    },
    ':disabled': {
      color: tokens.textWeak,
      cursor: 'not-allowed',
    },
  },
})

function formatValue(value: number | null | undefined, precision?: number): string {
  if (value === null || value === undefined || Number.isNaN(value)) return ''
  if (precision !== undefined) return value.toFixed(precision)
  return String(value)
}

function roundTo(value: number, precision?: number): number {
  if (precision === undefined) return value
  const factor = 10 ** precision
  return Math.round(value * factor) / factor
}

export function InputNumber(props: InputNumberProps): React.ReactElement {
  const {
    value,
    onChange,
    min,
    max,
    step = 1,
    precision,
    placeholder,
    disabled,
    readOnly,
    addonBefore,
    addonAfter,
    className,
    style,
    onBlur,
    onFocus,
    onPressEnter,
    ref,
    name,
    id,
  } = props

  const controlled = value !== undefined
  const display = controlled ? formatValue(value ?? null, precision) : ''
  const [text, setText] = React.useState(() => display)
  // 非受控用法(页面只传 value 不回写)下维持本地编辑文本
  const [editing, setEditing] = React.useState(false)
  const current = editing && !controlled ? text : controlled && !editing ? display : text

  const emit = (next: number | null): void => onChange?.(next)

  const handleChange = (event: React.ChangeEvent<HTMLInputElement>): void => {
    const raw = event.target.value
    setEditing(true)
    setText(raw)
    if (raw.trim() === '') {
      emit(null)
      return
    }
    const parsed = Number(raw)
    if (!Number.isNaN(parsed)) emit(parsed)
  }

  const commit = (): void => {
    const raw = text
    setEditing(false)
    if (raw.trim() === '') {
      setText('')
      if (controlled) emit(null)
      return
    }
    const parsed = Number(raw)
    if (Number.isNaN(parsed)) {
      // 非法输入回退到当前值
      setText(formatValue(controlled ? (value ?? null) : null, precision))
      return
    }
    const rounded = roundTo(parsed, precision)
    setText(formatValue(rounded, precision))
    emit(rounded)
  }

  const stepBy = (direction: 1 | -1): void => {
    if (disabled === true || readOnly === true) return
    const base = controlled && value !== null && value !== undefined ? value : Number(text || 0)
    let next = base + direction * step
    if (min !== undefined && next < min) next = min
    if (max !== undefined && next > max) next = max
    next = roundTo(next, precision)
    setEditing(false)
    setText(formatValue(next, precision))
    emit(next)
  }

  const sizeBoxBase = addonBefore !== undefined ? styles.boxWithBefore : styles.boxNoBefore
  const boxProps = stylex.props(
    styles.box,
    sizeBoxBase,
    ...(addonAfter !== undefined ? [styles.boxWithAfter] : []),
  )
  const wrapperProps = stylex.props(styles.wrapper)
  const mergedClass = [wrapperProps.className, className].filter(Boolean).join(' ') || undefined

  return (
    <span className={mergedClass} style={{ ...wrapperProps.style, ...style }}>
      {addonBefore !== undefined && (
        <span {...stylex.props(styles.addon, styles.addonBeforeR)}>{addonBefore}</span>
      )}
      <span className={boxProps.className} style={boxProps.style}>
        <input
          ref={ref}
          name={name}
          id={id}
          type="text"
          inputMode="decimal"
          autoComplete="off"
          value={current}
          placeholder={placeholder}
          disabled={disabled}
          readOnly={readOnly}
          onChange={handleChange}
          onBlur={(event) => {
            commit()
            onBlur?.(event)
          }}
          onFocus={(event) => {
            setEditing(true)
            setText(controlled ? formatValue(value ?? null, precision) : text)
            onFocus?.(event)
          }}
          onKeyDown={(event) => {
            if (event.key === 'Enter') onPressEnter?.(event)
            if (event.key === 'ArrowUp') {
              event.preventDefault()
              stepBy(1)
            }
            if (event.key === 'ArrowDown') {
              event.preventDefault()
              stepBy(-1)
            }
          }}
          className={stylex.props(styles.input).className}
        />
        {readOnly !== true && (
          <span {...stylex.props(styles.stepGroup)}>
            <button
              type="button"
              tabIndex={-1}
              aria-label="增加"
              disabled={disabled === true || (max !== undefined && (value ?? 0) >= max)}
              onClick={() => stepBy(1)}
              {...stylex.props(styles.stepBtn)}
            >
              <ArrowUpOutlined />
            </button>
            <button
              type="button"
              tabIndex={-1}
              aria-label="减少"
              disabled={disabled === true || (min !== undefined && (value ?? 0) <= min)}
              onClick={() => stepBy(-1)}
              {...stylex.props(styles.stepBtn)}
            >
              <ArrowDownOutlined />
            </button>
          </span>
        )}
      </span>
      {addonAfter !== undefined && (
        <span {...stylex.props(styles.addon, styles.addonAfterR)}>{addonAfter}</span>
      )}
    </span>
  )
}

InputNumber.displayName = 'InputNumber'
