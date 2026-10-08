import { gql } from '@apollo/client'

// 查询每日收款记录(按收款日期/送水工筛选,服务端分页 —— 规则 #13)
export const GET_DAILY_COLLECTIONS = gql`
  query GetDailyCollections($collectionDate: String, $deliveryWorkerId: Long, $page: Int, $size: Int) {
    dailyCollections(input: { collectionDate: $collectionDate, deliveryWorkerId: $deliveryWorkerId, page: $page, size: $size }) {
      list {
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
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
  }
`

// 收银对账管理页(DailyCollectionPage)单页查询(规则 #10):
// 一次请求取回「每日收款记录(分页) + 对账记录(分页) + 指定日期汇总卡」
export const DAILY_COLLECTION_PAGE_DATA_QUERY = gql`
  query DailyCollectionPageData(
    $collectionsInput: DailyCollectionListInput
    $reconciliationsInput: DailyReconciliationListInput
    $summaryDate: String
  ) {
    dailyCollections(input: $collectionsInput) {
      list {
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
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
    dailyReconciliations(input: $reconciliationsInput) {
      list {
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
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
    dailyCollectionSummary(collectionDate: $summaryDate) {
      collectionDate
      totalCashAmountCents
      totalQrCodeAmountCents
      totalWaterTicketCount
      totalOrderCount
      totalOrderAmountCents
      totalSubmittedCount
    }
  }
`

// 对账逐单差异明细(G29):按对账日期分页取回当日已送达订单流水(对账详情弹窗按需查询)
export const GET_RECONCILIATION_ORDER_DETAILS = gql`
  query GetReconciliationOrderDetails($reconciliationDate: String!, $deliveryWorkerId: Long, $page: Int!, $size: Int!) {
    dailyReconciliationOrderDetails(
      reconciliationDate: $reconciliationDate
      deliveryWorkerId: $deliveryWorkerId
      page: $page
      size: $size
    ) {
      list {
        orderId
        orderNo
        deliveryWorkerId
        amountCents
        paymentType
        status
        deliveredAt
      }
      pageInfo {
        total
        pageSize
        pageNum
        hasNext
        hasPrevious
        totalPages
      }
    }
  }
`
