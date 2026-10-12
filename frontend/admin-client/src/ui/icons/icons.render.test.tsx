/**
 * 图标 smoke 渲染(spec admin-radix-ui S2.9 / T2):全部 77 个图标经
 * react-dom/client 真渲染到 jsdom,断言每个产出 <svg>、viewBox 与数据一致、
 * 内部路径非空、stylex 编译后的 className 非空(兼作 jest stylex 插件冒烟)。
 *
 * 不引 @testing-library:React 19 用 react 的 act + createRoot 直驱。
 */
import { act } from 'react'
import { createRoot, type Root } from 'react-dom/client'
import { describe, expect, it } from '@jest/globals'
import { ICON_DATA, type IconName } from './data'
import * as icons from './index'

// React 19:非 testing-library 环境需显式开启 act 环境
;(globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true

const ALL_NAMES = Object.keys(ICON_DATA) as IconName[]

/** index 导出的图标组件(过滤掉非组件导出,当前 index 只导出图标) */
const ICON_COMPONENTS = Object.entries(icons).filter(([, value]) => typeof value === 'function') as Array<
  [string, (props: Record<string, unknown>) => React.ReactElement]
>

describe('ui/icons 渲染 smoke', () => {
  it('index 导出覆盖数据全集 77 个图标', () => {
    expect(ICON_COMPONENTS).toHaveLength(77)
    const exported = new Set(ICON_COMPONENTS.map(([name]) => name))
    for (const name of ALL_NAMES) {
      expect(exported.has(name)).toBe(true)
    }
  })

  it('每个图标渲染为带正确 viewBox 与非空内容的 <svg>,且 stylex className 非空', () => {
    const container = document.createElement('div')
    document.body.appendChild(container)
    const root: Root = createRoot(container)
    try {
      for (const name of ALL_NAMES) {
        const Icon = icons[name] as (props: Record<string, unknown>) => React.ReactElement
        act(() => {
          root.render(<Icon />)
        })
        const svg = container.querySelector('svg')
        expect(svg).not.toBeNull()
        expect(svg!.getAttribute('viewBox')).toBe(ICON_DATA[name].viewBox)
        // dangerouslySetInnerHTML 的路径内容非空
        expect(svg!.innerHTML.length).toBeGreaterThan(20)
        expect(svg!.innerHTML).toContain('path')
        // stylex 编译产物:className 是非空字符串(编译失败会是 undefined)
        const className = svg!.getAttribute('class')
        expect(className).toBeTruthy()
        expect(typeof className).toBe('string')
        expect(className!.length).toBeGreaterThan(0)
      }
    } finally {
      act(() => {
        root.unmount()
      })
      container.remove()
    }
  })
})
