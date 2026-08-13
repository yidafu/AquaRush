import type { NoticeStatus } from '@aquarush/common';

// 公告类型文案
export const NOTICE_TYPE_LABELS: Record<string, string> = {
  PLATFORM_NOTICE: '平台公告',
  WATER_OUTAGE: '停水通知',
  ACTIVITY_PREVIEW: '活动预告',
};

export const NOTICE_TYPE_COLORS: Record<string, string> = {
  PLATFORM_NOTICE: 'blue',
  WATER_OUTAGE: 'orange',
  ACTIVITY_PREVIEW: 'purple',
};

export const NOTICE_STATUS_LABELS: Record<NoticeStatus, string> = {
  DRAFT: '草稿',
  PUBLISHED: '已发布',
  STOPPED: '已停用',
};

export const NOTICE_STATUS_COLORS: Record<NoticeStatus, string> = {
  DRAFT: 'default',
  PUBLISHED: 'green',
  STOPPED: 'red',
};
