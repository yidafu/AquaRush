/**
 * DatePicker —— Radix Popover + react-day-picker 单日/区间选择器,值域 dayjs。
 * 契约:docs/compose/spec/admin-radix-ui.md [S2.7]。
 *
 * 本文件同时承载共享件(避免与 TimePicker 循环引用):
 * - PickerField:单值输入框外观触发器(日历/时钟图标 + 允许清空 + 可键入回验);
 * - RangePickerField:区间触发器(开始/结束两个输入框,DatePicker/TimePicker 共用);
 * - PickerFooter:面板底栏(今天/清空/确定);
 * - TimePanel:时/分/秒列面板(showTime 与 TimePicker 共用);
 * - RangeTimePanels:开始/结束双时间列(showTime 区间与 TimePicker 区间共用)。
 *
 * RangePicker 以 antd 兼容静态挂载:`DatePicker.RangePicker` / 解构 `const { RangePicker } = DatePicker`。
 */
import { useEffect, useState } from 'react'
import type { CSSProperties, ReactNode } from 'react'
import * as Popover from '@radix-ui/react-popover'
import * as stylex from '@stylexjs/stylex'
import type { StyleXStyles } from '@stylexjs/stylex'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { DayPicker } from 'react-day-picker'
import type { ClassNames, DateRange } from 'react-day-picker'
import { zhCN } from 'react-day-picker/locale/zh-CN'
import { tokens } from '@aquarush/common/theme/tokens.stylex'
import { CalendarOutlined, ClockCircleOutlined, CloseCircleOutlined } from './icons'
import {
  evaluateDisabled,
  formatDayjs,
  formatRange,
  mergeTime,
  normalizeRange,
  orderRange,
  parseInput,
} from './picker-core'
import type { DisabledDateFn, RangeDateStrings, RangeValue } from './picker-core'

export type PickerSize = 'small' | 'middle' | 'large'

const cn = (style: StyleXStyles): string => stylex.props(style).className ?? ''

