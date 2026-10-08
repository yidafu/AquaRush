import { gql } from '@apollo/client'

// 新增水桶费价目(G06 价目表;PERMISSION_PAYMENT_WRITE)
export const CREATE_BARREL_FEE_MUTATION = gql`
  mutation CreateBarrelFee($input: CreateBarrelFeeInput!) {
    createBarrelFee(input: $input) {
      id
      specification
      priceCents
      status
      sortOrder
    }
  }
`

// 编辑水桶费价目(G06;PERMISSION_PAYMENT_WRITE)
export const UPDATE_BARREL_FEE_MUTATION = gql`
  mutation UpdateBarrelFee($id: PrimaryId!, $input: UpdateBarrelFeeInput!) {
    updateBarrelFee(id: $id, input: $input) {
      id
      specification
      priceCents
      status
      sortOrder
    }
  }
`

// 停用水桶费价目(G06;PERMISSION_PAYMENT_WRITE)
export const DISABLE_BARREL_FEE_MUTATION = gql`
  mutation DisableBarrelFee($id: PrimaryId!) {
    disableBarrelFee(id: $id) {
      id
      specification
      priceCents
      status
    }
  }
`

// 退还水桶费(G06:管理员手动退还,执行退款并置「已退还」;PERMISSION_PAYMENT_REFUND)
export const REFUND_BARREL_PURCHASE_MUTATION = gql`
  mutation RefundBarrelPurchase($purchaseId: PrimaryId!, $remark: String) {
    refundBarrelPurchase(purchaseId: $purchaseId, remark: $remark) {
      id
      orderNo
      userId
      totalAmountCents
      paymentStatus
      paidAt
      refundedAt
      refundedBy
      refundRemark
      updatedAt
    }
  }
`
