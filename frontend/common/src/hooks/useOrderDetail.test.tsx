/**
 * useOrderDetail renderHook 单测:加载成功/失败/刷新/无 orderNo。
 */
import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it } from '@jest/globals'
import { Order, OrderStatus } from '../types/graphql'
import { useOrderDetail } from './useOrderDetail'

const makeOrder = (overrides: Partial<Order> = {}): Order => ({
  id: '1',
  orderNo: 'NO-1',
  status: OrderStatus.DELIVERING,
  amountCents: 6500,
  createdAt: '2026-08-01T10:00:00',
  deliveryFeeCents: 800,
  depositCents: 0,
  payAmountCents: 5800,
  isSelfCollect: false,
  items: [{ productId: 'p1', quantity: 1, unitPriceCents: 2500, totalPriceCents: 2500, product: { priceCents: 2500, coverImageUrl: '', name: '水' } as any }],
  updatedAt: '2026-08-01T10:00:00',
  ...overrides,
})

describe('useOrderDetail', () => {
  it('加载成功:返回 order/view/costRows/status', async () => {
    const { result } = renderHook(() =>
      useOrderDetail('NO-1', { fetchOrder: async () => makeOrder() })
    )
    expect(result.current.loading).toBe(true)
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBeNull()
    expect(result.current.order?.orderNo).toBe('NO-1')
    expect(result.current.view?.items[0].name).toBe('水')
    expect(result.current.status).toMatchObject({ text: '配送中', tone: 'success', step: 2 })
    expect(result.current.costRows.map((r) => r.key)).toEqual(['subtotal', 'delivery', 'total'])
  })

  it('fetch 返回 null:order 为 null,不抛错', async () => {
    const { result } = renderHook(() =>
      useOrderDetail('NO-2', { fetchOrder: async () => null })
    )
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.order).toBeNull()
    expect(result.current.view).toBeNull()
    expect(result.current.costRows).toEqual([])
  })

  it('fetch 抛错:error 有值', async () => {
    const { result } = renderHook(() =>
      useOrderDetail('NO-3', {
        fetchOrder: async () => {
          throw new Error('网络异常')
        },
      })
    )
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.error).toBe('网络异常')
  })

  it('无 orderNo:不发请求,loading=false', async () => {
    let called = 0
    const { result } = renderHook(() =>
      useOrderDetail(undefined, {
        fetchOrder: async () => {
          called += 1
          return makeOrder()
        },
      })
    )
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(called).toBe(0)
    expect(result.current.order).toBeNull()
  })

  it('refresh 重新拉取', async () => {
    let seq = 0
    const { result } = renderHook(() =>
      useOrderDetail('NO-4', {
        fetchOrder: async () => {
          seq += 1
          return makeOrder({ id: `v${seq}` })
        },
      })
    )
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(result.current.order?.id).toBe('v1')
    await act(async () => {
      await result.current.refresh()
    })
    expect(result.current.order?.id).toBe('v2')
  })
})
