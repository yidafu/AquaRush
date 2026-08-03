import { networkManager } from '../utils/networkManager'

// 每日收款记录类型
export interface DailyCollectionRecord {
  id: string
  deliveryWorkerId: string
  collectionDate: string
  cashAmountCents: number
  waterTicketCount: number
  qrCodeAmountCents: number
  orderCount: number
  orderAmountCents: number
  differenceAmountCents: number
  status: 'DRAFT' | 'SUBMITTED' | 'CONFIRMED' | 'DISPUTED'
  confirmedAt?: string
  confirmedBy?: string
  notes?: string
  createdAt: string
  updatedAt: string
}

// 配送员今日收款预计算结果类型
export interface MyTodayCollectionVO {
  orderCount: number
  orderAmountCents: number
  cashAmountCents?: number
  waterTicketCount?: number
  qrCodeAmountCents?: number
  differenceAmountCents?: number
  // 系统计算的理论值
  calculatedCashAmountCents?: number
  calculatedQrCodeAmountCents?: number
  calculatedWaterTicketCount?: number
  calculatedOrderAmountCents?: number
  status?: 'DRAFT' | 'SUBMITTED' | 'CONFIRMED' | 'DISPUTED' | null
}

// 提交每日收款输入
export interface SubmitDailyCollectionInput {
  cashAmountCents: number
  waterTicketCount: number
  qrCodeAmountCents: number
  collectionDate?: string
  notes?: string // 差异说明，超出容差时填写
}

// 对账记录类型
export interface DailyReconciliation {
  id: string
  reconciliationDate: string
  totalOrderCount: number
  totalOrderAmountCents: number
  totalCollectionAmountCents: number
  totalCashAmountCents: number
  totalWaterTicketCount: number
  totalQrCodeAmountCents: number
  discrepancyAmountCents: number
  status: 'PENDING' | 'MATCHED' | 'DISCREPANCY' | 'REVIEWED'
  reviewedAt?: string
  reviewedBy?: string
  reviewNotes?: string
}

// 配送员提交每日收款
export const submitDailyCollection = (input: SubmitDailyCollectionInput) => {
  return networkManager.mutate<{ submitDailyCollection: DailyCollectionRecord }>(`
    mutation SubmitDailyCollection($input: SubmitDailyCollectionInput!) {
      submitDailyCollection(input: $input) {
        id
        deliveryWorkerId
        collectionDate
        cashAmountCents
        waterTicketCount
        qrCodeAmountCents
        orderCount
        orderAmountCents
        differenceAmountCents
        status
        createdAt
        updatedAt
      }
    }
  `, { input })
}

// 获取今日收款预计算结果（配送员小程序使用）
export const getTodayCollection = () => {
  return networkManager.query<{ myTodayCollection: MyTodayCollectionVO }>(`
    query GetMyTodayCollection {
      myTodayCollection {
        orderCount
        orderAmountCents
        cashAmountCents
        waterTicketCount
        qrCodeAmountCents
        differenceAmountCents
        calculatedCashAmountCents
        calculatedQrCodeAmountCents
        calculatedWaterTicketCount
        calculatedOrderAmountCents
        status
      }
    }
  `)
}

// 获取对账记录列表
export const getDailyReconciliations = () => {
  return networkManager.query<{ dailyReconciliations: DailyReconciliation[] }>(`
    query GetDailyReconciliations {
      dailyReconciliations {
        id
        reconciliationDate
        totalOrderCount
        totalOrderAmountCents
        totalCollectionAmountCents
        totalCashAmountCents
        totalWaterTicketCount
        totalQrCodeAmountCents
        discrepancyAmountCents
        status
        reviewedAt
        reviewedBy
        reviewNotes
      }
    }
  `)
}