const styles = stylex.create({
  // —— 触发器(输入框外观) ——
  field: {
    display: 'inline-flex',
    alignItems: 'center',
    boxSizing: 'border-box',
    maxWidth: '100%',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '8px',
    paddingRight: '8px',
    borderStyle: 'solid',
    borderWidth: '1px',
    borderRadius: tokens.rSm,
    ':focus-within': {
      borderColor: tokens.brand,
    },
  },
  fieldIdle: {
    backgroundColor: tokens.card,
    borderColor: tokens.borderStrong,
    color: tokens.text,
  },
  fieldDisabled: {
    backgroundColor: tokens.bg,
    borderColor: tokens.border,
    color: tokens.textWeak,
    cursor: 'not-allowed',
  },
  sizeSmall: {
    height: '24px',
  },
  sizeMiddle: {
    height: '32px',
  },
  sizeLarge: {
    height: '40px',
  },
  input: {
    alignSelf: 'stretch',
    flexGrow: '1',
    minWidth: '0',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    marginRight: '8px',
    borderWidth: '0',
    backgroundColor: 'transparent',
    color: 'inherit',
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    outlineStyle: 'none',
    cursor: 'text',
    '::placeholder': {
      color: tokens.textWeak,
    },
  },
  inputDisabled: {
    cursor: 'not-allowed',
  },
  clearBtn: {
    display: 'inline-flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '16px',
    height: '16px',
    marginRight: '4px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    backgroundColor: 'transparent',
    borderWidth: '0',
    color: tokens.textWeak,
    cursor: 'pointer',
    ':hover': {
      color: tokens.text,
    },
  },
  clearIcon: {
    fontSize: '14px',
  },
  trailingIcon: {
    display: 'inline-flex',
    alignItems: 'center',
    flexShrink: '0',
    color: tokens.textWeak,
    fontSize: '14px',
  },
  // —— 区间触发器 ——
  rangeSep: {
    flexShrink: '0',
    alignSelf: 'center',
    marginRight: '8px',
    color: tokens.textWeak,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    userSelect: 'none',
  },

  // —— react-day-picker 外观(经 classNames 映射,不引入 daypicker CSS) ——
  rdpRoot: {
    width: '288px',
    color: tokens.text,
    fontFamily: tokens.font,
  },
  rdpMonths: {
    position: 'relative',
    width: '100%',
  },
  rdpNav: {
    position: 'absolute',
    top: '0px',
    left: '0px',
    right: '0px',
    height: '40px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    zIndex: 1,
  },
  rdpNavBtn: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '40px',
    height: '40px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    backgroundColor: 'transparent',
    borderWidth: '0',
    borderRadius: tokens.rSm,
    color: tokens.textSub,
    cursor: 'pointer',
    ':hover': {
      backgroundColor: tokens.brandTint,
      color: tokens.brand,
    },
  },
  rdpChevron: {
    width: '16px',
    height: '16px',
    fill: 'currentColor',
  },
  rdpMonth: {
    width: '100%',
  },
  rdpCaption: {
    height: '40px',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
  },
  rdpCaptionLabel: {
    fontSize: tokens.fsBody,
    lineHeight: tokens.lhBody,
    fontWeight: '600',
    color: tokens.text,
  },
  rdpGrid: {
    width: '100%',
    borderSpacing: '0',
    tableLayout: 'fixed',
  },
  rdpWeekday: {
    boxSizing: 'border-box',
    height: '32px',
    paddingTop: '0px',
    paddingBottom: '4px',
    fontSize: tokens.fsNote,
    lineHeight: tokens.lhNote,
    fontWeight: '600',
    color: tokens.textSub,
    textAlign: 'center',
  },
  rdpDay: {
    boxSizing: 'border-box',
    width: '41px',
    height: '40px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    textAlign: 'center',
    verticalAlign: 'middle',
    borderRadius: tokens.rSm,
  },
  rdpDayButton: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    width: '100%',
    height: '100%',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    backgroundColor: 'transparent',
    borderWidth: '0',
    borderRadius: tokens.rSm,
    color: 'inherit',
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    cursor: 'pointer',
    ':hover': {
      backgroundColor: tokens.brandTint,
    },
  },
  rdpSelected: {
    backgroundColor: tokens.brand,
    color: tokens.onBrand,
  },
  rdpToday: {
    outlineColor: tokens.brand,
    outlineStyle: 'solid',
    outlineWidth: '1px',
    outlineOffset: '-1px',
  },
  rdpDisabled: {
    color: tokens.textWeak,
    cursor: 'not-allowed',
  },
  rdpOutside: {
    color: tokens.textWeak,
  },

  // —— 时间列面板 ——
  timeRow: {
    display: 'flex',
    justifyContent: 'center',
    marginTop: '8px',
    marginBottom: '8px',
  },
  timeCol: {
    display: 'flex',
    flexDirection: 'column',
    width: '72px',
    height: '200px',
    overflowY: 'auto',
    overflowX: 'hidden',
  },
  timeColDivider: {
    borderLeftStyle: 'solid',
    borderLeftWidth: '1px',
    borderLeftColor: tokens.border,
  },
  timeItem: {
    flexShrink: '0',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    height: '32px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '0px',
    paddingRight: '0px',
    backgroundColor: 'transparent',
    borderWidth: '0',
    borderRadius: tokens.rSm,
    color: tokens.text,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    cursor: 'pointer',
    ':hover': {
      backgroundColor: tokens.brandTint,
    },
  },
  timeItemActive: {
    backgroundColor: tokens.brand,
    color: tokens.onBrand,
    ':hover': {
      backgroundColor: tokens.brand,
    },
  },
  // —— 区间时间列的「开始时间/结束时间」小标题 ——
  rangeTimeLabel: {
    marginTop: '4px',
    marginBottom: '0px',
    fontFamily: tokens.font,
    fontSize: tokens.fsNote,
    lineHeight: tokens.lhNote,
    fontWeight: '600',
    color: tokens.textSub,
    textAlign: 'left',
  },

  // —— 面板底栏 ——
  footer: {
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginTop: '8px',
    paddingTop: '8px',
    borderTopStyle: 'solid',
    borderTopWidth: '1px',
    borderTopColor: tokens.border,
  },
  footerGroup: {
    display: 'flex',
    alignItems: 'center',
  },
  footerBtn: {
    height: '28px',
    marginRight: '4px',
    paddingTop: '0px',
    paddingBottom: '0px',
    paddingLeft: '8px',
    paddingRight: '8px',
    backgroundColor: 'transparent',
    borderWidth: '0',
    borderRadius: tokens.rSm,
    color: tokens.brand,
    fontFamily: tokens.font,
    fontSize: tokens.fsAux,
    lineHeight: tokens.lhAux,
    cursor: 'pointer',
    ':hover': {
      backgroundColor: tokens.brandTint,
    },
  },
})

