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
      createdAt
      updatedAt
    }
  }
`

// 查询所有对账记录
export const GET_DAILY_RECONCILIATIONS = gql`
  query GetDailyReconciliations {
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

// 查询指定日期对账记录
export const GET_DAILY_RECONCILIATION = gql`
  query GetDailyReconciliation($reconciliationDate: String!) {
    dailyReconciliation(reconciliationDate: $reconciliationDate) {
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

export type { DailyCollectionRecord, DailyReconciliation }
