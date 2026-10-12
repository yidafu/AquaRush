/**
 * table-core 单测:排序 / 分页切片(含溢出末页与服务端模式)/ 选择开关 / rowKey 解析。
 * 纯 node 环境(无 DOM),对应用例覆盖 S2.5 验收的 Table 逻辑子集。
 */
import { describe, expect, it } from '@jest/globals';
import {
  applySort,
  clampCurrent,
  initialInnerPageSize,
  paginationSlice,
  resolveDataIndex,
  resolvePageChange,
  resolvePagination,
  resolveRowKey,
  toggleAll,
  toggleKey,
} from './table-core';

interface NumRow {
  id: number;
  name: string;
  nested?: { value?: number };
  key?: string;
}

const rows = (): NumRow[] => [
  { id: 3, name: 'c' },
  { id: 1, name: 'a' },
  { id: 2, name: 'b' },
];

describe('applySort', () => {
  it('按比较器升序排序且不修改原数组', () => {
    const input = rows();
    const output = applySort(input, (a, b) => a.id - b.id);
    expect(output.map((r) => r.id)).toEqual([1, 2, 3]);
    expect(input.map((r) => r.id)).toEqual([3, 1, 2]);
    expect(output).not.toBe(input);
  });

  it('传入反向比较器得到降序', () => {
    const output = applySort(rows(), (a, b) => b.id - a.id);
    expect(output.map((r) => r.id)).toEqual([3, 2, 1]);
  });

  it('比较器缺省时返回原序副本', () => {
    const input = rows();
    const output = applySort(input);
    expect(output).toEqual(input);
    expect(output).not.toBe(input);
    expect(applySort(input, null)).toEqual(input);
  });

  it('比较器返回 0 的等值元素保持原序(稳定排序)', () => {
    const input = [
      { id: 1, name: 'x' },
      { id: 2, name: 'y' },
      { id: 3, name: 'z' },
    ];
    const output = applySort(input, () => 0);
    expect(output.map((r) => r.id)).toEqual([1, 2, 3]);
  });
});

describe('paginationSlice', () => {
  it('pageSize 缺省时返回全量且 pageCount = 1', () => {
    const result = paginationSlice([1, 2, 3], 1, undefined);
    expect(result.pageRows).toEqual([1, 2, 3]);
    expect(result.pageCount).toBe(1);
  });

  it('客户端模式:整除分页', () => {
    const data = Array.from({ length: 20 }, (_, i) => i);
    const first = paginationSlice(data, 1, 10);
    expect(first.pageRows).toEqual(data.slice(0, 10));
    expect(first.pageCount).toBe(2);
    const second = paginationSlice(data, 2, 10);
    expect(second.pageRows).toEqual(data.slice(10, 20));
  });

  it('客户端模式:溢出末页只返回余量', () => {
    const data = Array.from({ length: 25 }, (_, i) => i);
    const result = paginationSlice(data, 3, 10);
    expect(result.pageRows).toEqual(data.slice(20, 25));
    expect(result.pageRows).toHaveLength(5);
    expect(result.pageCount).toBe(3);
  });

  it('客户端模式:current 越界收敛到末页', () => {
    const data = Array.from({ length: 25 }, (_, i) => i);
    const result = paginationSlice(data, 99, 10);
    expect(result.pageRows).toEqual(data.slice(20, 25));
    expect(result.pageCount).toBe(3);
    const first = paginationSlice(data, 0, 10);
    expect(first.pageRows).toEqual(data.slice(0, 10));
  });

  it('服务端模式:rows 长度小于 total 时不再切片,pageCount 按 total 计', () => {
    const page = Array.from({ length: 20 }, (_, i) => i);
    const result = paginationSlice(page, 5, 20, 100);
    expect(result.pageRows).toEqual(page);
    expect(result.pageCount).toBe(5);
  });

  it('服务端模式配置异常(rows > pageSize 但 < total):按 antd 警告分支切片', () => {
    const data = Array.from({ length: 15 }, (_, i) => i);
    const result = paginationSlice(data, 2, 10, 50);
    expect(result.pageRows).toEqual(data.slice(10, 15));
    expect(result.pageCount).toBe(5);
  });

  it('total 与 rows 等长(全量已返回)时走客户端切片', () => {
    const data = Array.from({ length: 30 }, (_, i) => i);
    const result = paginationSlice(data, 2, 10, 30);
    expect(result.pageRows).toEqual(data.slice(10, 20));
    expect(result.pageCount).toBe(3);
  });
});

describe('toggleKey', () => {
  it('未选中键 → 追加', () => {
    expect(toggleKey(['a'], 'b')).toEqual(['a', 'b']);
    expect(toggleKey([], 1)).toEqual([1]);
  });

  it('已选中键 → 移除', () => {
    expect(toggleKey(['a', 'b'], 'a')).toEqual(['b']);
    expect(toggleKey([1, 2], 2)).toEqual([1]);
  });

  it('不修改入参数组', () => {
    const selected = ['a'];
    toggleKey(selected, 'b');
    expect(selected).toEqual(['a']);
  });
});

