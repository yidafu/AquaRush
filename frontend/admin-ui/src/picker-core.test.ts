/**
 * picker-core 纯函数守卫:格式化/解析往返、非法输入、禁用判定、时间合并、区间归一。
 */
import { describe, expect, it } from '@jest/globals'
import dayjs from 'dayjs'
import type { Dayjs } from 'dayjs'
import {
  evaluateDisabled,
  formatDayjs,
  formatRange,
  mergeTime,
  normalizeRange,
  orderRange,
  parseInput,
} from './picker-core'

describe('formatDayjs', () => {
  it('按 format 输出', () => {
    expect(formatDayjs(dayjs('2026-02-14'), 'YYYY-MM-DD')).toBe('2026-02-14')
    expect(formatDayjs(dayjs('2026-02-14 09:30:00'), 'HH:mm')).toBe('09:30')
    expect(formatDayjs(dayjs('2026-02-14'), 'YYYY-MM')).toBe('2026-02')
  })

  it('空值与无效日期返回空串', () => {
    expect(formatDayjs(null, 'YYYY-MM-DD')).toBe('')
    expect(formatDayjs(undefined, 'YYYY-MM-DD')).toBe('')
    expect(formatDayjs(dayjs('garbage'), 'YYYY-MM-DD')).toBe('')
  })
})

describe('parseInput', () => {
  it('日期与时间解析', () => {
    expect(parseInput('2026-02-14', 'YYYY-MM-DD')?.format('YYYY-MM-DD')).toBe('2026-02-14')
    const time = parseInput('14:30', 'HH:mm')
    expect(time?.format('HH:mm')).toBe('14:30')
    expect(time?.year()).toBe(dayjs().year())
    const withSeconds = parseInput('09:30:05', 'HH:mm:ss')
    expect(withSeconds?.format('HH:mm:ss')).toBe('09:30:05')
  })

  it('format 往返一致', () => {
    const samples: Array<[string, string]> = [
      ['2026-02-14', 'YYYY-MM-DD'],
      ['2026-02', 'YYYY-MM'],
      ['2026-02-14 09:30:00', 'YYYY-MM-DD HH:mm:ss'],
      ['2024-02-29', 'YYYY-MM-DD'],
    ]
    for (const [text, fmt] of samples) {
      const parsed = parseInput(text, fmt)
      expect(parsed).not.toBeNull()
      expect(formatDayjs(parsed, fmt)).toBe(text)
    }
  })

  it('非法或与 format 不符的输入返回 null', () => {
    expect(parseInput('', 'YYYY-MM-DD')).toBeNull()
    expect(parseInput('   ', 'YYYY-MM-DD')).toBeNull()
    expect(parseInput('garbage', 'YYYY-MM-DD')).toBeNull()
    expect(parseInput('2026-02-31', 'YYYY-MM-DD')).toBeNull()
    expect(parseInput('2026-13-01', 'YYYY-MM-DD')).toBeNull()
    expect(parseInput('02/14/2026', 'YYYY-MM-DD')).toBeNull()
    expect(parseInput('2026年2月14日', 'YYYY-MM-DD')).toBeNull()
    expect(parseInput('25:00', 'HH:mm')).toBeNull()
    expect(parseInput('14:60', 'HH:mm')).toBeNull()
    expect(parseInput('14:30', 'YYYY-MM-DD')).toBeNull()
  })
})

describe('evaluateDisabled', () => {
  it('未提供 disabledDate 时可选,无效日期恒禁选', () => {
    expect(evaluateDisabled(dayjs('2026-02-14'))).toBe(false)
    expect(evaluateDisabled(dayjs('garbage'))).toBe(true)
    expect(evaluateDisabled(dayjs('garbage'), () => false)).toBe(true)
  })

  it('按回调结果判定', () => {
    expect(evaluateDisabled(dayjs('2026-02-14'), (d) => d.isAfter(dayjs('2026-01-01')))).toBe(true)
    expect(evaluateDisabled(dayjs('2025-12-31'), (d) => d.isAfter(dayjs('2026-01-01')))).toBe(false)
    expect(evaluateDisabled(dayjs('2026-02-14'), () => false)).toBe(false)
  })

  it('把 today 锚点透传给回调', () => {
    const today = dayjs('2026-06-01')
    const seen: Dayjs[] = []
    const result = evaluateDisabled(
      dayjs('2026-02-14'),
      (_day, anchor) => {
        seen.push(anchor)
        return anchor.isSame(today, 'day')
      },
      today,
    )
    expect(result).toBe(true)
    expect(seen).toHaveLength(1)
    expect(seen[0].isSame(today, 'day')).toBe(true)
  })

  it('回调收到归一到自然日的 day', () => {
    let received = ''
    const result = evaluateDisabled(dayjs('2026-02-14 18:30:45'), (d) => {
      received = d.format('YYYY-MM-DD HH:mm:ss')
      return false
    })
    expect(result).toBe(false)
    expect(received).toBe('2026-02-14 00:00:00')
  })
})

describe('mergeTime', () => {
  it('日期取第一参、时间取第二参', () => {
    const merged = mergeTime(dayjs('2026-02-14'), dayjs('2020-01-01 09:30:15.123'))
    expect(merged.format('YYYY-MM-DD HH:mm:ss')).toBe('2026-02-14 09:30:15')
    expect(merged.millisecond()).toBe(123)
  })

  it('同一天场景只替换时分秒', () => {
    const merged = mergeTime(dayjs('2026-02-14 08:00:00'), dayjs('2026-02-14 22:45:30'))
    expect(merged.format('YYYY-MM-DD HH:mm:ss')).toBe('2026-02-14 22:45:30')
  })

  it('不修改入参', () => {
    const datePart = dayjs('2026-02-14 10:00:00')
    const timePart = dayjs('2026-02-14 10:00:00')
    mergeTime(datePart, timePart)
    expect(datePart.format('HH:mm')).toBe('10:00')
    expect(timePart.format('HH:mm')).toBe('10:00')
  })
})

