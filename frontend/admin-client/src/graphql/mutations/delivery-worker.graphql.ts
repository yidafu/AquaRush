import { gql } from '@apollo/client';

// 创建送水员
export const CREATE_DELIVERY_WORKER_MUTATION = gql`
  mutation CreateDeliveryWorker($input: CreateDeliveryWorkerInput!) {
    createDeliveryWorker(input: $input) {
      id
      name
      phone
      avatarUrl
      onlineStatus
      accountStatus
      isAvailable
      wechatOpenId
      rating
      totalOrders
      completedOrders
      createdAt
    }
  }
`;

// 更新送水员
export const UPDATE_DELIVERY_WORKER_MUTATION = gql`
  mutation UpdateDeliveryWorker($workerId: PrimaryId!, $input: UpdateDeliveryWorkerInput!) {
    updateDeliveryWorker(workerId: $workerId, input: $input) {
      id
      name
      phone
      avatarUrl
      onlineStatus
      accountStatus
      isAvailable
      wechatOpenId
      rating
      totalOrders
      completedOrders
      updatedAt
    }
  }
`;

// 删除送水员(G11 落地,F-USR-021)
export const DELETE_DELIVERY_WORKER_MUTATION = gql`
  mutation DeleteDeliveryWorker($id: PrimaryId!) {
    deleteDeliveryWorker(id: $id)
  }
`;

// 重置送水员密码(G11 落地;newPassword 留空则重置为默认密码)
export const RESET_DELIVERY_WORKER_PASSWORD_MUTATION = gql`
  mutation ResetDeliveryWorkerPassword($id: PrimaryId!, $newPassword: String) {
    resetDeliveryWorkerPassword(id: $id, newPassword: $newPassword)
  }
`;
