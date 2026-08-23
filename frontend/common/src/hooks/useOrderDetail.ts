/**
 * useOrderDetail —— 订单详情页处理逻辑(weapp / delivery-app 共用,平台无关)
 *
 * 共享:加载编排(loading/error/refresh)、订单快照归一化(toOrderDetailView)、
 * 费用明细行(buildCostRows)、状态派生(getStatusView)。
 * 各端注入数据获取(fetcher),并自行触发 refresh(weapp useDidShow/usePullDownRefresh,
 * RN useEffect/RefreshControl);角色动作(支付/接单等)与 UI 留在各端。
 */
import { useCallback, useEffect, useRef, useState } from 'react'

import type { Order } from '../types/graphql'
import { buildCostRows, orderCostSnapshotFromOrder, type CostRow } from '../utils/order-cost'
import { getStatusView } from '../utils/order-status'
import type { StatusTone } from '../utils/status-tone'
import { toOrderDetailView, type OrderDetailView } from './order-view'

export interface UseOrderDetailOptions {
  /** 数据获取(各端注入:返回 null 表示无数据) */
  fetchOrder: () => Promise<Order | null>
}

export interface OrderStatusView {
  text: string | undefined
  tone: StatusTone | undefined
  step: number
}

export function useOrderDetail(
  orderNo: string | undefined,
  { fetchOrder }: UseOrderDetailOptions
): {
  order: Order | null
  loading: boolean
  error: string | null
  refresh: () => Promise<void>
  view: OrderDetailView | null
  costRows: CostRow[]
  status: OrderStatusView
} {
  const [order, setOrder] = useState<Order | null>(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const fetchingRef = useRef(false)
  // fetcher 经 ref 读取,避免调用方内联函数每次渲染变化触发 useEffect 重跑(加载循环)
  const fetchOrderRef = useRef(fetchOrder)
  fetchOrderRef.current = fetchOrder

  const load = useCallback(async (): Promise<void> => {
    if (!orderNo) {
      setLoading(false)
      return
    }
    if (fetchingRef.current) return
    fetchingRef.current = true
    setLoading(true)
    setError(null)
    try {
      const data = await fetchOrderRef.current()
      setOrder(data)
    } catch (e) {
      setError(e instanceof Error ? e.message : '获取订单详情失败')
    } finally {
      fetchingRef.current = false
      setLoading(false)
    }
  }, [orderNo])

  useEffect(() => {
    void load()
  }, [load])

  const view = order ? toOrderDetailView(order) : null
  const costRows = order ? buildCostRows(orderCostSnapshotFromOrder(order)) : []
  const status = getStatusView(order?.status ?? null)

  return { order, loading, error, refresh: load, view, costRows, status }
}
