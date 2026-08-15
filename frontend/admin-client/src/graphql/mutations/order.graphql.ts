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

// 取消订单(F-ORD-006,后台):未支付即时取消恢复库存;已支付(待派单)取消自动退款
export const CANCEL_ORDER_MUTATION = gql`
  mutation CancelOrder($orderId: PrimaryId!) {
    cancelOrder(orderId: $orderId) {
      id
      orderNo
      status
      updatedAt
    }
  }
`;

// 批量派单(F-ORD-022,G10):多选待派单订单一次派给同一送水工
export const BATCH_ASSIGN_ORDERS_MUTATION = gql`
  mutation BatchAssignOrders($orderIds: [PrimaryId!]!, $workerId: PrimaryId!) {
    batchAssignOrders(orderIds: $orderIds, workerId: $workerId) {
      id
      status
      deliveryWorker {
        id
        name
        phone
      }
      updatedAt
    }
  }
`;
