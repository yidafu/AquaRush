import { gql } from '@apollo/client';

export const GET_ORDERS_QUERY = gql`
  query GetOrders($input: OrderListInput) {
    orders(input: $input) {
      content {
        id
        orderNo
        userId
        user {
          id
          nickname
          phone
        }
        status
        totalAmount
        paymentStatus
        deliveryAddress {
          receiverName
          phone
          province
          city
          district
          detailAddress
        }
        deliveryWorker {
          id
          nickname
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
    order(id: $id) {
      id
      orderNo
      userId
      user {
        id
        nickname
        phone
        avatarUrl
      }
      status
      totalAmount
      paymentStatus
      deliveryAddress {
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
        nickname
        phone
        avatarUrl
      }
      items {
        id
        productId
        product {
          id
          name
          imageUrl
        }
        quantity
        unitPrice
        totalPrice
      }
      payment {
        id
        paymentMethod
        transactionId
        amount
        status
        paidAt
      }
      createdAt
      updatedAt
    }
  }
`;

export const GET_ORDERS_BY_STATUS_QUERY = gql`
  query GetOrdersByStatus($status: OrderStatus!) {
    ordersByStatus(status: $status) {
      id
      orderNo
      userId
      user {
        id
        nickname
        phone
      }
      status
      totalAmount
      paymentStatus
      createdAt
      updatedAt
    }
  }
`;