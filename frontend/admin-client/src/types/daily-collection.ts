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
