import { gql } from '@apollo/client'

// 确认每日收款
export const CONFIRM_DAILY_COLLECTION = gql`
  mutation ConfirmDailyCollection($input: ConfirmDailyCollectionInput!) {
    confirmDailyCollection(input: $input) {
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

// 驳回每日收款(G28 争议处理:驳回退回草稿,配送员可改可重提)
export const REJECT_DAILY_COLLECTION = gql`
  mutation RejectDailyCollection($input: RejectDailyCollectionInput!) {
    rejectDailyCollection(input: $input) {
      id
      deliveryWorkerId
      collectionDate
      status
      rejectedAt
      rejectedBy
      rejectReason
      createdAt
      updatedAt
    }
  }
`

// 复核对账记录
export const REVIEW_RECONCILIATION = gql`
  mutation ReviewReconciliation($input: ReviewReconciliationInput!) {
    reviewReconciliation(input: $input) {
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
      reviewedAt
      reviewedBy
      reviewNotes
      createdAt
      updatedAt
    }
  }
`

// 手动执行对账
export const EXECUTE_DAILY_RECONCILIATION = gql`
  mutation ExecuteDailyReconciliation($reconciliationDate: String) {
    executeDailyReconciliation(reconciliationDate: $reconciliationDate) {
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
      createdAt
      updatedAt
    }
  }
`