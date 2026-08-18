import { gql } from '@apollo/client'

// 水桶费价目分页查询(G06 水桶费管理-价目表,PERMISSION_PAYMENT_READ)
export const BARREL_FEES_PAGE_QUERY = gql`
  query BarrelFeesPage($status: BarrelFeeStatus, $keyword: String, $page: Int, $size: Int) {
    barrelFees(input: { status: $status, keyword: $keyword, page: $page, size: $size }) {
      content {
        id
        specification
        priceCents
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

// 购桶单分页查询(G06 水桶费管理-购桶记录,PERMISSION_PAYMENT_READ)
export const BARREL_PURCHASES_PAGE_QUERY = gql`
  query BarrelPurchasesPage($status: BarrelPaymentStatus, $dateFrom: LocalDateTime, $dateTo: LocalDateTime, $page: Int, $size: Int) {
    barrelPurchases(input: { status: $status, dateFrom: $dateFrom, dateTo: $dateTo, page: $page, size: $size }) {
      content {
        id
        orderNo
        userId
        totalAmountCents
        paymentStatus
        paidAt
        refundedAt
        refundedBy
        refundRemark
        remark
        items {
          specification
          unitPriceCents
          quantity
          amountCents
        }
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

/** BarrelFeesPage 操作名(供 refetchQueries 引用) */
export const BARREL_FEES_PAGE = 'BarrelFeesPage'

/** BarrelPurchasesPage 操作名(供 refetchQueries 引用) */
export const BARREL_PURCHASES_PAGE = 'BarrelPurchasesPage'
