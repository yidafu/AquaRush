import { networkManager } from '@/utils/network-manager';

/**
 * 送水工端消息中心服务(PRD 13-notification.md F-NTF-004,走 aqua-admin GraphQL)
 * 仅本人消息(后端按登录账号 id 强制隔离)。
 */

export type WorkerNotificationType = 'ORDER' | 'SYSTEM' | 'ANNOUNCEMENT' | 'FEEDBACK';

export interface WorkerNotificationVO {
  id: string;
  type: WorkerNotificationType;
  scene: string;
  title: string;
  summary?: string | null;
  relatedNo?: string | null;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}

export interface WorkerNotificationVOPage {
  content: WorkerNotificationVO[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}

export const notificationService = {
  /** 送水工端消息中心分页查询(仅本人) */
  async getMyNotifications(page = 0, size = 10): Promise<WorkerNotificationVOPage> {
    const response = await networkManager.query<{ myWorkerNotifications: WorkerNotificationVOPage }>(`
      query MyWorkerNotifications($page: Int!, $size: Int!) {
        myWorkerNotifications(page: $page, size: $size) {
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
    `, { page, size });
    return (
      response.myWorkerNotifications ?? {
        content: [],
        totalElements: 0,
        totalPages: 0,
        size,
        number: page,
      }
    );
  },

  /** 未读消息数 */
  async getUnreadCount(): Promise<number> {
    const response = await networkManager.query<{ workerUnreadNotificationCount: number }>(`
      query {
        workerUnreadNotificationCount
      }
    `);
    return response.workerUnreadNotificationCount ?? 0;
  },

  /** 单条标已读 */
  async markRead(id: string): Promise<boolean> {
    const response = await networkManager.mutate<{ markWorkerNotificationRead: boolean }>(`
      mutation MarkWorkerNotificationRead($id: PrimaryId!) {
        markWorkerNotificationRead(id: $id)
      }
    `, { id });
    return response.markWorkerNotificationRead ?? false;
  },

  /** 全部已读 */
  async markAllRead(): Promise<boolean> {
    const response = await networkManager.mutate<{ markAllWorkerNotificationsRead: boolean }>(`
      mutation {
        markAllWorkerNotificationsRead
      }
    `);
    return response.markAllWorkerNotificationsRead ?? false;
  },
};
