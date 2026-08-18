import { gql } from '@apollo/client'
import type { DailyCollectionRecord, DailyReconciliation } from '../../types/daily-collection'

// 查询每日收款记录（可选日期筛选）
export const GET_DAILY_COLLECTIONS = gql`
  query GetDailyCollections($collectionDate: String) {
    dailyCollections(collectionDate: $collectionDate) {
      id
      deliveryWorkerId
      collectionDate
      cashAmountCents
      waterTicketCount
      qrCodeAmountCents
      orderCount
      orderAmountCents
      differenceAmountCents
      calculatedCashAmountCents
      calculatedQrCodeAmountCents
      calculatedWaterTicketCount
      calculatedOrderAmountCents
      status
      confirmedAt
      confirmedBy
      notes
      rejectedAt
      rejectedBy
      rejectReason
      createdAt
      updatedAt
    }
  }
`

// 查询所有对账记录
// 已并入 DAILY_COLLECTION_PAGE_DATA_QUERY(收银对账页单页查询,规则 #10)

// 收银对账管理页(DailyCollectionPage)单页查询:
// 一次请求取回「每日收款记录 + 对账记录」(规则 #10)
export const DAILY_COLLECTION_PAGE_DATA_QUERY = gql`
  query DailyCollectionPageData {
    dailyCollections {
      id
      deliveryWorkerId
      collectionDate
      cashAmountCents
      waterTicketCount
      qrCodeAmountCents
      orderCount
      orderAmountCents
      differenceAmountCents
      calculatedCashAmountCents
      calculatedQrCodeAmountCents
      calculatedWaterTicketCount
      calculatedOrderAmountCents
      status
      confirmedAt
      confirmedBy
      notes
      rejectedAt
      rejectedBy
      rejectReason
      createdAt
      updatedAt
    }
    dailyReconciliations {
      id
      reconciliationDate
      totalCashAmountCents
      totalWaterTicketCount
      totalQrCodeAmountCents
      totalOrderCount
      totalOrderAmountCents
      totalCollectionAmountCents
      discrepancyAmountCents
      status
      reportData
      reviewedAt
      reviewedBy
      reviewNotes
      createdAt
      updatedAt
    }
  }
`

// DailyCollectionPageData 操作名(供 refetchQueries 引用)
export const DAILY_COLLECTION_PAGE_DATA = 'DailyCollectionPageData'

export type { DailyCollectionRecord, DailyReconciliation }