/** 面板容器样式(DatePicker / TimePicker 共用)。 */
export const pickerPanelStyle = stylex.create({
  panel: {
    boxSizing: 'border-box',
    paddingTop: '8px',
    paddingBottom: '8px',
    paddingLeft: '8px',
    paddingRight: '8px',
    backgroundColor: tokens.card,
    borderRadius: tokens.rCard,
    boxShadow: tokens.shModal,
    fontFamily: tokens.font,
    color: tokens.text,
    zIndex: 1100,
  },
})

const zhLabels = {
  labelPrevious: () => '上一月',
  labelNext: () => '下一月',
}

const dayPickerClassNames: Partial<ClassNames> = {
  root: cn(styles.rdpRoot),
  months: cn(styles.rdpMonths),
  nav: cn(styles.rdpNav),
  button_previous: cn(styles.rdpNavBtn),
  button_next: cn(styles.rdpNavBtn),
  chevron: cn(styles.rdpChevron),
  month: cn(styles.rdpMonth),
  month_caption: cn(styles.rdpCaption),
  caption_label: cn(styles.rdpCaptionLabel),
  month_grid: cn(styles.rdpGrid),
  weekday: cn(styles.rdpWeekday),
  day: cn(styles.rdpDay),
  day_button: cn(styles.rdpDayButton),
  selected: cn(styles.rdpSelected),
  today: cn(styles.rdpToday),
  disabled: cn(styles.rdpDisabled),
  outside: cn(styles.rdpOutside),
}

export interface PickerFieldProps {
  value: Dayjs | null
  format: string
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
  size?: PickerSize
  /** 尾部图标:日期用日历,时间用时钟 */
  icon: 'calendar' | 'clock'
  /** input size 属性,决定基础宽度(配合 flexGrow 适配满宽容器) */
  inputSize: number
  id?: string
  className?: string
  style?: CSSProperties
  onClear: () => void
  /** 输入框失焦/回车提交:解析成功给日期,整串清空给 null,非法文本静默回退 */
  onCommit: (date: Dayjs | null) => void
}

/** 输入框外观触发器(置于 Popover.Root 内,由 Radix Trigger 承载点击开合)。 */
export function PickerField({
  value,
  format,
  placeholder,
  disabled = false,
  allowClear,
  size = 'middle',
  icon,
  inputSize,
  id,
  className,
  style,
  onClear,
  onCommit,
}: PickerFieldProps) {
  const [draft, setDraft] = useState<string | null>(null)
  // 外部 value 变化(表单回填/面板选中/清空)时丢弃未提交草稿
  useEffect(() => {
    setDraft(null)
  }, [value])

  const text = draft ?? formatDayjs(value, format)
  const showClear = Boolean(allowClear && value && !disabled)

  const commitDraft = (): void => {
    if (draft === null) return
    const raw = draft
    setDraft(null)
    if (raw.trim() === '') {
      onCommit(null)
      return
    }
    const parsed = parseInput(raw, format)
    if (parsed !== null) onCommit(parsed)
  }

  const sizeStyle =
    size === 'small' ? styles.sizeSmall : size === 'large' ? styles.sizeLarge : styles.sizeMiddle
  const sp = stylex.props(
    styles.field,
    sizeStyle,
    disabled ? styles.fieldDisabled : styles.fieldIdle,
  )
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style

  return (
    <Popover.Trigger asChild>
      <div {...sp} className={mergedClassName} style={mergedStyle}>
        <input
          {...stylex.props(styles.input, disabled && styles.inputDisabled)}
          id={id}
          disabled={disabled}
          size={inputSize}
          value={text}
          placeholder={placeholder}
          onChange={(event) => setDraft(event.target.value)}
          onBlur={commitDraft}
          onKeyDown={(event) => {
            if (event.key === 'Enter') commitDraft()
          }}
        />
        {showClear && (
          <span
            {...stylex.props(styles.clearBtn)}
            role="button"
            tabIndex={0}
            aria-label="清空"
            onClick={(event) => {
              event.stopPropagation()
              setDraft(null)
              onClear()
            }}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                setDraft(null)
                onClear()
              }
            }}
          >
            <CloseCircleOutlined {...stylex.props(styles.clearIcon)} />
          </span>
        )}
        {icon === 'calendar' ? (
          <CalendarOutlined {...stylex.props(styles.trailingIcon)} />
        ) : (
          <ClockCircleOutlined {...stylex.props(styles.trailingIcon)} />
        )}
      </div>
    </Popover.Trigger>
  )
}

