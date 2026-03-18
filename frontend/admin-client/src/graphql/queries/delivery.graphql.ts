import { gql } from '@apollo/client';

// 查询待派单订单
export const GET_PENDING_DELIVERY_ORDERS_QUERY = gql`
  query GetPendingDeliveryOrders {
    pendingDeliveryOrders {
      id
      orderNo
      quantity
      amount
      status
      isSelfCollect
      createdAt
      user {
        id
        nickname
        phone
      }
      address {
        id
        province
        city
        district
        detailAddress
        receiverName
        phone
      }
      product {
        id
        name
      }
    }
  }
`;

// 查询在线配送员
export const GET_ONLINE_DELIVERY_WORKERS_QUERY = gql`
  query GetOnlineDeliveryWorkers {
    onlineDeliveryWorkers {
      id
      name
      phone
      onlineStatus
      currentTaskCount
    }
  }
`;

// 查询所有配送员
export const GET_ALL_DELIVERY_WORKERS_QUERY = gql`
  query GetAllDeliveryWorkers {
    deliveryWorkers {
      id
      name
      phone
      onlineStatus
      totalOrders
      completedOrders
    }
  }
`;

// 查询配送员已接单订单
export const GET_ASSIGNED_ORDERS_QUERY = gql`
  query GetAssignedOrders($workerId: PrimaryId!) {
    assignedOrders(workerId: $workerId) {
      id
      orderNo
      quantity
      amount
      status
      isSelfCollect
      deliveryStartedAt
      createdAt
      user {
        id
        nickname
        phone
      }
      address {
        id
        detailAddress
        receiverName
        phone
      }
    }
  }
`;

// 查询配送员配送中订单
export const GET_DELIVERING_ORDERS_QUERY = gql`
  query GetDeliveringOrders($workerId: PrimaryId!) {
    deliveringOrders(workerId: $workerId) {
      id
      orderNo
      quantity
      amount
      status
      isSelfCollect
      deliveryStartedAt
      deliveryConfirmedAt
      createdAt
      user {
        id
        nickname
        phone
      }
      address {
        id
        detailAddress
        receiverName
        phone
      }
    }
  }
`;

// 查询当日统计数据
export const GET_TODAY_STATISTICS_QUERY = gql`
  query GetTodayStatistics($workerId: PrimaryId) {
    todayStatistics(workerId: $workerId) {
      totalOrders
      completedOrders
      pendingOrders
      earningCents
    }
  }
`;
