import { gql } from '@apollo/client';

export const ASSIGN_DELIVERY_WORKER_MUTATION = gql`
  mutation AssignDeliveryWorker($orderId: PrimaryId!, $workerId: PrimaryId!, $isSelfCollect: Boolean!) {
    assignDeliveryWorker(orderId: $orderId, workerId: $workerId, isSelfCollect: $isSelfCollect) {
      id
      isSelfCollect
      deliveryWorker {
        id
        name
        phone
      }
      status
      updatedAt
    }
  }
`;
