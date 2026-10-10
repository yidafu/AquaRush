import { gql } from '@apollo/client'

// 押金价目分页查询(G06 押金管理-价目表,PERMISSION_PAYMENT_READ)
export const DEPOSIT_PRICES_PAGE_QUERY = gql`
  query DepositPricesPage($status: DepositPriceStatus, $keyword: String, $page: Int, $size: Int) {
    depositPrices(input: { status: $status, keyword: $keyword, page: $page, size: $size }) {
      content {
        id
        specification
        depositCents
        status
        sortOrder
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`

// 押金记录分页查询(G06 押金管理-押金记录,R-MON-03)
export const DEPOSIT_RECORDS_PAGE_QUERY = gql`
  query DepositRecordsPage(
    $userId: PrimaryId
    $status: DepositStatus
    $specification: String
    $page: Int
    $size: Int
  ) {
    depositRecords(
      input: { userId: $userId, status: $status, specification: $specification, page: $page, size: $size }
    ) {
      content {
        id
        orderId
        specification
        bucketCount
        unitDepositCents
        depositAmountCents
        status
        collectedAt
        refundedAt
        refundRemark
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`

// 退押申请分页查询(空桶回收确认与退押审核,F-PRO-010/R-PRO-04)
export const DEPOSIT_REFUND_APPLICATIONS_PAGE_QUERY = gql`
  query DepositRefundApplicationsPage($status: DepositRefundApplicationStatus, $page: Int, $size: Int) {
    depositRefundApplications(status: $status, page: $page, size: $size) {
      content {
        id
        specification
        bucketCount
        depositCents
        receiverName
        receiverPhone
        receiverDetail
        remark
        status
        rejectReason
        recoveredAt
        refundedAt
        rejectedAt
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`

// 桶账台账分页查询(F-PRO-012 按客户的循环水桶台账)
export const BARREL_LEDGER_PAGE_QUERY = gql`
  query BarrelLedgerPage($keyword: String, $onlyOwed: Boolean, $page: Int, $size: Int) {
    barrelLedger(input: { keyword: $keyword, onlyOwed: $onlyOwed, page: $page, size: $size }) {
      content {
        userId
        nickname
        phone
        heldBucketCount
        depositHeldCents
        depositRefundedCents
        deliveredBucketCount
        recoveredBucketCount
        owedBucketCount
      }
      totalElements
      totalPages
      size
      number
    }
  }
`

/** DepositPricesPage 操作名(供 refetchQueries 引用) */
export const DEPOSIT_PRICES_PAGE = 'DepositPricesPage'

/** DepositRecordsPage 操作名(供 refetchQueries 引用) */
export const DEPOSIT_RECORDS_PAGE = 'DepositRecordsPage'

/** DepositRefundApplicationsPage 操作名(供 refetchQueries 引用) */
export const DEPOSIT_REFUND_APPLICATIONS_PAGE = 'DepositRefundApplicationsPage'

/** BarrelLedgerPage 操作名(供 refetchQueries 引用) */
export const BARREL_LEDGER_PAGE = 'BarrelLedgerPage'