export interface RangePickerFieldProps {
  value: RangeValue
  format: string
  placeholder?: [string, string] | undefined
  disabled?: boolean
  allowClear?: boolean
  size?: PickerSize
  /** 尾部图标:日期区间用日历,时间区间用时钟 */
  icon: 'calendar' | 'clock'
  /** 两侧 input 的 size 属性(等宽),配合 flexGrow 适配满宽容器 */
  inputSize: number
  id?: string
  className?: string
  style?: CSSProperties
  onClear: () => void
  /** 输入框失焦/回车提交某一侧:解析成功给日期,整串清空给 null,非法文本静默回退 */
  onCommitSide: (index: 0 | 1, date: Dayjs | null) => void
}

/** 区间触发器:开始/结束两个输入框 + 「~」分隔,置于 Popover.Root 内。 */
export function RangePickerField({
  value,
  format,
  placeholder,
  disabled = false,
  allowClear,
  size = 'middle',
  icon,
  inputSize,
  id,
  className,
  style,
  onClear,
  onCommitSide,
}: RangePickerFieldProps) {
  const [drafts, setDrafts] = useState<[string | null, string | null]>([null, null])
  // 草稿生命周期:仅在输入框聚焦期间存在;失焦(回车)即提交或回退。
  // 任何外部 value 变化(面板提交/清空/回填)之前必先触发 blur,故无需 effect 重置。

  const setDraftAt = (index: 0 | 1, text: string | null): void => {
    setDrafts((prev) => (index === 0 ? [text, prev[1]] : [prev[0], text]))
  }

  const commitSide = (index: 0 | 1): void => {
    const raw = drafts[index]
    if (raw === null) return
    setDraftAt(index, null)
    if (raw.trim() === '') {
      onCommitSide(index, null)
      return
    }
    const parsed = parseInput(raw, format)
    if (parsed !== null) onCommitSide(index, parsed)
  }

  const hasValue = value !== null && (value[0] !== null || value[1] !== null)
  const showClear = Boolean(allowClear && hasValue && !disabled)

  const sizeStyle =
    size === 'small' ? styles.sizeSmall : size === 'large' ? styles.sizeLarge : styles.sizeMiddle
  const sp = stylex.props(
    styles.field,
    sizeStyle,
    disabled ? styles.fieldDisabled : styles.fieldIdle,
  )
  const mergedClassName = [sp.className, className].filter(Boolean).join(' ') || undefined
  const mergedStyle = sp.style ? { ...sp.style, ...style } : style

  const sideInput = (index: 0 | 1): ReactNode => (
    <input
      {...stylex.props(styles.input, disabled && styles.inputDisabled)}
      id={index === 0 ? id : undefined}
      disabled={disabled}
      size={inputSize}
      value={drafts[index] ?? formatDayjs(value === null ? null : value[index], format)}
      placeholder={placeholder?.[index]}
      onChange={(event) => setDraftAt(index, event.target.value)}
      onBlur={() => commitSide(index)}
      onKeyDown={(event) => {
        if (event.key === 'Enter') commitSide(index)
      }}
    />
  )

  return (
    <Popover.Trigger asChild>
      <div {...sp} className={mergedClassName} style={mergedStyle}>
        {sideInput(0)}
        <span {...stylex.props(styles.rangeSep)} aria-hidden="true">
          ~
        </span>
        {sideInput(1)}
        {showClear && (
          <span
            {...stylex.props(styles.clearBtn)}
            role="button"
            tabIndex={0}
            aria-label="清空"
            onClick={(event) => {
              event.stopPropagation()
              setDrafts([null, null])
              onClear()
            }}
            onKeyDown={(event) => {
              if (event.key === 'Enter' || event.key === ' ') {
                event.preventDefault()
                setDrafts([null, null])
                onClear()
              }
            }}
          >
            <CloseCircleOutlined {...stylex.props(styles.clearIcon)} />
          </span>
        )}
        {icon === 'calendar' ? (
          <CalendarOutlined {...stylex.props(styles.trailingIcon)} />
        ) : (
          <ClockCircleOutlined {...stylex.props(styles.trailingIcon)} />
        )}
      </div>
    </Popover.Trigger>
  )
}

