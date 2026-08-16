import { networkManager } from '../utils/networkManager'

/**
 * 消息中心与订阅消息服务(用户端,PRD 13-notification.md F-NTF-002/001)
 */

export type NotificationType = 'ORDER' | 'SYSTEM' | 'ANNOUNCEMENT' | 'FEEDBACK'

export interface NotificationVO {
  id: string
  type: NotificationType
  scene: string
  title: string
  summary?: string | null
  relatedNo?: string | null
  read: boolean
  readAt?: string | null
  createdAt: string
}

export interface NotificationVOPage {
  content: NotificationVO[]
  totalElements: number
  totalPages: number
  size: number
  number: number
}

export interface SubscribeTemplateVO {
  scene: string
  templateId: string
  enabled: boolean
}

class NotificationService {
  private static instance: NotificationService

  public static getInstance(): NotificationService {
    if (!NotificationService.instance) {
      NotificationService.instance = new NotificationService()
    }
    return NotificationService.instance
  }

  /** 消息中心分页查询(仅本人消息) */
  public async getMyNotifications(type?: NotificationType, page = 0, size = 10): Promise<NotificationVOPage> {
    const query = `
      query MyNotifications($type: NotificationType, $page: Int!, $size: Int!) {
        myNotifications(type: $type, page: $page, size: $size) {
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
    `
    const response = await networkManager.query<{ myNotifications: NotificationVOPage }>(query, {
      type,
      page,
      size
    })
    return (
      response.myNotifications ?? {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size,
        number: page
      }
    )
  }

  /** 未读消息数(我的页角标) */
  public async getUnreadCount(): Promise<number> {
    const query = `
      query UnreadNotificationCount {
        unreadNotificationCount
      }
    `
    const response = await networkManager.query<{ unreadNotificationCount: number }>(query)
    return response.unreadNotificationCount ?? 0
  }

  /** 单条标已读 */
  public async markRead(id: string): Promise<boolean> {
    const mutation = `
      mutation MarkNotificationRead($id: PrimaryId!) {
        markNotificationRead(id: $id)
      }
    `
    const response = await networkManager.mutate<{ markNotificationRead: boolean }>(mutation, { id })
    return response.markNotificationRead ?? false
  }

  /** 全部已读 */
  public async markAllRead(): Promise<boolean> {
    const mutation = `
      mutation MarkAllNotificationsRead {
        markAllNotificationsRead
      }
    `
    const response = await networkManager.mutate<{ markAllNotificationsRead: boolean }>(mutation)
    return response.markAllNotificationsRead ?? false
  }

  /** 已启用的订阅消息模板(下单确认页订阅授权用) */
  public async getSubscribeTemplates(): Promise<SubscribeTemplateVO[]> {
    const query = `
      query SubscribeTemplates {
        subscribeTemplates {
          scene
          templateId
          enabled
        }
      }
    `
    const response = await networkManager.query<{ subscribeTemplates: SubscribeTemplateVO[] }>(query)
    return response.subscribeTemplates ?? []
  }

  /** 记录订阅消息授权结果(仅记录用户接受的模板) */
  public async recordSubscribeAuth(templateIds: string[]): Promise<boolean> {
    if (templateIds.length === 0) {
      return true
    }
    const mutation = `
      mutation RecordSubscribeAuth($templateIds: [String!]!) {
        recordSubscribeAuth(templateIds: $templateIds)
      }
    `
    const response = await networkManager.mutate<{ recordSubscribeAuth: boolean }>(mutation, { templateIds })
    return response.recordSubscribeAuth ?? false
  }
}

export default NotificationService.getInstance()
