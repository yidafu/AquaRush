import { gql } from '@apollo/client';

export const GET_USERS_QUERY = gql`
  query GetUsers($input: UserListInput) {
    users(input: $input) {
      content {
        id
        wechatOpenId
        nickname
        phone
        avatarUrl
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
      first
      last
      empty
    }
  }
`;

export const GET_USER_DETAIL_QUERY = gql`
query GetUserDetail($id: Long!) {
  user(id: $id) {
    id
    wechatOpenId
    nickname
    phone
    avatarUrl
    createdAt
    updatedAt
    __typename
  }
  userAddresses(userId: $id) {
    id
    province
    provinceCode
    city
    cityCode
    district
    districtCode
    receiverName
    phone
    detailAddress
    longitude
    latitude
    isDefault
  }
}
`;

export const GET_ADMINS_QUERY = gql`
  query GetAdmins {
    admins {
      id
      username
      realName
      phone
      role
      lastLoginAt
      createdAt
      updatedAt
    }
  }
`;

export const GET_DELIVERY_WORKERS_QUERY = gql`
  query GetDeliveryWorkers {
    deliveryWorkers {
      id
      userId
      wechatOpenId
      name
      phone
      avatarUrl
      onlineStatus
      rating
      totalOrders
      completedOrders
      averageRating
      isAvailable
      createdAt
      updatedAt
    }
  }
`;

export const DELIVERY_WORKER_DETAIL_QUERY = gql`
  query GetDeliveryWorkerDetail($id: Long!) {
    deliveryWorker(id: $id) {
      id
      userId
      wechatOpenId
      name
      phone
      avatarUrl
      onlineStatus
      rating
      totalOrders
      completedOrders
      averageRating
      isAvailable
      coordinates
      currentLocation
      earning
      createdAt
      updatedAt
    }
  }
`;

export const DELIVERY_WORKER_STATISTICS_QUERY = gql`
  query GetDeliveryWorkerStatistics($deliveryWorkerId: Long!) {
    deliveryWorkerStatistics(deliveryWorkerId: $deliveryWorkerId) {
      deliveryWorkerId
      workerName
      averageRating
      totalReviews
      fiveStarReviews
      fourStarReviews
      threeStarReviews
      twoStarReviews
      oneStarReviews
      ratingDistribution
      lastUpdated
    }
  }
`;

export const DELIVERY_WORKER_ORDERS_QUERY = gql`
  query GetDeliveryWorkerOrders($deliveryWorkerId: Long!, $status: OrderStatus!) {
    ordersByUserAndStatus(userId: $deliveryWorkerId, status: $status) {
      id
      orderNumber
      status
      amount
      quantity
      createdAt
      updatedAt
      completedAt
      paymentMethod
      paymentTime
      deliveryPhotos
      product {
        id
        name
        price
        coverImageUrl
      }
      user {
        id
        nickname
        phone
        avatarUrl
      }
      address {
        id
        receiverName
        phone
        province
        city
        district
        detailAddress
      }
      deliveryWorker {
        id
        name
        phone
        avatarUrl
      }
    }
  }
`;

export const USER_ORDERS_QUERY = gql`
  query GetUserOrders($userId: Long!) {
    ordersByUser(userId: $userId) {
      id
      orderNumber
      status
      amount
      quantity
      createdAt
      updatedAt
      completedAt
      paymentMethod
      paymentTime
      deliveryPhotos
      product {
        id
        name
        price
        coverImageUrl
      }
      user {
        id
        nickname
        phone
        avatarUrl
      }
      address {
        id
        receiverName
        phone
        province
        city
        district
        detailAddress
      }
      deliveryWorker {
        id
        name
        phone
        avatarUrl
      }
    }
  }
`;
