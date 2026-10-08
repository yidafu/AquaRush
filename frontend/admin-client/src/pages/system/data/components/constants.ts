import { LogBackupStatus, LogTableType, SystemSettingKeys } from '@aquarush/common';

/** 数据维护页(F-SYS-006)共享常量与格式化工具 */

export interface DataMaintenanceValues {
  apiLogRetentionDays?: number;
  userActionLogRetentionDays?: number;
  businessLogRetentionDays?: number;
  cleanupEnabled?: boolean;
}

// 键 → 表单字段映射(种子默认值兜底:90/90/90/true)
export const FIELD_KEYS: Record<keyof DataMaintenanceValues, string> = {
  apiLogRetentionDays: SystemSettingKeys.LOG_RETENTION_API_DAYS,
  userActionLogRetentionDays: SystemSettingKeys.LOG_RETENTION_USER_ACTION_DAYS,
  businessLogRetentionDays: SystemSettingKeys.LOG_RETENTION_BUSINESS_DAYS,
  cleanupEnabled: SystemSettingKeys.LOG_CLEANUP_ENABLED,
};

export const DEFAULTS: Record<keyof DataMaintenanceValues, number | boolean> = {
  apiLogRetentionDays: 90,
  userActionLogRetentionDays: 90,
  businessLogRetentionDays: 90,
  cleanupEnabled: true,
};

export const TABLE_TABS: { key: LogTableType; label: string }[] = [
  { key: LogTableType.API_LOG, label: 'API 日志' },
  { key: LogTableType.USER_ACTION_LOG, label: '用户操作日志' },
  { key: LogTableType.BUSINESS_LOG, label: '业务日志' },
];

export const LOG_TABLE_LABEL: Record<LogTableType, string> = {
  [LogTableType.API_LOG]: 'API 日志',
  [LogTableType.USER_ACTION_LOG]: '用户操作日志',
  [LogTableType.BUSINESS_LOG]: '业务日志',
};

export const BACKUP_STATUS_CONFIG: Record<LogBackupStatus, { label: string; color: string }> = {
  [LogBackupStatus.RUNNING]: { label: '进行中', color: 'blue' },
  [LogBackupStatus.SUCCEEDED]: { label: '成功', color: 'green' },
  [LogBackupStatus.FAILED]: { label: '失败', color: 'red' },
};

export const formatBytes = (bytes?: number | null): string => {
  if (bytes == null) return '-';
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(2)} MB`;
};

export const formatDateTime = (date?: string | null): string => (date ? new Date(date).toLocaleString('zh-CN') : '-');
