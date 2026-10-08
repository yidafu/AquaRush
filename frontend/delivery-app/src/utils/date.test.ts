/**
 * todayDateString 单测(本地日期 yyyy-MM-dd 格式化)
 */
import { todayDateString } from '@/utils/date';

describe('todayDateString', () => {
  it('返回 yyyy-MM-dd 格式(与本地日期一致)', () => {
    const result = todayDateString();
    expect(result).toMatch(/^\d{4}-\d{2}-\d{2}$/);

    const now = new Date();
    const pad = (n: number) => String(n).padStart(2, '0');
    const expected = `${now.getFullYear()}-${pad(now.getMonth() + 1)}-${pad(now.getDate())}`;
    expect(result).toBe(expected);
  });
});
