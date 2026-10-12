/**
 * Table 纯逻辑内核 —— 排序 / 分页切片 / 行选择 / rowKey 与取值解析。
 * 契约: docs/compose/spec/admin-radix-ui.md [S2.5](TanStack 核心 + antd 形状表壳)。
 * 无 JSX / 无 DOM / 无 stylex 依赖,node 环境 jest 可直接单测(见 table-core.test.ts)。
 */
import type { Key } from 'react';

/** antd rowKey 函数形态(antd 允许 index 可选)。 */
export type RowKeyFn<T> = (record: T, index?: number) => Key;

/** antd rowKey:字段名或函数;缺省语义见 resolveRowKey。 */
export type RowKey<T> = string | RowKeyFn<T>;

/** antd sorter 比较器:返回值语义同 Array.prototype.sort。 */
export type SorterCompare<T> = (a: T, b: T) => number;

/** antd dataSource 取值路径:标量字段名 / 嵌套路径数组。 */
export type DataIndex = string | number | ReadonlyArray<string | number>;

export interface PaginationSliceResult<T> {
  /** 当前页行(已按 antd 规则决定是否切片)。 */
  pageRows: T[];
  /** 总页数(至少 1;服务端模式按 total 计算)。 */
  pageCount: number;
}

/** resolvePagination 读取的 pagination 配置子集(antd 受控/非受控判定字段)。 */
export interface PaginationConfigSlice {
  current?: number;
  pageSize?: number;
  defaultPageSize?: number;
}

export interface ResolvePaginationInput {
  /** Table 的 pagination 配置(false/缺省 → null,完全非受控)。 */
  config: PaginationConfigSlice | null;
  /** 表壳内部 current 状态(仅非受控时生效)。 */
  innerCurrent: number;
  /** 表壳内部 pageSize 状态(仅非受控时生效)。 */
  innerPageSize: number;
}

export interface ResolvedPagination {
  /** 生效页码(非受控 = 内部状态;受控 = config.current 原样)。 */
  current: number;
  /** 生效每页条数(非受控 = 内部状态;受控 = config.pageSize 原样)。 */
  pageSize: number;
  /** config.current 已定义 → current 受控,内部状态不参与渲染。 */
  controlledCurrent: boolean;
  /** config.pageSize 已定义 → pageSize 受控,内部状态不参与渲染。 */
  controlledPageSize: boolean;
}

/**
 * 分页受控/非受控判定(antd 对齐:current 与 pageSize 各自独立判定)。
 * `config.current !== undefined` → 受控(prop 优先);缺省 → 由内部状态驱动。
 * 注意:不做越界收敛 —— 非受控 current 的收敛由 clampCurrent 在渲染期完成。
 */
export function resolvePagination({
  config,
  innerCurrent,
  innerPageSize,
}: ResolvePaginationInput): ResolvedPagination {
  const configCurrent = config?.current;
  const configPageSize = config?.pageSize;
  const controlledCurrent = configCurrent !== undefined;
  const controlledPageSize = configPageSize !== undefined;
  return {
    current: controlledCurrent ? configCurrent : innerCurrent,
    pageSize: controlledPageSize ? configPageSize : innerPageSize,
    controlledCurrent,
    controlledPageSize,
  };
}

/** 表壳内部 pageSize 初始值:config.pageSize ?? config.defaultPageSize ?? 10(antd 初始化语义)。 */
export function initialInnerPageSize(config: PaginationConfigSlice | null | undefined): number {
  return config?.pageSize ?? config?.defaultPageSize ?? 10;
}

/**
 * 非受控 current 渲染期收敛:数据收缩后 current 超出页数时收敛到末页
 * (antd 内部 current 调整的渲染期等价;受控 current 不经过此函数,由调用方原样透传)。
 * pageSize/total 非法(≤0/NaN)时视作单页,收敛到 1(与 paginationSlice 的 pageCount=1 一致)。
 */
export function clampCurrent(current: number, total: number, pageSize: number): number {
  const safePage = Number.isFinite(pageSize) && pageSize > 0 ? pageSize : 0;
  const safeTotal = Number.isFinite(total) && total > 0 ? total : 0;
  if (safePage === 0 || safeTotal === 0) return 1;
  const pageCount = Math.max(1, Math.ceil(safeTotal / safePage));
  if (!Number.isFinite(current)) return 1;
  return Math.min(Math.max(Math.trunc(current), 1), pageCount);
}

export interface PageChangeRequest {
  /** 当前展示页(Pagination 侧已收敛值)。 */
  current: number;
  /** 当前每页条数。 */
  pageSize: number;
  /** 目标页。 */
  nextPage: number;
  /** 目标每页条数(缺省 = 不变)。 */
  nextPageSize?: number;
  /** 数据总量(收敛目标页用)。 */
  total: number;
}

export interface PageChangeResolution {
  /** 有实际变化(current/pageSize 任一变化),才应触发 onChange。 */
  changed: boolean;
  /** 收敛后的目标页。 */
  page: number;
  /** pageSize 变化,应同时触发 onShowSizeChange。 */
  sizeChanged: boolean;
}

/**
 * Pagination 翻页/改每页条数的纯决策(antd rc-pagination 对齐):
 * - 目标页收敛到 [1, ceil(total / nextSize)];
 * - 页码或每页条数均未变 → 不触发任何回调;
 * - pageSize 变化 → onChange 与 onShowSizeChange 都触发(onShowSizeChange 先发)。
 */
