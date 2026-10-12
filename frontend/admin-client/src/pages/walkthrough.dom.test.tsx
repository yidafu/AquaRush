/**
 * 代表页挂载走查(spec admin-radix-ui [S2.9] 视觉走查的自动化部分):
 * 浏览器像素截图在本环境不可用(IAB compositor 空帧、evaluate 只读无法伪造登录态),
 * 以 jsdom 挂载冒烟替代 —— 订单列表 / 系统设置(库存阈值) / 统计 三页在
 * stub Apollo(立即回空数据)下挂载不抛异常且渲染出 DOM。
 * 样式层面的像素级核对仍需人工 QA(见 spec Report)。
 */
import { afterEach, describe, expect, it } from '@jest/globals'
import { ApolloClient, ApolloLink, ApolloProvider, InMemoryCache, Observable } from '@apollo/client'
import { MemoryRouter } from 'react-router-dom'
import type { ReactElement } from 'react'
import { cleanup, render } from '@/ui/test-utils'
import OrdersPage from './orders/index'
import SystemStockPage from './system/stock/index'
import StatisticsPage from './statistics/index'

const stubLink = new ApolloLink(
  () =>
    new Observable((observer) => {
      observer.next({ data: {} })
      observer.complete()
    }),
)

const client = new ApolloClient({ link: stubLink, cache: new InMemoryCache() })

const mount = (ui: ReactElement, path: string) =>
  render(
    <MemoryRouter initialEntries={[path]}>
      <ApolloProvider client={client}>{ui}</ApolloProvider>
    </MemoryRouter>,
  )

afterEach(() => {
  cleanup()
})

describe('代表页挂载走查', () => {
  it('订单列表页(/orders)挂载不抛异常并渲染 DOM', () => {
    const { container } = mount(<OrdersPage />, '/orders')
    expect(container.firstChild).not.toBeNull()
    expect(container.textContent).toBeDefined()
  })

  it('系统设置页(库存阈值 /system-config/stock)挂载不抛异常并渲染 DOM', () => {
    const { container } = mount(<SystemStockPage />, '/system-config/stock')
    expect(container.firstChild).not.toBeNull()
    // 表单页在空数据下也应渲染出配置项文案
    expect(container.textContent).toContain('阈值')
  })

  it('统计页(/statistics)挂载不抛异常并渲染 DOM', () => {
    const { container } = mount(<StatisticsPage />, '/statistics')
    expect(container.firstChild).not.toBeNull()
    expect(container.textContent).toBeDefined()
  })
})
