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
      isAvailable
      wechatOpenId
      rating
      totalOrders
      completedOrders
      updatedAt
    }
  }
`;

// 删除送水员
export const DELETE_DELIVERY_WORKER_MUTATION = gql`
  mutation DeleteDeliveryWorker($workerId: PrimaryId!) {
    deleteDeliveryWorker(workerId: $workerId)
  }
`;
