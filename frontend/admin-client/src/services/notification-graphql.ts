import { useLazyQuery, useMutation, useQuery } from '@apollo/client';
import {
  GET_NOTIFICATIONS_QUERY,
  GET_NOTIFICATION_QUERY,
  RESEND_NOTIFICATION_MUTATION,
} from '../graphql/queries/notification.graphql';

/**
 * 通知与消息服务(F-NTF-006,后台消息列表)
 * 筛选:消息类型/接收人类型/关键字/已读状态/时间范围;默认近 30 天由页面层给定。
 */

export interface NotificationFilter {
  messageType?: string | null;
  receiverType?: string | null;
  keyword?: string | null;
  read?: boolean | null;
  startAt?: string | null;
  endAt?: string | null;
}

export const useNotificationsQuery = (variables: NotificationFilter & { page: number; size: number }) => {
  return useQuery(GET_NOTIFICATIONS_QUERY, {
    variables,
    fetchPolicy: 'cache-and-network',
    errorPolicy: 'all',
  });
};

export const useNotificationDetail = (id: string | null) => {
  return useLazyQuery(GET_NOTIFICATION_QUERY, {
    fetchPolicy: 'network-only',
    errorPolicy: 'all',
    variables: { id },
  });
};

/**
 * 手动重发失败/待发送的微信订阅消息(F-NTF-006,仅超管)
 */
export const useResendNotification = () => {
  return useMutation(RESEND_NOTIFICATION_MUTATION, {
    errorPolicy: 'all',
  });
};
