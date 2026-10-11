/**
 * 设计 token 同步守卫(user-weapp 侧):theme.stylex.ts 的 pageTheme 状态色对
 * (statusXxxFg/statusXxxBg)必须与 @aquarush/common STATUS_TONE_COLORS 一致
 * (标签颜色单一来源,spec/design/ui-design-system.md §2.2)。
 *
 * 原实现守卫 theme.scss 的 --status-* 变量;StyleX 迁移后 theme.scss 已删除,
 * 载体换成 pageTheme(源码文本解析 —— stylex 运行时不暴露主题值)。
 * 共享层 tokens.stylex ↔ STATUS_TONE_COLORS 由 common 的 tokens.test.ts 守卫。
 */
import { readFileSync } from 'node:fs'
import { join } from 'node:path'
import { describe, expect, it } from '@jest/globals'
import { STATUS_TONE_COLORS } from '@aquarush/common'

const themeStylexSource = readFileSync(join(__dirname, 'theme.stylex.ts'), 'utf8')

/** 截取 pageTheme 主体(createTheme 花括号内),避免误匹配文件其它部分 */
const pageThemeBody = (): string => {
  const start = themeStylexSource.indexOf('export const pageTheme')
  expect(start).toBeGreaterThan(-1)
  const open = themeStylexSource.indexOf('{', start)
  const close = themeStylexSource.lastIndexOf('}')
  return themeStylexSource.slice(open, close)
}

/** 提取 statusXxxFg / statusXxxBg: value; 映射 */
const extractStatusPairs = (): Record<string, string> => {
  const map: Record<string, string> = {}
  const body = pageThemeBody()
  const re = /status([A-Za-z]+)(Fg|Bg):\s*'([^']+)'/g
  let m: RegExpExecArray | null
  while ((m = re.exec(body)) !== null) {
    map[`${m[1]}${m[2]}`] = m[3]
  }
  return map
}

describe('pageTheme 状态色对与 common STATUS_TONE_COLORS 同步', () => {
  it('pageTheme 定义全部 6 个 tone 的 Fg/Bg 色对', () => {
    const vars = extractStatusPairs()
    const tones = Object.keys(STATUS_TONE_COLORS)
    expect(tones.length).toBeGreaterThan(0)
    tones.forEach((tone) => {
      const capitalized = tone[0].toUpperCase() + tone.slice(1)
      expect(vars[`${capitalized}Fg`]).toBeDefined()
      expect(vars[`${capitalized}Bg`]).toBeDefined()
    })
  })

  it('tone 色对与 common 逐值一致(§2.2)', () => {
    const vars = extractStatusPairs()
    for (const [tone, pair] of Object.entries(STATUS_TONE_COLORS)) {
      const capitalized = tone[0].toUpperCase() + tone.slice(1)
      expect(vars[`${capitalized}Fg`]).toBe(pair.text)
      expect(vars[`${capitalized}Bg`]).toBe(pair.bg)
    }
  })
})
