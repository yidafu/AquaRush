import { gql } from '@apollo/client';

/**
 * 通知与消息查询(F-NTF-006,PRD 13-notification.md 后台消息列表)
 */

export const GET_NOTIFICATIONS_QUERY = gql`
  query GetNotifications(
    $messageType: NotificationType
    $receiverType: NotificationReceiverType
    $keyword: String
    $read: Boolean
    $startAt: LocalDateTime
    $endAt: LocalDateTime
    $page: Int
    $size: Int
  ) {
    notifications(
      messageType: $messageType
      receiverType: $receiverType
      keyword: $keyword
      read: $read
      startAt: $startAt
      endAt: $endAt
      page: $page
      size: $size
    ) {
      content {
        id
        type
        scene
        title
        summary
        relatedNo
        read
        readAt
        createdAt
      }
      totalElements
      totalPages
      size
      number
    }
  }
`;

export const GET_NOTIFICATION_QUERY = gql`
  query GetNotification($id: PrimaryId!) {
    notification(id: $id) {
      id
      type
      scene
      title
      summary
      relatedNo
      read
      readAt
      createdAt
      channelStatus
      wxMessageId
      errorMessage
      sentAt
    }
  }
`;

/**
 * 手动重发失败/待发送的微信订阅消息(F-NTF-006,PERMISSION_NOTIFICATION_WRITE 仅超管)
 */
export const RESEND_NOTIFICATION_MUTATION = gql`
  mutation ResendNotification($id: PrimaryId!) {
    resendNotification(id: $id) {
      id
      channelStatus
      wxMessageId
      errorMessage
      sentAt
    }
  }
`;
