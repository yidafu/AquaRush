/**
 * usePagedList renderHook 单测:首屏加载、分页追加、hasMore 终止、setParams 重置。
 */
import { act, renderHook, waitFor } from '@testing-library/react'
import { describe, expect, it } from '@jest/globals'
import { usePagedList } from './usePagedList'

const makeFetcher = (pages: string[][], lastHasMore = true) => {
  const calls: Array<{ page: number; params: Record<string, unknown> }> = []
  const fetchPage = async (page: number, params: Record<string, unknown>) => {
    calls.push({ page, params })
    const items = pages[page] ?? []
    return { items, hasMore: page < pages.length - 1 ? lastHasMore : false }
  }
  return { fetchPage, calls }
}

describe('usePagedList', () => {
  it('首屏:refresh 拉第 0 页,loading/refreshing 状态流转', async () => {
    const { fetchPage, calls } = makeFetcher([['a', 'b'], ['c']])
    const { result } = renderHook(() => usePagedList({ fetchPage }))
    expect(result.current.loading).toBe(true)
    await waitFor(() => expect(result.current.loading).toBe(false))
    expect(calls[0].page).toBe(0)
    expect(result.current.items).toEqual(['a', 'b'])
    expect(result.current.hasMore).toBe(true)
    expect(result.current.refreshing).toBe(false)
  })

  it('loadMore 追加下一页并推进 page;hasMore=false 后不再请求', async () => {
    const { fetchPage, calls } = makeFetcher([['a'], ['b'], ['c']])
    const { result } = renderHook(() => usePagedList({ fetchPage }))
    await waitFor(() => expect(result.current.loading).toBe(false))

    await act(async () => {
      await result.current.loadMore()
    })
    expect(result.current.items).toEqual(['a', 'b'])
    expect(result.current.page).toBe(1)
    expect(result.current.loadingMore).toBe(false)

    await act(async () => {
      await result.current.loadMore()
    })
    expect(result.current.items).toEqual(['a', 'b', 'c'])
    expect(result.current.page).toBe(2)
    expect(result.current.hasMore).toBe(false)

    await act(async () => {
      await result.current.loadMore()
    })
    expect(calls.length).toBe(3) // 第 4 次未发起
  })

  it('refresh 重置回第 0 页并替换数据', async () => {
    const pages = [['a'], ['b']]
    let seq = 0
    const { result } = renderHook(() =>
      usePagedList({
        fetchPage: async (page) => {
          seq += 1
          if (page === 0) return { items: [`r${seq}-a`], hasMore: true }
          return { items: pages[page] ?? [], hasMore: false }
        },
      })
    )
    await waitFor(() => expect(result.current.loading).toBe(false))
    await act(async () => {
      await result.current.loadMore()
    })
    expect(result.current.items).toEqual(['r1-a', 'b'])

    await act(async () => {
      await result.current.refresh()
    })
    expect(result.current.items).toEqual(['r3-a'])
    expect(result.current.page).toBe(0)
  })

  it('setParams 更新参数并回到第一页', async () => {
    const { fetchPage, calls } = makeFetcher([['a'], ['b']])
    const { result } = renderHook(() => usePagedList({ fetchPage, initialParams: { status: 'ALL' } }))
    await waitFor(() => expect(result.current.loading).toBe(false))
    await act(async () => {
      await result.current.loadMore()
    })
    await act(async () => {
      await result.current.setParams({ status: 'DELIVERING' })
    })
    expect(calls[calls.length - 1]).toMatchObject({ page: 0, params: { status: 'DELIVERING' } })
    expect(result.current.page).toBe(0)
  })

  it('刷新失败保留旧数据', async () => {
    let fail = false
    const { result } = renderHook(() =>
      usePagedList({
        fetchPage: async (page) => {
          if (fail && page === 0) throw new Error('boom')
          return { items: page === 0 ? ['a'] : ['b'], hasMore: true }
        },
      })
    )
    await waitFor(() => expect(result.current.loading).toBe(false))
    fail = true
    await act(async () => {
      await result.current.refresh()
    })
    expect(result.current.items).toEqual(['a'])
    expect(result.current.refreshing).toBe(false)
  })
})
