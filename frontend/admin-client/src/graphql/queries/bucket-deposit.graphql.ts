import { gql } from '@apollo/client';

export const GET_BUCKET_DEPOSITS_QUERY = gql`
  query GetBucketDeposits($status: BucketDepositStatus, $userId: Long, $page: Int, $size: Int) {
    allBucketDeposits(status: $status, userId: $userId, page: $page, size: $size) {
      list {
        id
        userId
        quantity
        amountCents
        status
        paymentTransactionId
        paymentTime
        refundedAt
        refundedBy
        remark
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
`;

export const GET_BUCKET_DEPOSIT_AMOUNT_QUERY = gql`
  query GetBucketDepositAmount {
    bucketDepositAmount
  }
`;