describe('normalizeRange', () => {
  it('null/undefined/非数组归一为 null', () => {
    expect(normalizeRange(null)).toBeNull()
    expect(normalizeRange(undefined)).toBeNull()
    expect(normalizeRange('2026-02-14')).toBeNull()
    expect(normalizeRange({ from: '2026-02-14' })).toBeNull()
    expect(normalizeRange(0)).toBeNull()
  })

  it('数组按位取 Dayjs,非法位归 null,位置保持', () => {
    const start = dayjs('2026-02-14')
    const end = dayjs('2026-02-15')

    const both = normalizeRange([start, end])
    expect(both?.[0]?.format('YYYY-MM-DD')).toBe('2026-02-14')
    expect(both?.[1]?.format('YYYY-MM-DD')).toBe('2026-02-15')

    expect(normalizeRange([start, null])?.[1]).toBeNull()
    expect(normalizeRange([null, end])?.[0]).toBeNull()

    // 孤立结束侧保留位置(提交层再丢弃)
    const orphan = normalizeRange([null, end])
    expect(orphan?.[1]?.format('YYYY-MM-DD')).toBe('2026-02-15')

    // 非 Dayjs / 无效日期的位归 null
    const mixed = normalizeRange([start, '2026-02-15'])
    expect(mixed?.[0]?.format('YYYY-MM-DD')).toBe('2026-02-14')
    expect(mixed?.[1]).toBeNull()
    expect(normalizeRange([dayjs('garbage'), end])?.[0]).toBeNull()
    expect(normalizeRange([])).toEqual([null, null])
  })

  it('不修改入参且返回新数组', () => {
    const start = dayjs('2026-02-14')
    const end = dayjs('2026-02-15')
    const input: [Dayjs, Dayjs] = [start, end]
    const out = normalizeRange(input)
    expect(out).not.toBe(input)
    expect(input[0].format('YYYY-MM-DD')).toBe('2026-02-14')
    expect(input[1].format('YYYY-MM-DD')).toBe('2026-02-15')
  })
})

describe('orderRange', () => {
  it('null 与半区间原样返回', () => {
    expect(orderRange(null)).toBeNull()
    const start = dayjs('2026-02-14')
    const end = dayjs('2026-02-15')
    expect(orderRange([start, null])).toEqual([start, null])
    expect(orderRange([null, end])).toEqual([null, end])
  })

  it('结束早于开始时交换(开始 ≤ 结束锁序)', () => {
    const start = dayjs('2026-02-14')
    const end = dayjs('2026-02-10')
    const out = orderRange([start, end])
    expect(out?.[0]?.format('YYYY-MM-DD')).toBe('2026-02-10')
    expect(out?.[1]?.format('YYYY-MM-DD')).toBe('2026-02-14')
  })

  it('顺序正确与同值时不交换,不修改入参', () => {
    const start = dayjs('2026-02-14')
    const end = dayjs('2026-02-15')
    const same = dayjs('2026-02-14')
    expect(orderRange([start, end])).toEqual([start, end])
    expect(orderRange([start, same])).toEqual([start, same])

    const reversed: [Dayjs, Dayjs] = [end, start]
    orderRange(reversed)
    expect(reversed[0].format('YYYY-MM-DD')).toBe('2026-02-15')
    expect(reversed[1].format('YYYY-MM-DD')).toBe('2026-02-14')
  })

  it('带时间的交换保留完整时刻', () => {
    const out = orderRange([dayjs('2026-02-14 17:00'), dayjs('2026-02-14 09:30')])
    expect(out?.[0]?.format('YYYY-MM-DD HH:mm')).toBe('2026-02-14 09:30')
    expect(out?.[1]?.format('YYYY-MM-DD HH:mm')).toBe('2026-02-14 17:00')
  })
})

describe('formatRange', () => {
  it('按 format 输出两端,空侧为空串', () => {
    expect(formatRange([dayjs('2026-02-14'), dayjs('2026-02-15')], 'YYYY-MM-DD')).toEqual([
      '2026-02-14',
      '2026-02-15',
    ])
    expect(formatRange(null, 'YYYY-MM-DD')).toEqual(['', ''])
    expect(formatRange([dayjs('2026-02-14'), null], 'YYYY-MM-DD')).toEqual(['2026-02-14', ''])
    expect(formatRange([null, dayjs('2026-02-15')], 'YYYY-MM-DD')).toEqual(['', '2026-02-15'])
    expect(formatRange([dayjs('2026-02'), null], 'YYYY-MM')).toEqual(['2026-02', ''])
  })

  it('时间格式与带时间日期格式', () => {
    expect(formatRange([dayjs('2026-02-14 09:30'), dayjs('2026-02-14 17:00')], 'HH:mm')).toEqual([
      '09:30',
      '17:00',
    ])
    expect(
      formatRange([dayjs('2026-02-14 09:30:00'), dayjs('2026-02-15 17:00:00')], 'YYYY-MM-DD HH:mm'),
    ).toEqual(['2026-02-14 09:30', '2026-02-15 17:00'])
  })

  it('无效日期位输出空串', () => {
    expect(formatRange([dayjs('garbage'), dayjs('2026-02-15')], 'YYYY-MM-DD')).toEqual([
      '',
      '2026-02-15',
    ])
  })
})
