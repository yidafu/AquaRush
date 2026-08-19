/** 后台消息列表页(F-NTF-006)共享常量与类型 */

export interface NotificationRecord {
  id: string;
  receiverType: 'USER' | 'WORKER';
  type: 'ORDER' | 'SYSTEM' | 'ANNOUNCEMENT' | 'FEEDBACK';
  scene: string;
  title: string;
  summary?: string | null;
  relatedNo?: string | null;
  read: boolean;
  readAt?: string | null;
  createdAt: string;
}

export const TYPE_LABELS: Record<string, { label: string; color: string }> = {
  ORDER: { label: '订单', color: 'blue' },
  SYSTEM: { label: '系统', color: 'default' },
  ANNOUNCEMENT: { label: '公告', color: 'green' },
  FEEDBACK: { label: '反馈', color: 'orange' },
};

export const RECEIVER_LABELS: Record<string, string> = {
  USER: '用户',
  WORKER: '送水工',
};