export interface PickerFooterProps {
  /** 「今天」快捷选中;时间面板不传则不渲染该按钮 */
  onToday?: (() => void) | undefined
  onClear: () => void
  onConfirm: () => void
}

/** 面板底栏:今天 / 清空 / 确定。 */
export function PickerFooter({ onToday, onClear, onConfirm }: PickerFooterProps) {
  return (
    <div {...stylex.props(styles.footer)}>
      <div {...stylex.props(styles.footerGroup)}>
        {onToday !== undefined && (
          <button type="button" {...stylex.props(styles.footerBtn)} onClick={onToday}>
            今天
          </button>
        )}
        <button type="button" {...stylex.props(styles.footerBtn)} onClick={onClear}>
          清空
        </button>
      </div>
      <button type="button" {...stylex.props(styles.footerBtn)} onClick={onConfirm}>
        确定
      </button>
    </div>
  )
}

export interface TimeSteps {
  hour?: number
  minute?: number
  second?: number
}

export interface TimePatch {
  hour?: number
  minute?: number
  second?: number
}

export interface TimePanelProps {
  showHour: boolean
  showMinute: boolean
  showSecond: boolean
  hour?: number
  minute?: number
  second?: number
  steps?: TimeSteps
  onPick: (patch: TimePatch) => void
}

/** 时/分/秒列面板(antd 风格),点选即通过 onPick 上报增量。 */
export function TimePanel({
  showHour,
  showMinute,
  showSecond,
  hour,
  minute,
  second,
  steps,
  onPick,
}: TimePanelProps) {
  const stepOf = (raw: number | undefined): number =>
    raw !== undefined && raw >= 1 ? Math.floor(raw) : 1
  const values = (count: number, step: number): number[] =>
    Array.from({ length: Math.floor((count - 1) / step) + 1 }, (_, index) => index * step)

  const column = (
    list: number[],
    active: number | undefined,
    unit: 'hour' | 'minute' | 'second',
    divider: boolean,
  ) => (
    <div {...stylex.props(styles.timeCol, divider && styles.timeColDivider)}>
      {list.map((item) => (
        <button
          key={item}
          type="button"
          aria-pressed={active === item}
          {...stylex.props(styles.timeItem, active === item && styles.timeItemActive)}
          onClick={() => {
            if (unit === 'hour') onPick({ hour: item })
            else if (unit === 'minute') onPick({ minute: item })
            else onPick({ second: item })
          }}
        >
          {String(item).padStart(2, '0')}
        </button>
      ))}
    </div>
  )

  return (
    <div {...stylex.props(styles.timeRow)}>
      {showHour && column(values(24, stepOf(steps?.hour)), hour, 'hour', false)}
      {showMinute && column(values(60, stepOf(steps?.minute)), minute, 'minute', showHour)}
      {showSecond &&
        column(values(60, stepOf(steps?.second)), second, 'second', showHour || showMinute)}
    </div>
  )
}

