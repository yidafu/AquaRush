import http from '../utils/http';

/**
 * API 日志查询参数
 */
export interface ApiLogQueryParams {
  requestType?: 'HTTP' | 'GRAPHQL';
  method?: string;
  responseStatus?: number;
  userId?: number;
  startTime?: string;
  endTime?: string;
  page?: number;
  size?: number;
}

/**
 * 用户操作日志查询参数
 */
export interface UserActionLogQueryParams {
  userId?: string;
  actionType?: string;
  username?: string;
  startTime?: string;
  endTime?: string;
  page?: number;
  size?: number;
}

/**
 * API 日志项
 */
export interface ApiLogItem {
  id: number;
  correlationId?: string;
  requestType: 'HTTP' | 'GRAPHQL';
  operationType?: 'QUERY' | 'MUTATION' | 'SUBSCRIPTION';
  operationName?: string;
  method?: string;
  uri?: string;
  query?: string;
  requestBody?: string;
  responseStatus?: number;
  durationMs?: number;
  ipAddress?: string;
  userAgent?: string;
  userId?: number;
  username?: string;
  errorMessage?: string;
  createdAt: string;
}

/**
 * 用户操作日志项
 */
export interface UserActionLogItem {
  id: number;
  userId?: string;
  username?: string;
  actionType: string;
  target?: string;
  pageUrl?: string;
  elementId?: string;
  elementType?: string;
  elementText?: string;
  clientIp?: string;
  userAgent?: string;
  properties?: string;
  createdAt: string;
}

/**
 * 分页响应 - 与后端 PageImpl 匹配
 */
export interface PageResponse<T> {
  list: T[];
  pageInfo: {
    hasNext: boolean;
    hasPrevious: boolean;
    pageNum: number;
    pageSize: number;
    total: number;
    totalPages: number;
  };
}

/**
 * 查询 API 日志
 */
export const queryApiLogs = (params: ApiLogQueryParams) => {
  return http.get<PageResponse<ApiLogItem>>('/logs/api', { params });
};

/**
 * 业务日志查询参数
 */
export interface BizLogQueryParams {
  level?: string;
  loggerName?: string;
  userId?: number;
  startTime?: string;
  endTime?: string;
  page?: number;
  size?: number;
}

/**
 * 业务日志项
 */
export interface BizLogItem {
  id: number;
  correlationId?: string;
  level: string;
  loggerName?: string;
  message?: string;
  stackTrace?: string;
  userId?: number;
  username?: string;
  createdAt: string;
}

/**
 * 查询用户操作日志
 */
export const queryUserActionLogs = (params: UserActionLogQueryParams) => {
  return http.get<PageResponse<UserActionLogItem>>('/logs/user-actions', { params });
};

/**
 * 查询业务日志
 */
export const queryBusinessLogs = (params: BizLogQueryParams) => {
  return http.get<PageResponse<BizLogItem>>('/logs/business', { params });
};

/**
 * 根据关联ID查询 API 日志
 */
export const getApiLogsByCorrelationId = (correlationId: string) => {
  return http.get<ApiLogItem[]>(`/logs/api/correlation/${correlationId}`);
};
