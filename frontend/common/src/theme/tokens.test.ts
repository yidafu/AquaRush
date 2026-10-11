/**
 * theme token 守卫测试(源码级比对):
 * - tokens.stylex.ts 的状态色对必须与 STATUS_TONE_COLORS(三端状态色唯一来源)一致;
 * - antd.ts 的 themeToken 必须与 tokens.stylex.ts 的对应 token 一致。
 *
 * tokens.stylex.ts 的值经 stylex babel 插件静态求值、不经运行时导出,
 * 故按源码文本解析比对(与 user-weapp 原有 theme.scss 守卫测试同一套路)。
 */
import { readFileSync } from 'node:fs'
import path from 'node:path'
import { describe, expect, it } from '@jest/globals'
import { ANTD_THEME_TOKEN } from './antd'
import { COLORS } from './colors'
import { STATUS_TONE_COLORS, type StatusTone } from '../utils/status-tone'

const source = readFileSync(path.join(__dirname, 'tokens.stylex.ts'), 'utf8')

/** 解析 defineVars 中 `  key: 'value',` 形式的 token 字面量(容忍行尾注释) */
const tokenValues: Record<string, string> = {}
for (const match of source.matchAll(/^ {2}(\w+): '([^']+)',?\s*(?:\/\/.*)?$/gm)) {
  tokenValues[match[1]] = match[2]
}

const toneTokenKey = (tone: StatusTone, part: 'Fg' | 'Bg'): string =>
  `status${tone[0].toUpperCase()}${tone.slice(1)}${part}`

describe('tokens.stylex ↔ STATUS_TONE_COLORS', () => {
  for (const tone of Object.keys(STATUS_TONE_COLORS) as StatusTone[]) {
    it(`tone ${tone} 的文字色/底色与状态 token 一致`, () => {
      expect(tokenValues[toneTokenKey(tone, 'Fg')]).toBe(STATUS_TONE_COLORS[tone].text)
      expect(tokenValues[toneTokenKey(tone, 'Bg')]).toBe(STATUS_TONE_COLORS[tone].bg)
    })
  }
})

describe('antd themeToken ↔ tokens.stylex', () => {
  it('色值与 token 一致', () => {
    expect(ANTD_THEME_TOKEN.colorPrimary).toBe(tokenValues.brand)
    expect(ANTD_THEME_TOKEN.colorLink).toBe(tokenValues.brand)
    expect(ANTD_THEME_TOKEN.colorBgLayout).toBe(tokenValues.bg)
    expect(ANTD_THEME_TOKEN.colorBgContainer).toBe(tokenValues.card)
    expect(ANTD_THEME_TOKEN.colorText).toBe(tokenValues.text)
    expect(ANTD_THEME_TOKEN.colorTextSecondary).toBe(tokenValues.textSub)
    expect(ANTD_THEME_TOKEN.colorError).toBe(tokenValues.error)
    expect(ANTD_THEME_TOKEN.colorSuccess).toBe(tokenValues.success)
    expect(ANTD_THEME_TOKEN.colorWarning).toBe(tokenValues.warning)
    expect(ANTD_THEME_TOKEN.colorBorderSecondary).toBe(tokenValues.border)
  })

  it('圆角与 token 数值一致', () => {
    expect(ANTD_THEME_TOKEN.borderRadius).toBe(Number.parseFloat(tokenValues.rSm))
    expect(ANTD_THEME_TOKEN.borderRadiusLG).toBe(Number.parseFloat(tokenValues.rCard))
  })
})

describe('COLORS(图表/antd 用真实值) ↔ tokens.stylex', () => {
  const colorKeyToToken: Record<string, string> = {
    brand: 'brand',
    brandLight: 'brandLight',
    brandDark: 'brandDark',
    brandTint: 'brandTint',
    action: 'action',
    price: 'price',
    bg: 'bg',
    card: 'card',
    text: 'text',
    textSub: 'textSub',
    textWeak: 'textWeak',
    border: 'border',
    success: 'success',
    warning: 'warning',
    error: 'error',
  }
  for (const [colorKey, tokenKey] of Object.entries(colorKeyToToken)) {
    it(`COLORS.${colorKey} === tokens.${tokenKey}`, () => {
      expect(COLORS[colorKey as keyof typeof COLORS]).toBe(tokenValues[tokenKey])
    })
  }
})
