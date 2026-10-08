/**
 * usePagedList —— 通用分页列表状态机(weapp order-list / delivery history-orders / admin 表格共用)
 *
 * 共享:currentTab 无关的分页状态机(page/hasMore/loading/refreshing/loadingMore、
 * 分页合并、刷新替换、参数重置);Tab/筛选参数由调用方经 setParams 传入。
 * 平台无关:触底加载由页面在 useReachBottom / onEndReached 中调用 loadMore,
 * 下拉刷新在 usePullDownRefresh / RefreshControl 中调用 refresh。
 */
import { useCallback, useEffect, useRef, useState } from 'react'

export interface PagedListFetcher<T> {
  (page: number, params: Record<string, unknown>): Promise<{ items: T[]; hasMore: boolean }>
}

export interface UsePagedListOptions<T> {
  fetchPage: PagedListFetcher<T>
  initialParams?: Record<string, unknown>
}

export function usePagedList<T>({ fetchPage, initialParams = {} }: UsePagedListOptions<T>): {
  items: T[]
  /** 首屏/空列表加载中 */
  loading: boolean
  /** 下拉刷新中 */
  refreshing: boolean
  /** 触底加载更多中 */
  loadingMore: boolean
  hasMore: boolean
  page: number
  refresh: () => Promise<void>
  loadMore: () => Promise<void>
  /** 更新查询参数并回到第一页 */
  setParams: (params: Record<string, unknown>) => Promise<void>
} {
  const [items, setItems] = useState<T[]>([])
  const [loading, setLoading] = useState(true)
  const [refreshing, setRefreshing] = useState(false)
  const [loadingMore, setLoadingMore] = useState(false)
  const [hasMore, setHasMore] = useState(true)
  const [page, setPage] = useState(0)

  const itemsRef = useRef<T[]>([])
  const pageRef = useRef(0)
  const hasMoreRef = useRef(true)
  const busyRef = useRef(false)
  const paramsRef = useRef<Record<string, unknown>>(initialParams)
  const fetchPageRef = useRef(fetchPage)
  fetchPageRef.current = fetchPage

  const refresh = useCallback(async (): Promise<void> => {
    if (busyRef.current) return
    busyRef.current = true
    if (itemsRef.current.length === 0) setLoading(true)
    setRefreshing(true)
    try {
      const res = await fetchPageRef.current(0, paramsRef.current)
      itemsRef.current = res.items
      hasMoreRef.current = res.hasMore
      pageRef.current = 0
      setItems(res.items)
      setHasMore(res.hasMore)
      setPage(0)
    } catch (e) {
      // 刷新失败保留旧数据
      console.error('列表加载失败:', e)
    } finally {
      setRefreshing(false)
      setLoading(false)
      busyRef.current = false
    }
  }, [])

  const loadMore = useCallback(async (): Promise<void> => {
    if (busyRef.current || !hasMoreRef.current) return
    busyRef.current = true
    setLoadingMore(true)
    try {
      const next = pageRef.current + 1
      const res = await fetchPageRef.current(next, paramsRef.current)
      itemsRef.current = [...itemsRef.current, ...res.items]
      hasMoreRef.current = res.hasMore
      pageRef.current = next
      setItems(itemsRef.current)
      setHasMore(res.hasMore)
      setPage(next)
    } catch (e) {
      // 加载更多失败:保留现状,可再次触发
      console.error('加载更多失败:', e)
    } finally {
      setLoadingMore(false)
      busyRef.current = false
    }
  }, [])

  const setParams = useCallback(
    async (params: Record<string, unknown>): Promise<void> => {
      paramsRef.current = params
      await refresh()
    },
    [refresh]
  )

  // 挂载自动加载第一页(refresh 为稳定引用,仅执行一次)
  useEffect(() => {
    void refresh()
  }, [refresh])

  return { items, loading, refreshing, loadingMore, hasMore, page, refresh, loadMore, setParams }
}
