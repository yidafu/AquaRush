/**
 * 图标数据完整性守卫：名称全集、viewBox、路径非空。
 */
import { describe, expect, it } from '@jest/globals'
import { ICON_DATA, type IconName } from './data'

const ALL_NAMES = Object.keys(ICON_DATA) as IconName[]

describe('ui/icons 数据', () => {
  it('覆盖 77 个图标（与迁移前 @ant-design/icons 用集一致）', () => {
    expect(ALL_NAMES).toHaveLength(77)
  })

  it('每个图标都有 viewBox 与非空 inner', () => {
    for (const name of ALL_NAMES) {
      const def = ICON_DATA[name]
      expect(def.viewBox).toMatch(/^\d+ \d+ \d+ \d+$/)
      expect(def.inner.length).toBeGreaterThan(20)
      expect(def.inner).toContain('path')
    }
  })
})
