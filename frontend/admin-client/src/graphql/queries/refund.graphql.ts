import { gql } from '@apollo/client'

// 退款申请分页查询(R-PAY-02 后台退款管理页,PERMISSION_PAYMENT_READ)
export const REFUND_REQUESTS_PAGE_QUERY = gql`
  query RefundRequestsPage($status: RefundStatus, $dateFrom: LocalDateTime, $dateTo: LocalDateTime, $page: Int, $size: Int) {
    refundRequests(status: $status, dateFrom: $dateFrom, dateTo: $dateTo, page: $page, size: $size) {
      content {
        id
        originalTransactionId
        orderId
        userId
        amountCents
        reason
        status
        requestedAt
        processedAt
        processedBy
        adminNote
      }
      totalElements
      totalPages
      size
      number
    }
  }
`

/** RefundRequestsPage 操作名(供 refetchQueries 引用) */
export const REFUND_REQUESTS_PAGE = 'RefundRequestsPage'
