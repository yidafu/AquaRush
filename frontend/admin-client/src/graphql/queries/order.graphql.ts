import { gql } from '@apollo/client';

/**
 * 订单列表页(OrdersPage)单页查询:
 * 一次请求取回「订单分页 + 在线配送员(批量派单下拉)」,满足
 * CLAUDE.md 规则 #10 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const ORDERS_PAGE_DATA_QUERY = gql`
  query OrdersPageData($input: OrderListInput) {
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
        items {
          productId
          quantity
          product {
            id
            name
            coverImageUrl
          }
        }
        createdAt
        updatedAt
      }
      totalElements
      totalPages
      size
      number
    }
    onlineDeliveryWorkers {
      id
      name
      phone
      onlineStatus
      currentTaskCount
    }
  }
`;

/** OrdersPageData 操作名(供 refetchQueries 引用) */
export const ORDERS_PAGE_DATA = 'OrdersPageData';

/**
 * 订单详情页(OrderDetailPage)单页查询:
 * 一次请求取回「订单详情 + 在线配送员(派单下拉)+ 操作记录时间线」,满足
 * CLAUDE.md 规则 #10 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const ORDER_DETAIL_PAGE_DATA_QUERY = gql`
  query OrderDetailPageData($orderId: PrimaryId!) {
    order(orderId: $orderId) {
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
      items {
        productId
        quantity
        unitPriceCents
        totalPriceCents
        product {
          id
          name
          coverImageUrl
          specification
          priceCents
        }
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
    onlineDeliveryWorkers {
      id
      name
      phone
      onlineStatus
      currentTaskCount
    }
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

/** OrderDetailPageData 操作名(供 refetchQueries 引用) */
export const ORDER_DETAIL_PAGE_DATA = 'OrderDetailPageData';