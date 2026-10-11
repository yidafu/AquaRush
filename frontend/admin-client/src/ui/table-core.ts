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
