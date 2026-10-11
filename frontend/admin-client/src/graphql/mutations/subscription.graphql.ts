import { gql } from '@apollo/client';

// 创建周期订购计划(F-ORD-026/G77,后台代客下单入口)
export const CREATE_SUBSCRIPTION_MUTATION = gql`
  mutation CreateSubscription($input: CreateSubscriptionInput!) {
    createSubscription(input: $input) {
      id
      status
    }
  }
`;

// 暂停计划(暂停期间不生成订单;可恢复)
export const PAUSE_SUBSCRIPTION_MUTATION = gql`
  mutation PauseSubscription($subscriptionId: PrimaryId!) {
    pauseSubscription(subscriptionId: $subscriptionId) {
      id
      status
    }
  }
`;

// 恢复计划(仅 ACTIVE 之外的已暂停可恢复)
export const RESUME_SUBSCRIPTION_MUTATION = gql`
  mutation ResumeSubscription($subscriptionId: PrimaryId!) {
    resumeSubscription(subscriptionId: $subscriptionId) {
      id
      status
    }
  }
`;

// 终止计划(不可恢复,需新建;前端 Popconfirm 二次确认)
export const TERMINATE_SUBSCRIPTION_MUTATION = gql`
  mutation TerminateSubscription($subscriptionId: PrimaryId!) {
    terminateSubscription(subscriptionId: $subscriptionId) {
      id
      status
    }
  }
`;
