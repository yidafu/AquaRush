/**
 * 组件 DOM 测试共享工具(非 .test 文件,jest testMatch 不会把它当套件)。
 * React 19:不用 @testing-library,react 的 act + react-dom/client 直驱。
 */
import { act } from 'react'
import type { ReactElement } from 'react'
import { createRoot, type Root } from 'react-dom/client'

// React 19:非 testing-library 环境需显式开启 act 环境
;(globalThis as { IS_REACT_ACT_ENVIRONMENT?: boolean }).IS_REACT_ACT_ENVIRONMENT = true

export interface RenderResult {
  container: HTMLElement
  rerender: (next: ReactElement) => void
  unmount: () => void
}

const mounted: RenderResult[] = []

/** 渲染到独立容器并登记,afterEach(cleanup) 统一卸载。 */
export function render(element: ReactElement): RenderResult {
  const container = document.createElement('div')
  document.body.appendChild(container)
  const root: Root = createRoot(container)
  act(() => {
    root.render(element)
  })
  const result: RenderResult = {
    container,
    rerender: (next) => {
      act(() => {
        root.render(next)
      })
    },
    unmount: () => {
      const at = mounted.indexOf(result)
      if (at >= 0) mounted.splice(at, 1)
      act(() => {
        root.unmount()
      })
      container.remove()
    },
  }
  mounted.push(result)
  return result
}

export function cleanup(): void {
  while (mounted.length > 0) {
    mounted[mounted.length - 1].unmount()
  }
}

/** 冒泡派发 click(React 19 监听挂在根容器上)。 */
export function click(element: Element): void {
  act(() => {
    element.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true }))
  })
}

/**
 * 受控 input 赋值 + 触发 onChange:绕过 React value tracker 直接走
 * HTMLInputElement 原型 setter,再派发 input 事件。
 */
export function changeInput(input: HTMLInputElement, value: string): void {
  const setter = Object.getOwnPropertyDescriptor(HTMLInputElement.prototype, 'value')?.set
  act(() => {
    setter?.call(input, value)
    input.dispatchEvent(new Event('input', { bubbles: true }))
  })
}
