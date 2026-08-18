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

// 配送照片核验(G23/F-ORD-021):后台「通过/驳回」核验流(PERMISSION_ORDER_REVIEW)
export const REVIEW_DELIVERY_PHOTO_MUTATION = gql`
  mutation ReviewDeliveryPhoto($orderId: PrimaryId!, $action: DeliveryPhotoReviewAction!, $remark: String) {
    reviewDeliveryPhoto(orderId: $orderId, action: $action, remark: $remark) {
      id
      deliveryPhotos
      deliveryPhotoReviewStatus
      deliveryPhotoReviewedAt
      deliveryPhotoReviewedBy
      deliveryPhotoReviewRemark
      status
      updatedAt
    }
  }
`;
