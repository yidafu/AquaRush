/** 业务日志页共享常量与类型 */

export const LOG_LEVEL_OPTIONS = [
  { value: 'TRACE', label: 'TRACE', color: 'default' },
  { value: 'DEBUG', label: 'DEBUG', color: 'green' },
  { value: 'INFO', label: 'INFO', color: 'blue' },
  { value: 'WARN', label: 'WARN', color: 'orange' },
  { value: 'ERROR', label: 'ERROR', color: 'red' },
];

export const DOMAIN_OPTIONS = [
  { value: 'ORDER', label: '订单' },
  { value: 'PAYMENT', label: '支付' },
  { value: 'DELIVERY', label: '配送' },
  { value: 'USER', label: '用户' },
  { value: 'AUTH', label: '认证' },
  { value: 'PRODUCT', label: '产品' },
  { value: 'MARKETING', label: '营销' },
  { value: 'OPS', label: '运营配置' },
  { value: 'RECONCILIATION', label: '对账' },
];

export const getLogLevelInfo = (level: string) => {
  return LOG_LEVEL_OPTIONS.find(opt => opt.value === level) || { label: level, color: 'default' };
};

export interface BizLogFilters {
  domain?: string;
  operationType?: string;
  level?: string;
  userId?: number;
  startTime?: string;
  endTime?: string;
}
