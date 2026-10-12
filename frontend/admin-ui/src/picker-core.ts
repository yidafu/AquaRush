/**
 * DatePicker / TimePicker / RangePicker 纯函数内核(不依赖 React):格式化、
 * 输入解析、禁用判定、日期时间合并、区间归一。组件层只做接线,
 * 取值/回验规则集中在本文件。
 *
 * 契约:docs/compose/spec/admin-radix-ui.md [S2.7]。
 */
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'

/**
 * disabledDate 回调。第二个参数是评估时锚定的「今天」——
 * 回调内需要比较当前时间时用它,避免同一帧内重复取 now 造成跨日抖动。
 */
export type DisabledDateFn = (day: Dayjs, today: Dayjs) => boolean

/** 按 format 格式化;null / undefined / 无效日期返回空串。 */
export function formatDayjs(date: Dayjs | null | undefined, format: string): string {
  if (date === null || date === undefined || !date.isValid()) return ''
  return date.format(format)
}

/**
 * 解析用户输入为 Dayjs,任何不合法或与 format 不一致的输入返回 null:
 * 1. 先用 dayjs 宽松解析,再以 format 往返回验 —— 拦截 "2026-02-31"(滚位到
 *    03-03)、"2026-13-01" 这类日历上不存在但能被 Date 构造的字符串;
 * 2. 纯时间串("14:30")dayjs 核心不识别,按 HH:mm[:ss] 构造到「今天」再回验
 *    (回验同时拦掉 "25:00" 这类溢出值)。
 * 注意:回验要求与 format 逐字符一致,"2026-2-5" 对 "YYYY-MM-DD" 会被拒绝。
 */
export function parseInput(input: string, format: string): Dayjs | null {
  const text = input.trim()
  if (text === '') return null

  const direct = dayjs(text)
  if (direct.isValid() && direct.format(format) === text) return direct

  const timeMatch = /^(\d{1,2}):(\d{2})(?::(\d{2}))?$/.exec(text)
  if (timeMatch !== null) {
    const hour = timeMatch[1]
    const minute = timeMatch[2]
    const second = timeMatch[3] ?? '0'
    const built = dayjs()
      .hour(Number(hour))
      .minute(Number(minute))
      .second(Number(second))
      .millisecond(0)
    if (built.format(format) === text) return built
  }
  return null
}

/**
 * 判定某天是否禁选:
 * - 无效日期恒禁选(即使未提供 disabledDate);
 * - 未提供 disabledDate 时一律可选(不做默认的「禁过去/禁未来」);
 * - 提供时把归一到自然日的 day 与锚点 today 一并交给回调。
 */
export function evaluateDisabled(
  day: Dayjs,
  disabledDate?: DisabledDateFn,
  today: Dayjs = dayjs(),
): boolean {
  if (!day.isValid()) return true
  if (!disabledDate) return false
  return disabledDate(day.startOf('day'), today)
}

/** 日期取 datePart、时间(时分秒毫秒)取 timePart,合成新值(不修改入参)。 */
export function mergeTime(datePart: Dayjs, timePart: Dayjs): Dayjs {
  return datePart
    .startOf('day')
    .hour(timePart.hour())
    .minute(timePart.minute())
    .second(timePart.second())
    .millisecond(timePart.millisecond())
}

// —— RangePicker 区间内核 ——

/** RangePicker 值类型(antd 同构):[开始, 结束],两侧各自可为 null。 */
export type RangeValue = [Dayjs | null, Dayjs | null] | null

/** RangePicker onChange 第二参:按 format 输出的 [开始串, 结束串],空侧为空串。 */
export type RangeDateStrings = [string, string]

/**
 * 归一化外部值(Form 回填 / 任意形状)为区间:
 * - null、undefined、非数组 → null;
 * - 数组按 [开始, 结束] 取位,非 Dayjs 或无效日期的位 → null;
 * 不做顺序交换(见 orderRange),孤立的结束侧原样保留(仅影响展示,
 * 提交时由组件层丢弃,见 emit 载荷守卫)。
 */
export function normalizeRange(value: unknown): RangeValue {
  if (!Array.isArray(value)) return null
  const toDayjsOrNull = (raw: unknown): Dayjs | null =>
    dayjs.isDayjs(raw) && raw.isValid() ? raw : null
  return [toDayjsOrNull(value[0]), toDayjsOrNull(value[1])]
}

/**
 * 首尾顺序归一(「开始 ≤ 结束」锁序):两端都有值且结束早于开始时交换
 * (antd 语义:后选日期早于先选 → 交换);只有一侧有值时原样返回。
 * 不修改入参。
 */
export function orderRange(range: RangeValue): RangeValue {
  if (range === null) return null
  const [start, end] = range
  if (start !== null && end !== null && end.isBefore(start)) return [end, start]
  return [start, end]
}

/** 按 format 输出 [开始串, 结束串];null 侧为空串(与 antd dateStrings 对齐)。 */
export function formatRange(range: RangeValue, format: string): RangeDateStrings {
  return [formatDayjs(range?.[0] ?? null, format), formatDayjs(range?.[1] ?? null, format)]
}
