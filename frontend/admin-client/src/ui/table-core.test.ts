/**
 * table-core 单测:排序 / 分页切片(含溢出末页与服务端模式)/ 选择开关 / rowKey 解析。
 * 纯 node 环境(无 DOM),对应用例覆盖 S2.5 验收的 Table 逻辑子集。
 */
import { describe, expect, it } from '@jest/globals';
import {
  applySort,
  paginationSlice,
  resolveDataIndex,
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