export function resolvePageChange({
  current,
  pageSize,
  nextPage,
  nextPageSize,
  total,
}: PageChangeRequest): PageChangeResolution {
  const size = nextPageSize ?? pageSize;
  const pageCount = Math.max(1, Math.ceil(total / size));
  const page = Math.min(Math.max(Math.trunc(nextPage), 1), Math.max(pageCount, 1));
  const sizeChanged = size !== pageSize;
  return { changed: sizeChanged || page !== current, page, sizeChanged };
}

function clampPage(current: number | undefined, pageCount: number): number {
  if (!Number.isFinite(current) || current === undefined) return 1;
  return Math.min(Math.max(Math.trunc(current), 1), Math.max(pageCount, 1));
}

/**
 * 本地排序:复制数组后按比较器排序。
 * JS sort 稳定:等值元素保持原序(与 antd 本地 sorter 行为一致)。
 * sorterFn 缺省(null/undefined)时返回原序副本。
 */
export function applySort<T>(rows: readonly T[], sorterFn?: SorterCompare<T> | null): T[] {
  if (!sorterFn) return [...rows];
  return [...rows].sort((a, b) => sorterFn(a, b));
}

/**
 * 分页切片(规则对齐 antd 6 `InternalTable.getPageData`):
 * - `pageSize` 缺省/非法 → 全量返回,pageCount = 1;
 * - `total` 存在且 `rows.length < total` → 服务端分页模式:rows 已是服务端返回的
 *   单页数据,不再切片(数据量异常地超过 pageSize 时,对齐 antd 警告分支仍按客户端切片);
 * - 其余情况客户端切片;`current` 越界收敛到末页(末页允许不足 pageSize,即溢出尾页)。
 */
export function paginationSlice<T>(
  rows: readonly T[],
  current: number | undefined,
  pageSize: number | undefined,
  total?: number,
): PaginationSliceResult<T> {
  if (!pageSize || pageSize <= 0) {
    return { pageRows: [...rows], pageCount: 1 };
  }
  if (total !== undefined && rows.length < total) {
    const serverPageCount = Math.max(1, Math.ceil(total / pageSize));
    if (rows.length > pageSize) {
      // 配置异常分支(antd 同款 warning):数据小于 total 却大于 pageSize,仍切片
      const dataPageCount = Math.max(1, Math.ceil(rows.length / pageSize));
      const start = (clampPage(current, dataPageCount) - 1) * pageSize;
      return { pageRows: [...rows.slice(start, start + pageSize)], pageCount: serverPageCount };
    }
    return { pageRows: [...rows], pageCount: serverPageCount };
  }
  const pageCount = Math.max(1, Math.ceil(rows.length / pageSize));
  const start = (clampPage(current, pageCount) - 1) * pageSize;
  return { pageRows: [...rows.slice(start, start + pageSize)], pageCount };
}

/**
 * 勾选/取消单个键:在场 → 移除,缺席 → 追加(保持原相对顺序)。
 */
export function toggleKey(selected: readonly Key[], key: Key): Key[] {
  return selected.includes(key) ? selected.filter((k) => k !== key) : [...selected, key];
}

/**
 * 全选/全不选当前页:
 * - checked=true:保留原顺序,追加页内未选中的键(去重);
 * - checked=false:仅移除页内键,跨页已选保留(antd preserveSelectedRowKeys 语义,
 *   本表壳的选择键始终由调用方持有,天然跨页保留)。
 */
export function toggleAll(pageKeys: readonly Key[], selected: readonly Key[], checked: boolean): Key[] {
  if (checked) {
    const next = [...selected];
    const seen = new Set(next);
    for (const k of pageKeys) {
      if (!seen.has(k)) {
        next.push(k);
        seen.add(k);
      }
    }
    return next;
  }
  const drop = new Set(pageKeys);
  return selected.filter((k) => !drop.has(k));
}

/**
 * 解析行键(antd rowKey 语义):
 * - 函数 → 调用 (record, index);
 * - 字符串 → 取该字段值;
 * - 缺省/字段缺失 → 回落 record.key,再回落行内下标(antd 默认 key 属性语义)。
 */
export function resolveRowKey<T>(row: T, index: number, rowKey?: RowKey<T>): Key {
  if (typeof rowKey === 'function') return rowKey(row, index);
  const source = row as unknown as Record<string, unknown>;
  if (typeof rowKey === 'string') {
    const value = source[rowKey];
    if (value !== undefined && value !== null) return value as Key;
  }
  const fallback = source['key'];
  if (fallback !== undefined && fallback !== null) return fallback as Key;
  return index;
}

/**
 * 按 antd dataIndex 取单元格值(语义对齐 @rc-component/table 的 get:
 * 空路径返回整行 record;标量直取;数组逐段嵌套)。
 * `render(value, record, index)` 的首个参数即本函数结果。
 */
export function resolveDataIndex<T>(record: T, dataIndex?: DataIndex): unknown {
  if (dataIndex === undefined || dataIndex === null || dataIndex === '') return record;
  const path: ReadonlyArray<string | number> = Array.isArray(dataIndex) ? dataIndex : [dataIndex];
  let current: unknown = record;
  for (const segment of path) {
    if (current === null || current === undefined) return undefined;
    current = (current as Record<string | number, unknown>)[segment];
  }
  return current;
}
