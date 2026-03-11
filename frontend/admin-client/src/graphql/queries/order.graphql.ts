import { gql } from '@apollo/client';

export const GET_ORDERS_QUERY = gql`
  query GetOrders($input: OrderListInput) {
    orders(input: $input) {
      content {
        id
        orderNumber
        user {
          id
          nickname
          phone
        }
        status
        amount
        address {
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
        }
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_ORDER_DETAIL_QUERY = gql`
  query GetOrderDetail($id: PrimaryId!) {
    order(orderId: $id) {
      id
      orderNumber
      user {
        id
        nickname
        phone
        avatarUrl
      }
      status
      amount
      quantity
      product {
        id
        name
        coverImageUrl
      }
      address {
        receiverName
        phone
        province
        city
        district
        detailAddress
        longitude
        latitude
      }
      deliveryWorker {
        id
        name
        phone
        avatarUrl
      }
      paymentMethod
      paymentTransactionId
      paymentTime
      deliveryPhotos
      isSelfCollect
      paymentType
      deliveryStartedAt
      deliveryConfirmedAt
      completedAt
      remark
      createdAt
      updatedAt
    }
  }
`;

export const GET_ORDERS_BY_STATUS_QUERY = gql`
  query GetOrdersByStatus($status: OrderStatus!) {
    ordersByStatus(status: $status) {
      id
      orderNumber
      user {
        id
        nickname
        phone
      }
      status
      amount
      createdAt
      updatedAt
    }
  }
`;