import type { TablePaginationConfig } from '@aquarush/admin-ui';

/**
 * 表格分页工厂 —— 统一全项目 Table pagination 的 showSizeChanger/showQuickJumper/
 * showTotal 样板。
 *
 * - showTotal 默认文案:共 ${total} 条;
 * - showTotal: 'range' 时改为「第 a-b 条/共 N 条」(常用于用户/明细类列表);
 * - 其余字段(如 onChange/current/pageSize/pageSizeOptions)经 opts 透传。
 */
export type PaginationShowTotalVariant = 'count' | 'range';

export interface PaginationOptions {
  current?: number;
  pageSize?: number;
  pageSizeOptions?: string[];
  defaultPageSize?: number;
  showSizeChanger?: boolean;
  showQuickJumper?: boolean;
  /** showTotal 文案变体;false = 不显示 total */
  showTotal?: PaginationShowTotalVariant | false;
  onChange?: (page: number, pageSize: number) => void;
}

export function pagination(total: number, opts: PaginationOptions = {}): TablePaginationConfig {
  const { showTotal = 'count', ...rest } = opts;
  const config: TablePaginationConfig = { total, ...rest };

  if (showTotal === 'range') {
    config.showTotal = (t, range) =>
      range[0] && range[1] ? `第 ${range[0]}-${range[1]} 条/共 ${t} 条` : `共 ${t} 条`;
  } else if (showTotal !== false) {
    config.showTotal = (t) => `共 ${t} 条`;
  }
  return config;
}