describe('toggleAll', () => {
  it('全选:追加页内键并保留跨页已选,自动去重', () => {
    const next = toggleAll(['p1', 'p2'], ['other'], true);
    expect(next).toEqual(['other', 'p1', 'p2']);
    expect(toggleAll(['p1', 'p2'], ['p1', 'x'], true)).toEqual(['p1', 'x', 'p2']);
  });

  it('全不选:仅移除页内键,跨页已选保留', () => {
    const next = toggleAll(['p1', 'p2'], ['p1', 'other', 'p2'], false);
    expect(next).toEqual(['other']);
  });
});

describe('resolveRowKey', () => {
  it('字符串 rowKey 取字段值', () => {
    expect(resolveRowKey({ id: 42 }, 0, 'id')).toBe(42);
    expect(resolveRowKey({ name: 'n' }, 0, 'name')).toBe('n');
  });

  it('函数 rowKey 收到 record 与 index', () => {
    expect(resolveRowKey({ id: 7 }, 3, (row, index) => `${row.id}-${index}`)).toBe('7-3');
    expect(resolveRowKey({ id: 7 }, 3, (row) => row.id)).toBe(7);
  });

  it('缺省 rowKey 回落 record.key,再回落 index', () => {
    expect(resolveRowKey({ key: 'k1' }, 0)).toBe('k1');
    expect(resolveRowKey({ id: 1 }, 4)).toBe(4);
  });

  it('字符串 rowKey 字段缺失时回落 key / index', () => {
    expect(resolveRowKey({ key: 'fallback' }, 2, 'missing')).toBe('fallback');
    expect(resolveRowKey({ id: 1 }, 5, 'missing')).toBe(5);
  });
});

describe('resolveDataIndex', () => {
  it('缺省 dataIndex 返回整行(antd 空路径语义)', () => {
    const record = { id: 1 };
    expect(resolveDataIndex(record)).toBe(record);
  });

  it('标量字段直取', () => {
    expect(resolveDataIndex({ name: 'n' }, 'name')).toBe('n');
  });

  it('数组路径逐段嵌套', () => {
    const record = { user: { nickname: 'nick' } };
    expect(resolveDataIndex(record, ['user', 'nickname'])).toBe('nick');
    expect(resolveDataIndex(record, ['user', 'missing'])).toBeUndefined();
    expect(resolveDataIndex(record, ['missing', 'x'])).toBeUndefined();
  });
});

describe('resolvePagination(受控/非受控判定)', () => {
  it('config 为 null:完全非受控,内部状态驱动且标记均为 false', () => {
    const result = resolvePagination({ config: null, innerCurrent: 3, innerPageSize: 20 });
    expect(result).toEqual({
      current: 3,
      pageSize: 20,
      controlledCurrent: false,
      controlledPageSize: false,
    });
  });

  it('config 缺省 current/pageSize:非受控,内部状态生效(页码按钮可点)', () => {
    const result = resolvePagination({
      config: {},
      innerCurrent: 2,
      innerPageSize: 10,
    });
    expect(result.controlledCurrent).toBe(false);
    expect(result.controlledPageSize).toBe(false);
    expect(result.current).toBe(2);
    expect(result.pageSize).toBe(10);
  });

  it('config 定义 current/pageSize:受控,prop 覆盖内部状态', () => {
    const result = resolvePagination({
      config: { current: 5, pageSize: 50 },
      innerCurrent: 1,
      innerPageSize: 10,
    });
    expect(result).toEqual({
      current: 5,
      pageSize: 50,
      controlledCurrent: true,
      controlledPageSize: true,
    });
  });

  it('字段独立判定:current 受控 + pageSize 非受控(及反向)', () => {
    const mixed = resolvePagination({
      config: { current: 4 },
      innerCurrent: 1,
      innerPageSize: 20,
    });
    expect(mixed.controlledCurrent).toBe(true);
    expect(mixed.controlledPageSize).toBe(false);
    expect(mixed.current).toBe(4);
    expect(mixed.pageSize).toBe(20);

    const flipped = resolvePagination({
      config: { pageSize: 50 },
      innerCurrent: 7,
      innerPageSize: 10,
    });
    expect(flipped.controlledCurrent).toBe(false);
    expect(flipped.controlledPageSize).toBe(true);
    expect(flipped.current).toBe(7);
    expect(flipped.pageSize).toBe(50);
  });

  it('current = 0 属于受控(以 !== undefined 判定,不做真值陷阱)', () => {
    const result = resolvePagination({
      config: { current: 0 },
      innerCurrent: 3,
      innerPageSize: 10,
    });
    expect(result.controlledCurrent).toBe(true);
    expect(result.current).toBe(0);
  });
});

