import { gql } from '@apollo/client';

export const GET_ORDERS_QUERY = gql`
  query GetOrders($input: OrderListInput) {
    orders(input: $input) {
      content {
        id
        orderNo
        user {
          id
          nickname
          phone
        }
        status
        amountCents
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
      orderNo
      user {
        id
        nickname
        phone
        avatarUrl
      }
      status
      amountCents
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

export const GET_ORDER_OPERATIONS_QUERY = gql`
  query GetOrderOperations($orderId: PrimaryId!) {
    orderOperations(orderId: $orderId) {
      id
      operationType
      operatorId
      operatorType
      description
      extraData
      createdAt
    }
  }
`;