export interface RangeTimePanelsProps {
  showHour: boolean
  showMinute: boolean
  showSecond: boolean
  value: RangeValue
  steps?: TimeSteps | undefined
  /** 更新某一侧的时间(0 = 开始,1 = 结束),仅暂存不提交 */
  onPick: (index: 0 | 1, patch: TimePatch) => void
}

/**
 * 开始/结束双时间列(showTime 区间与 TimePicker 区间共用):
 * 两侧各一组 TimePanel,带「开始时间/结束时间」小标题。
 */
export function RangeTimePanels({
  showHour,
  showMinute,
  showSecond,
  value,
  steps,
  onPick,
}: RangeTimePanelsProps) {
  const start = value?.[0] ?? null
  const end = value?.[1] ?? null
  return (
    <>
      <div {...stylex.props(styles.rangeTimeLabel)}>开始时间</div>
      <TimePanel
        showHour={showHour}
        showMinute={showMinute}
        showSecond={showSecond}
        hour={start?.hour()}
        minute={start?.minute()}
        second={start?.second()}
        steps={steps}
        onPick={(patch) => onPick(0, patch)}
      />
      <div {...stylex.props(styles.rangeTimeLabel)}>结束时间</div>
      <TimePanel
        showHour={showHour}
        showMinute={showMinute}
        showSecond={showSecond}
        hour={end?.hour()}
        minute={end?.minute()}
        second={end?.second()}
        steps={steps}
        onPick={(patch) => onPick(1, patch)}
      />
    </>
  )
}

export interface DatePickerProps {
  value?: Dayjs | null
  onChange?: (date: Dayjs | null, dateString: string) => void
  format?: string
  placeholder?: string
  disabled?: boolean
  disabledDate?: DisabledDateFn
  allowClear?: boolean
  showTime?: boolean
  size?: PickerSize
  /** 受控开合(调用点未传时走内部状态) */
  open?: boolean
  onOpenChange?: (open: boolean) => void
  id?: string
  className?: string
  style?: CSSProperties
}