describe('initialInnerPageSize(defaultPageSize 初始化)', () => {
  it('defaultPageSize 被采纳(修复 UserTable defaultPageSize:20 静默丢弃)', () => {
    expect(initialInnerPageSize({ defaultPageSize: 20 })).toBe(20);
  });

  it('显式 pageSize 优先于 defaultPageSize', () => {
    expect(initialInnerPageSize({ pageSize: 10, defaultPageSize: 20 })).toBe(10);
  });

  it('两者缺省回落 10;config 为 null/undefined 同样回落 10', () => {
    expect(initialInnerPageSize({})).toBe(10);
    expect(initialInnerPageSize(null)).toBe(10);
    expect(initialInnerPageSize(undefined)).toBe(10);
  });
});

describe('clampCurrent(数据收缩渲染期收敛)', () => {
  it('非受控 current 超出页数 → 收敛到末页(删除后 page N 不悬空)', () => {
    // 51 行 → 10/页 = 6 页,停在第 5 页;删到 25 行 = 3 页 → 收敛到 3
    expect(clampCurrent(5, 25, 10)).toBe(3);
    expect(clampCurrent(6, 25, 10)).toBe(3);
  });

  it('current 在页数范围内原样保留', () => {
    expect(clampCurrent(1, 100, 10)).toBe(1);
    expect(clampCurrent(3, 25, 10)).toBe(3);
  });

  it('越界下界与非法输入收敛到 1', () => {
    expect(clampCurrent(0, 25, 10)).toBe(1);
    expect(clampCurrent(-2, 25, 10)).toBe(1);
    expect(clampCurrent(Number.NaN, 25, 10)).toBe(1);
    expect(clampCurrent(5, 0, 10)).toBe(1);
    expect(clampCurrent(5, 25, 0)).toBe(1);
  });

  it('总页数向上取整(溢出尾页仍可达)', () => {
    expect(clampCurrent(3, 21, 10)).toBe(3);
    expect(clampCurrent(4, 21, 10)).toBe(3);
  });

  it('组合 resolvePagination:非受控 current 收敛后 page N 仍可渲染到末页', () => {
    const resolved = resolvePagination({ config: { pageSize: 10 }, innerCurrent: 5, innerPageSize: 10 });
    const current = resolved.controlledCurrent
      ? resolved.current
      : clampCurrent(resolved.current, 25, resolved.pageSize);
    expect(current).toBe(3);
    expect(paginationSlice(Array.from({ length: 25 }, (_, i) => i), current, 10).pageRows).toHaveLength(5);
  });
});

describe('resolvePageChange(Pagination 翻页/改条数纯决策)', () => {
  it('页码前进:触发 onChange,不触发 onShowSizeChange,目标页按 total 收敛', () => {
    const result = resolvePageChange({ current: 1, pageSize: 10, nextPage: 2, total: 100 });
    expect(result).toEqual({ changed: true, page: 2, sizeChanged: false });
  });

  it('同页重复点击:不触发任何回调', () => {
    expect(
      resolvePageChange({ current: 3, pageSize: 10, nextPage: 3, total: 100 }),
    ).toEqual({ changed: false, page: 3, sizeChanged: false });
  });

  it('改每页条数:onShowSizeChange 与 onChange 都触发(修复 NotificationTable)', () => {
    const result = resolvePageChange({
      current: 1,
      pageSize: 10,
      nextPage: 1,
      nextPageSize: 50,
      total: 200,
    });
    expect(result).toEqual({ changed: true, page: 1, sizeChanged: true });
  });

  it('改每页条数同时目标页越界 → 收敛后再上报', () => {
    const result = resolvePageChange({
      current: 8,
      pageSize: 10,
      nextPage: 8,
      nextPageSize: 50,
      total: 100,
    });
    // 100 条 / 50 每页 = 2 页,第 8 页收敛到 2
    expect(result).toEqual({ changed: true, page: 2, sizeChanged: true });
  });

  it('目标页越界(数据收缩)收敛到末页;与当前页相同则不触发', () => {
    const result = resolvePageChange({ current: 3, pageSize: 10, nextPage: 5, total: 25 });
    expect(result).toEqual({ changed: false, page: 3, sizeChanged: false });
    const other = resolvePageChange({ current: 1, pageSize: 10, nextPage: 5, total: 25 });
    expect(other).toEqual({ changed: true, page: 3, sizeChanged: false });
  });

  it('nextPageSize 缺省视为不变', () => {
    const result = resolvePageChange({ current: 2, pageSize: 20, nextPage: 3, total: 100 });
    expect(result.sizeChanged).toBe(false);
    expect(result.page).toBe(3);
  });
});
