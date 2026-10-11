import { gql } from '@apollo/client';

/**
 * 周期订购计划列表页(SubscriptionsPageData)单页查询:
 * 一次请求取回「计划分页 + 送水工列表(新建计划绑定下拉)」,满足
 * AGENTS.md 规则 7 —— 一个页面的一次渲染只发起一个 GraphQL 请求。
 */
export const SUBSCRIPTIONS_PAGE_DATA_QUERY = gql`
  query SubscriptionsPageData($input: SubscriptionListInput) {
    subscriptions(input: $input) {
      content {
        id
        user {
          id
          nickname
          phone
        }
        address {
          receiverName
          phone
          province
          city
          district
          detailAddress
        }
        workerId
        workerName
        cycleType
        weekdays
        monthDay
        timeSlot
        startDate
        endDate
        nextRunDate
        maxPeriods
        executedPeriods
        status
        remark
        lastError
        lastRunAt
        items {
          productId
          productName
          quantity
        }
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
    deliveryWorkers(page: 0, size: 100, role: "DELIVERY_WORKER") {
      content {
        id
        name
        phone
      }
      totalElements
    }
  }
`;

/** SubscriptionsPageData 操作名(供 refetchQueries 引用) */
export const SUBSCRIPTIONS_PAGE_DATA = 'SubscriptionsPageData';

/** 新建计划:按选中用户拉地址簿(选用户后触发,独立于列表单页查询) */
export const SUBSCRIPTION_ADDRESSES_QUERY = gql`
  query SubscriptionAddresses($userId: PrimaryId!) {
    userAddresses(userId: $userId) {
      id
      receiverName
      phone
      province
      city
      district
      detailAddress
      isDefault
    }
  }
`;