export function DatePicker({
  value = null,
  onChange,
  format,
  placeholder,
  disabled = false,
  disabledDate,
  allowClear,
  showTime = false,
  size = 'middle',
  open,
  onOpenChange,
  id,
  className,
  style,
}: DatePickerProps) {
  // showTime 且未显式给 format 时对齐 antd:默认带时间
  const fmt = format ?? (showTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
  const [uncontrolledOpen, setUncontrolledOpen] = useState(false)
  const [month, setMonth] = useState<Date>(() => (value ?? dayjs()).startOf('month').toDate())
  const isOpen = open ?? uncontrolledOpen

  const setOpen = (next: boolean): void => {
    if (disabled && next) return
    if (open === undefined) setUncontrolledOpen(next)
    onOpenChange?.(next)
  }

  const emit = (next: Dayjs | null): void => {
    onChange?.(next, next === null ? '' : formatDayjs(next, fmt))
  }

  /** 选中某天;closeAfter 用于「面板点选后收起」(showTime 时需保留面板改时间)。 */
  const commitDay = (day: Dayjs, closeAfter: boolean): void => {
    const next = showTime ? mergeTime(day, value ?? dayjs().startOf('day')) : day.startOf('day')
    emit(next)
    if (closeAfter && !showTime) setOpen(false)
  }

  const handleClear = (): void => {
    emit(null)
  }

  const handleTimePick = (patch: TimePatch): void => {
    const base = value ?? dayjs().startOf('day')
    const timePart = base
      .hour(patch.hour ?? base.hour())
      .minute(patch.minute ?? base.minute())
      .second(patch.second ?? base.second())
      .millisecond(0)
    emit(mergeTime(base, timePart))
  }

  return (
    <Popover.Root open={isOpen} onOpenChange={setOpen}>
      <PickerField
        value={value}
        format={fmt}
        placeholder={placeholder ?? '选择日期'}
        disabled={disabled}
        allowClear={allowClear}
        size={size}
        icon="calendar"
        inputSize={12}
        id={id}
        className={className}
        style={style}
        onClear={handleClear}
        onCommit={(date) => {
          if (date === null) emit(null)
          else commitDay(date, false)
        }}
      />
      <Popover.Portal>
        <Popover.Content
          align="start"
          sideOffset={8}
          onOpenAutoFocus={(event) => event.preventDefault()}
          {...stylex.props(pickerPanelStyle.panel)}
        >
          <DayPicker
            mode="single"
            required
            locale={zhCN}
            month={month}
            onMonthChange={setMonth}
            selected={value ? value.toDate() : undefined}
            onSelect={(selected) => commitDay(dayjs(selected), true)}
            disabled={(date) => evaluateDisabled(dayjs(date), disabledDate)}
            showOutsideDays
            labels={zhLabels}
            formatters={{
              formatCaption: (date) => dayjs(date).format('YYYY年M月'),
            }}
            classNames={dayPickerClassNames}
          />
          {showTime && (
            <TimePanel
              showHour={fmt.includes('H')}
              showMinute={fmt.includes('m')}
              showSecond={fmt.includes('s')}
              hour={value?.hour()}
              minute={value?.minute()}
              second={value?.second()}
              onPick={handleTimePick}
            />
          )}
          <PickerFooter
            onToday={() => commitDay(dayjs(), true)}
            onClear={handleClear}
            onConfirm={() => setOpen(false)}
          />
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  )
}

/** react-day-picker 受控区间 → {from,to};无起始侧(含孤立结束侧)不进日历选择态。 */
function toRdpRange(range: RangeValue): DateRange | undefined {
  if (range === null || range[0] === null) return undefined
  return { from: range[0].toDate(), to: range[1] === null ? undefined : range[1].toDate() }
}

export interface RangePickerProps {
  value?: RangeValue
  onChange?: (dates: RangeValue, dateStrings: RangeDateStrings) => void
  format?: string
  showTime?: boolean
  placeholder?: [string, string]
  disabled?: boolean
  disabledDate?: DisabledDateFn
  allowClear?: boolean
  size?: PickerSize
  /** 受控开合(调用点未传时走内部状态) */
  open?: boolean
  onOpenChange?: (open: boolean) => void
  id?: string
  className?: string
  style?: CSSProperties
}

/**
 * RangePicker —— 区间日期选择器(antd 同构,值域 [开始, 结束],两侧可 null):
 * - 提交时机:date-only 选中结束日即提交并收起;showTime 仅「确定」提交;
 *   输入框失焦补全两端即提交;「清空」立即提交 null;
 * - 载荷锁序:提交时 end 早于 start 则交换(「开始 ≤ 结束」归一);
 *   起始侧为空 → null(孤立结束侧不入表单,规避 [null,end] 类校验崩溃);
 * - 「确定」仅在两端齐全或全空时提交,半区间只收起;
 * - 面板点选期间值只进 pending 草稿,打开时以受控值重置。
 */
export function RangePicker({
  value = null,
  onChange,
  format,
  showTime = false,
  placeholder = ['开始日期', '结束日期'],
  disabled = false,
  disabledDate,
  allowClear = true,
  size = 'middle',
  open,
  onOpenChange,
  id,
  className,
  style,
}: RangePickerProps) {
  // showTime 且未显式给 format 时对齐 antd:默认带时间
  const fmt = format ?? (showTime ? 'YYYY-MM-DD HH:mm:ss' : 'YYYY-MM-DD')
  const committed = normalizeRange(value)
  const [pending, setPending] = useState<RangeValue>(committed)
  const [uncontrolledOpen, setUncontrolledOpen] = useState(false)
  const [month, setMonth] = useState<Date>(
    () => (committed?.[0] ?? dayjs()).startOf('month').toDate(),
  )
  const isOpen = open ?? uncontrolledOpen

  const setOpen = (next: boolean): void => {
    if (disabled && next) return
    // 打开即以受控值为面板草稿(丢弃上次未提交的选择);事件时机内同步,无 effect
    if (next) setPending(normalizeRange(value))
    if (open === undefined) setUncontrolledOpen(next)
    onOpenChange?.(next)
  }

  /** onChange 载荷:起始侧为空 → null(丢弃孤立结束侧);否则锁序后成对提交。 */
  const emitRange = (next: RangeValue): void => {
    const payload = next !== null && next[0] !== null ? orderRange(next) : null
    onChange?.(payload, formatRange(payload, fmt))
  }

  /** 「确定」:两端齐全或全空才提交,半区间仅收起(防 [start,null] 流入表单校验)。 */
  const handleConfirm = (): void => {
    const [start, end]: [Dayjs | null, Dayjs | null] = pending ?? [null, null]
    if (start !== null && end !== null) emitRange(pending)
    else if (start === null && end === null && committed !== null) emitRange(null)
    setOpen(false)
  }

  const handleClear = (): void => {
    setPending(null)
    emitRange(null)
  }

  const handleDaySelect = (selected: DateRange | undefined): void => {
    const next: RangeValue =
      selected?.from !== undefined
        ? [dayjs(selected.from), selected.to !== undefined ? dayjs(selected.to) : null]
        : null
    setPending(next)
    if (showTime) return // showTime:面板只暂存,「确定」统一提交
    if (next === null) {
      // rdp 清除选择(单日区间再点一次)视同清空
      emitRange(null)
      setOpen(false)
      return
    }
    const [start, end] = next
    if (start !== null && end !== null) {
      // date-only:选中结束日即提交并收起(antd 行为;rdp 已对「后选早于先选」交换)
      emitRange(next)
      setOpen(false)
    }
  }

  const handleSideCommit = (index: 0 | 1, date: Dayjs | null): void => {
    const cur: [Dayjs | null, Dayjs | null] = pending ?? [null, null]
    const next: [Dayjs | null, Dayjs | null] =
      index === 0 ? [date, cur[1]] : [cur[0], date]
    setPending(next)
    const [start, end] = next
    if (start !== null && end !== null) emitRange(next)
    else if (start === null && end === null) emitRange(null)
    // 半区间仅暂存,由「确定」决定是否提交
  }

  const handleTimePick = (index: 0 | 1, patch: TimePatch): void => {
    const base = pending === null ? null : pending[index]
    if (base === null) return // 未选该侧日期前不接受时间点选(先选日期再调时间)
    const timePart = base
      .hour(patch.hour ?? base.hour())
      .minute(patch.minute ?? base.minute())
      .second(patch.second ?? base.second())
      .millisecond(0)
    const next: RangeValue =
      index === 0 ? [timePart, pending?.[1] ?? null] : [pending?.[0] ?? null, timePart]
    setPending(next)
  }

  return (
    <Popover.Root open={isOpen} onOpenChange={setOpen}>
      <RangePickerField
        value={isOpen ? pending : committed}
        format={fmt}
        placeholder={placeholder}
        disabled={disabled}
        allowClear={allowClear}
        size={size}
        icon="calendar"
        inputSize={Math.min(fmt.length, 19)}
        id={id}
        className={className}
        style={style}
        onClear={handleClear}
        onCommitSide={handleSideCommit}
      />
      <Popover.Portal>
        <Popover.Content
          align="start"
          sideOffset={8}
          onOpenAutoFocus={(event) => event.preventDefault()}
          {...stylex.props(pickerPanelStyle.panel)}
        >
          <DayPicker
            mode="range"
            resetOnSelect
            locale={zhCN}
            month={month}
            onMonthChange={setMonth}
            selected={toRdpRange(pending)}
            onSelect={handleDaySelect}
            disabled={(date) => evaluateDisabled(dayjs(date), disabledDate)}
            showOutsideDays
            labels={zhLabels}
            formatters={{
              formatCaption: (date) => dayjs(date).format('YYYY年M月'),
            }}
            classNames={dayPickerClassNames}
          />
          {showTime && (
            <RangeTimePanels
              showHour={fmt.includes('H')}
              showMinute={fmt.includes('m')}
              showSecond={fmt.includes('s')}
              value={pending}
              onPick={handleTimePick}
            />
          )}
          <PickerFooter onClear={handleClear} onConfirm={handleConfirm} />
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  )
}

// antd 兼容静态成员:`<DatePicker.RangePicker />` 与 `const { RangePicker } = DatePicker`
DatePicker.RangePicker = RangePicker
