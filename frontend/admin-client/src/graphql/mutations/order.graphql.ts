import { gql } from '@apollo/client';
import {} from '@aquarush/common'
export const UPDATE_ORDER_STATUS_MUTATION = gql`
  mutation UpdateOrderStatus($orderId: PrimaryId!, $status: OrderStatus!) {
    updateOrderStatus(orderId: $orderId, status: $status) {
      id
      status
      updatedAt
    }
  }
`;

export const ASSIGN_DELIVERY_WORKER_MUTATION = gql`
  mutation AssignDeliveryWorker($orderId: PrimaryId!, $deliveryWorkerId: PrimaryId!) {
    assignDeliveryWorker(orderId: $orderId, deliveryWorkerId: $deliveryWorkerId) {
      id
      deliveryWorker {
        id
        nickname
        phone
      }
      status
      updatedAt
    }
  }
`;

export const PROCESS_REFUND_MUTATION = gql`
  mutation ProcessRefund($input: ProcessRefundInput!) {
    processRefund(input: $input) {
      id
      status
      refundAmount
      processedAt
    }
  }
`;
