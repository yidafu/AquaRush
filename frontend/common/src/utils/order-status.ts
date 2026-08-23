/**
 * 订单状态文案/进度步骤/Tab —— 三端(weapp / delivery-app / admin-client)统一来源
 *
 * - 文案键 = 后端 OrderStatus 枚举 wire 值(common types/graphql.ts);
 * - 颜色一律走 status-tone.ts 的 ORDER_STATUS_TONE(本模块不含颜色);
 * - delivery-app 送水工视角的「待接单」是对 PENDING_DISPATCH 的本地措辞覆盖,
 *   系统统一文案为「待派单」(admin 统计页 / PRD 02-business-flows.md)。
 */
import { OrderStatus } from '../types/graphql'
import { ORDER_STATUS_TONE, getStatusTone, type StatusTone } from './status-tone'

/** 订单状态 → 统一文案 */
export const ORDER_STATUS_TEXT: Record<OrderStatus, string> = {
  [OrderStatus.PENDING_PAYMENT]: '待付款',
  [OrderStatus.PENDING_DISPATCH]: '待派单',
  [OrderStatus.PENDING_DELIVERY]: '待配送',
  [OrderStatus.DELIVERING]: '配送中',
  [OrderStatus.COMPLETED]: '已完成',
  [OrderStatus.CANCELLED]: '已取消',
  [OrderStatus.REFUNDED]: '已退款',
}

/** 配送进度步骤(不含待付款/已取消/已退款) */
export const ORDER_STEPS: string[] = ['待派单', '待配送', '配送中', '已完成']

/** 状态 → 进度下标(PENDING_DISPATCH=0 … COMPLETED=3;其余返回 0) */
export function getCurrentStep(status: string | null | undefined): number {
  const statusToStep: Record<string, number> = {
    [OrderStatus.PENDING_DISPATCH]: 0,
    [OrderStatus.PENDING_DELIVERY]: 1,
    [OrderStatus.DELIVERING]: 2,
    [OrderStatus.COMPLETED]: 3,
  }
  if (status == null) return 0
  return statusToStep[status] ?? 0
}

/** 订单状态 → 统一文案(未命中返回 undefined,调用方兜底) */
export function getOrderStatusText(status: string | null | undefined): string | undefined {
  if (status == null) return undefined
  return ORDER_STATUS_TEXT[status as OrderStatus]
}

/** 订单状态 → 视图派生:{ text, tone, step }(颜色 tone 供各端映射到平台渲染) */
export function getStatusView(status: string | null | undefined): {
  text: string | undefined
  tone: StatusTone | undefined
  step: number
} {
  return {
    text: getOrderStatusText(status),
    tone: getStatusTone(ORDER_STATUS_TONE, status),
    step: getCurrentStep(status),
  }
}

/** 订单列表 Tab(「待发货」口径 = 待派单 + 待配送,见 02-business-flows.md §订单状态机) */
export const ORDER_TABS: { title: string; statuses: OrderStatus[] | null }[] = [
  { title: '全部', statuses: null },
  { title: '待付款', statuses: [OrderStatus.PENDING_PAYMENT] },
  { title: '待配送', statuses: [OrderStatus.PENDING_DISPATCH, OrderStatus.PENDING_DELIVERY] },
  { title: '配送中', statuses: [OrderStatus.DELIVERING] },
  { title: '已完成', statuses: [OrderStatus.COMPLETED] },
]
