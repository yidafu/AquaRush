// Daily Collection Types

export type DailyCollectionStatus = 'DRAFT' | 'SUBMITTED' | 'CONFIRMED' | 'DISPUTED'

export interface DailyCollectionRecord {
  id: string
  deliveryWorkerId: string
  collectionDate: string
  cashAmountCents: number | string
  waterTicketCount: number
  qrCodeAmountCents: number | string
  orderCount: number
  orderAmountCents: number | string
  differenceAmountCents: number | string
  // 系统计算字段
  calculatedCashAmountCents: number | string
  calculatedQrCodeAmountCents: number | string
  calculatedWaterTicketCount: number
  calculatedOrderAmountCents: number | string
  status: DailyCollectionStatus
  confirmedAt?: string
  confirmedBy?: string
  notes?: string
  // 驳回留痕(G28:管理员驳回争议日报)
  rejectedAt?: string
  rejectedBy?: string
  rejectReason?: string
  createdAt: string
  updatedAt: string
}

export type DailyReconciliationStatus = 'PENDING' | 'MATCHED' | 'DISCREPANCY' | 'REVIEWED'

export interface DailyReconciliation {
  id: string
  reconciliationDate: string
  totalCashAmountCents: number
  totalWaterTicketCount: number
  totalQrCodeAmountCents: number
  totalOrderCount: number
  totalOrderAmountCents: number
  totalCollectionAmountCents: number
  discrepancyAmountCents: number
  status: DailyReconciliationStatus
  reportData?: Record<string, unknown>
  reviewedAt?: string
  reviewedBy?: string
  reviewNotes?: string
  createdAt: string
  updatedAt: string
}

// ==================== 分页/汇总/逐单明细(2026 收银对账分页改造) ====================

export interface PageInfo {
  total: number
  pageSize: number
  pageNum: number
  hasNext: boolean
  hasPrevious: boolean
  totalPages: number
}

export interface DailyCollectionVoPage {
  list: DailyCollectionRecord[]
  pageInfo: PageInfo
}

export interface DailyReconciliationVoPage {
  list: DailyReconciliation[]
  pageInfo: PageInfo
}

/** 指定日期收银汇总(顶部汇总卡,服务端聚合) */
export interface DailyCollectionSummary {
  collectionDate: string
  totalCashAmountCents: number | string
  totalQrCodeAmountCents: number | string
  totalWaterTicketCount: number
  totalOrderCount: number
  totalOrderAmountCents: number | string
  totalSubmittedCount: number
}

/** 对账逐单明细(G29):当日已送达订单流水 */
export interface ReconciliationOrderDetail {
  orderId: string
  orderNo: string
  deliveryWorkerId?: string
  amountCents: number | string
  paymentType?: 'CASH' | 'QR_CODE' | 'WATER_TICKET'
  status: string
  deliveredAt?: string
}

export interface ReconciliationOrderDetailVoPage {
  list: ReconciliationOrderDetail[]
  pageInfo: PageInfo
}

/** 收款方式文案(对账逐单明细) */
export const PAYMENT_TYPE_TEXT: Record<string, string> = {
  CASH: '现金',
  QR_CODE: '扫码',
  WATER_TICKET: '水票',
}
