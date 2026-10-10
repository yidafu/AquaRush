import { gql } from '@apollo/client'

// 押金价目新增/编辑合一(G06 设计 §5.1 upsertDepositPrice;id 空 = 新增,规格唯一冲突由后端拦截)
export const UPSERT_DEPOSIT_PRICE_MUTATION = gql`
  mutation UpsertDepositPrice($input: UpsertDepositPriceInput!) {
    upsertDepositPrice(input: $input) {
      id
      specification
      depositCents
      status
      sortOrder
    }
  }
`

// 空桶回收确认(F-ORD-011:退押申请 PENDING → RECOVERED,送水工回收空桶后管理员确认)
export const CONFIRM_BARREL_RECOVERED_MUTATION = gql`
  mutation ConfirmBarrelRecovered($applicationId: PrimaryId!) {
    confirmBarrelRecovered(applicationId: $applicationId) {
      id
      status
      recoveredAt
    }
  }
`

// 审核通过退押(RECOVERED → REFUNDED,按完好桶数全额退还押金,R-MON-03)
export const APPROVE_DEPOSIT_REFUND_MUTATION = gql`
  mutation ApproveDepositRefund($applicationId: PrimaryId!) {
    approveDepositRefund(applicationId: $applicationId) {
      id
      status
      refundedAt
      depositCents
    }
  }
`

// 驳回退押申请(PENDING/RECOVERED → REJECTED,必填驳回原因)
export const REJECT_DEPOSIT_REFUND_MUTATION = gql`
  mutation RejectDepositRefund($applicationId: PrimaryId!, $reason: String!) {
    rejectDepositRefund(applicationId: $applicationId, reason: $reason) {
      id
      status
      rejectReason
      rejectedAt
    }
  }
`
