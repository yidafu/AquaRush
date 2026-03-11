import { gql } from '@apollo/client';

export const REFUND_BUCKET_DEPOSIT_MUTATION = gql`
  mutation RefundBucketDeposit($depositId: Long!, $remark: String) {
    refundBucketDeposit(depositId: $depositId, remark: $remark) {
      id
      userId
      quantity
      amountCents
      status
      refundedAt
      refundedBy
      remark
      updatedAt
    }
  }
`;

export const SET_BUCKET_DEPOSIT_AMOUNT_MUTATION = gql`
  mutation SetBucketDepositAmount($amountCents: Long!) {
    setBucketDepositAmount(amountCents: $amountCents)
  }
`;
