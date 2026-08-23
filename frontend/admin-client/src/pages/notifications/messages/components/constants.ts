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
  /** 微信订阅消息通道投递状态(PENDING/SENT/FAILED,F-NTF-006 失败消息手动重发) */
  channelStatus?: 'PENDING' | 'SENT' | 'FAILED' | null;
  wxMessageId?: string | null;
  errorMessage?: string | null;
  sentAt?: string | null;
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

export const CHANNEL_STATUS_LABELS: Record<string, { label: string; color: string }> = {
  PENDING: { label: '待发送', color: 'default' },
  SENT: { label: '已发送', color: 'green' },
  FAILED: { label: '发送失败', color: 'red' },
};
