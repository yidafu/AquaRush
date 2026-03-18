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

export const BATCH_ASSIGN_DELIVERY_WORKER_MUTATION = gql`
  mutation BatchAssignDeliveryWorker($orderIds: [PrimaryId!]!, $workerId: PrimaryId!) {
    batchAssignOrders(orderIds: $orderIds, workerId: $workerId) {
      id
      isSelfCollect
      deliveryWorker {
        id
        name
        phone
      }
      status
    }
  }
`;

export const ACCEPT_DELIVERY_MUTATION = gql`
  mutation AcceptDelivery($orderId: PrimaryId!, $workerId: PrimaryId!) {
    acceptDelivery(orderId: $orderId, workerId: $workerId) {
      id
      status
    }
  }
`;

export const START_DELIVERY_MUTATION = gql`
  mutation StartDelivery($orderId: PrimaryId!) {
    startDelivery(orderId: $orderId) {
      id
      status
      deliveryStartedAt
    }
  }
`;

export const COMPLETE_DELIVERY_MUTATION = gql`
  mutation CompleteDelivery($orderId: PrimaryId!, $photos: [String!]!, $paymentType: PaymentType, $remark: String) {
    completeDelivery(orderId: $orderId, photos: $photos, paymentType: $paymentType, remark: $remark) {
      id
      status
      deliveryConfirmedAt
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
