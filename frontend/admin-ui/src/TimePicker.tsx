/**
 * TimePicker —— Radix Popover + 时/分/秒列面板(antd 风格),值域 dayjs。
 * 契约:docs/compose/spec/admin-radix-ui.md [S2.7];format 取调用点子集(HH:mm)。
 * 触发器/底栏/时间列复用 ./DatePicker 的共享件,单向引用避免循环依赖。
 * 区间形态以 antd 兼容静态挂载:`TimePicker.RangePicker`(开始/结束双时间列)。
 */
import { useState } from 'react'
import type { CSSProperties } from 'react'
import * as Popover from '@radix-ui/react-popover'
import * as stylex from '@stylexjs/stylex'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import { formatDayjs, formatRange, mergeTime, normalizeRange, orderRange } from './picker-core'
import type { RangeDateStrings, RangeValue } from './picker-core'
import {
  PickerField,
  PickerFooter,
  RangePickerField,
  RangeTimePanels,
  TimePanel,
  pickerPanelStyle,
} from './DatePicker'
import type { PickerSize, TimePatch, TimeSteps } from './DatePicker'

export interface TimePickerProps {
  value?: Dayjs | null
  onChange?: (date: Dayjs | null, dateString: string) => void
  format?: string
  placeholder?: string
  disabled?: boolean
  allowClear?: boolean
  size?: PickerSize
  /** 时/分/秒步长,默认 1 */
  steps?: TimeSteps
  /** 受控开合(调用点未传时走内部状态) */
  open?: boolean
  onOpenChange?: (open: boolean) => void
  id?: string
  className?: string
  style?: CSSProperties
}

export function TimePicker({
  value = null,
  onChange,
  format = 'HH:mm',
  placeholder,
  disabled = false,
  allowClear,
  size = 'middle',
  steps,
  open,
  onOpenChange,
  id,
  className,
  style,
}: TimePickerProps) {
  const [uncontrolledOpen, setUncontrolledOpen] = useState(false)
  const isOpen = open ?? uncontrolledOpen

  const setOpen = (next: boolean): void => {
    if (disabled && next) return
    if (open === undefined) setUncontrolledOpen(next)
    onOpenChange?.(next)
  }

  const emit = (next: Dayjs | null): void => {
    onChange?.(next, next === null ? '' : formatDayjs(next, format))
  }

  const handlePick = (patch: TimePatch): void => {
    // 未选值时以「今天零点」为底:先点小时得到整点,再点分钟补齐
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
        format={format}
        placeholder={placeholder ?? '选择时间'}
        disabled={disabled}
        allowClear={allowClear}
        size={size}
        icon="clock"
        inputSize={6}
        id={id}
        className={className}
        style={style}
        onClear={() => emit(null)}
        onCommit={(date) => {
          // 键入内容只有时间语义,日期部分沿用现值(缺省为今天)
          if (date === null) emit(null)
          else emit(mergeTime(value ?? dayjs(), date))
        }}
      />
      <Popover.Portal>
        <Popover.Content
          align="start"
          sideOffset={8}
          onOpenAutoFocus={(event) => event.preventDefault()}
          {...stylex.props(pickerPanelStyle.panel)}
        >
          <TimePanel
            showHour={format.includes('H')}
            showMinute={format.includes('m')}
            showSecond={format.includes('s')}
            hour={value?.hour()}
            minute={value?.minute()}
            second={value?.second()}
            steps={steps}
            onPick={handlePick}
          />
          <PickerFooter onClear={() => emit(null)} onConfirm={() => setOpen(false)} />
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  )
}

export interface TimeRangePickerProps {
  value?: RangeValue
  onChange?: (dates: RangeValue, dateStrings: RangeDateStrings) => void
  format?: string
  placeholder?: [string, string]
  disabled?: boolean
  allowClear?: boolean
  size?: PickerSize
  /** 时/分/秒步长,默认 1 */
  steps?: TimeSteps
  /** 受控开合(调用点未传时走内部状态) */
  open?: boolean
  onOpenChange?: (open: boolean) => void
  id?: string
  className?: string
  style?: CSSProperties
}

/**
 * TimeRangePicker —— 时间区间(开始/结束双时间列一个 Popover,无日历):
 * - 值域 [开始, 结束](两侧可 null),onChange(dates, dateStrings),format 默认 HH:mm;
 * - 点选过程只进 pending 草稿,「确定」统一锁序提交(与 showTime 日期区间一致);
 *   输入框失焦补全两端即提交;「清空」立即提交 null;
 * - 「确定」仅在两端齐全或全空时提交,半区间只收起(system/order 的
 *   「开始 < 结束」校验依赖成对载荷,避免 [start,null] 触发校验崩溃)。
 */
export function TimeRangePicker({
  value = null,
  onChange,
  format = 'HH:mm',
  placeholder = ['开始时间', '结束时间'],
  disabled = false,
  allowClear = true,
  size = 'middle',
  steps,
  open,
  onOpenChange,
  id,
  className,
  style,
}: TimeRangePickerProps) {
  const committed = normalizeRange(value)
  const [pending, setPending] = useState<RangeValue>(committed)
  const [uncontrolledOpen, setUncontrolledOpen] = useState(false)
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
    onChange?.(payload, formatRange(payload, format))
  }

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

  const handlePick = (index: 0 | 1, patch: TimePatch): void => {
    const cur: [Dayjs | null, Dayjs | null] = pending ?? [null, null]
    // 未选侧以「今天零点」为底(与单值 TimePicker 一致):先点小时得到整点,再点分钟补齐
    const base = cur[index] ?? dayjs().startOf('day')
    const timePart = base
      .hour(patch.hour ?? base.hour())
      .minute(patch.minute ?? base.minute())
      .second(patch.second ?? base.second())
      .millisecond(0)
    const next: [Dayjs | null, Dayjs | null] =
      index === 0 ? [timePart, cur[1]] : [cur[0], timePart]
    setPending(next)
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

  return (
    <Popover.Root open={isOpen} onOpenChange={setOpen}>
      <RangePickerField
        value={isOpen ? pending : committed}
        format={format}
        placeholder={placeholder}
        disabled={disabled}
        allowClear={allowClear}
        size={size}
        icon="clock"
        inputSize={Math.min(format.length, 19)}
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
          <RangeTimePanels
            showHour={format.includes('H')}
            showMinute={format.includes('m')}
            showSecond={format.includes('s')}
            value={pending}
            steps={steps}
            onPick={handlePick}
          />
          <PickerFooter onClear={handleClear} onConfirm={handleConfirm} />
        </Popover.Content>
      </Popover.Portal>
    </Popover.Root>
  )
}

// antd 兼容静态成员:`<TimePicker.RangePicker />` 与解构 `const { RangePicker } = TimePicker`
TimePicker.RangePicker = TimeRangePicker
