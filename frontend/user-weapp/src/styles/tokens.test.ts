/**
 * 设计 token 同步守卫:user-weapp theme.scss 的 --status-* 变量值必须与
 * @aquarush/common STATUS_TONE_COLORS 一致(标签颜色单一来源,spec/design/ui-design-system.md §2.2)。
 * 任何一端漂移都会在此测试失败。
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from '@jest/globals'
import { STATUS_TONE_COLORS } from '@aquarush/common'

const themeScss = readFileSync(join(__dirname, 'theme.scss'), 'utf8')

/** 提取 --status-xxx: value; 映射 */
const extractStatusVars = (): Record<string, string> => {
  const map: Record<string, string> = {}
  const re = /--status-([a-z-]+):\s*([^;]+);/g
  let m: RegExpExecArray | null
  while ((m = re.exec(themeScss)) !== null) {
    map[m[1]] = m[2].trim()
  }
  return map
}

describe('theme.scss --status-* 与 common STATUS_TONE_COLORS 同步', () => {
  it('theme.scss 定义全部 --status-* 变量', () => {
    const vars = extractStatusVars()
    const expected = [
      'pending-pay-text',
      'pending-pay-bg',
      'dispatch-text',
      'dispatch-bg',
      'delivering-text',
      'delivering-bg',
      'completed-text',
      'completed-bg',
      'cancelled-text',
      'cancelled-bg',
    ]
    expected.forEach((name) => {
      expect(vars[name]).toBeDefined()
    })
  })

  it('tone 色对与 common 逐值一致(§2.2)', () => {
    const vars = extractStatusVars()
    const cases: Array<[string, string, string]> = [
      ['pending-pay-text', 'warning', 'text'],
      ['pending-pay-bg', 'warning', 'bg'],
      ['dispatch-text', 'info', 'text'],
      ['dispatch-bg', 'info', 'bg'],
      ['delivering-text', 'success', 'text'],
      ['delivering-bg', 'success', 'bg'],
      ['completed-text', 'neutral', 'text'],
      ['completed-bg', 'neutral', 'bg'],
      ['cancelled-text', 'error', 'text'],
      ['cancelled-bg', 'error', 'bg'],
    ]
    cases.forEach(([varSuffix, tone, key]) => {
      expect(vars[varSuffix]).toBe(
        STATUS_TONE_COLORS[tone as keyof typeof STATUS_TONE_COLORS][key as 'text' | 'bg']
      )
    })
  })
})
