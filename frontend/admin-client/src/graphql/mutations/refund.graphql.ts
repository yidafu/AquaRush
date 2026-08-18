import { gql } from '@apollo/client'

// 审核退款申请(R-PAY-02:APPROVE=执行退款并流转订单「已退款」;REJECT=驳回终止;PERMISSION_PAYMENT_REFUND)
export const PROCESS_REFUND_MUTATION = gql`
  mutation ProcessRefund($input: ProcessRefundInput!) {
    processRefund(input: $input) {
      id
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
  }
`
