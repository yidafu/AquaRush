import { isDialablePhone } from './phone';

describe('isDialablePhone(F-ORD-032 号码保护拨号判定)', () => {
  test('完整号码可拨(配送阶段服务端下发形态)', () => {
    expect(isDialablePhone('13812341234')).toBe(true);
    expect(isDialablePhone('057187654321')).toBe(true);
  });

  test('服务端脱敏串不可拨(非配送阶段形态,前 3 + **** + 后 4)', () => {
    expect(isDialablePhone('138****1234')).toBe(false);
    expect(isDialablePhone('057****4321')).toBe(false);
  });

  test('空值/空白/异常格式不可拨', () => {
    expect(isDialablePhone(undefined)).toBe(false);
    expect(isDialablePhone(null)).toBe(false);
    expect(isDialablePhone('')).toBe(false);
    expect(isDialablePhone('   ')).toBe(false);
    expect(isDialablePhone('123')).toBe(false);
    expect(isDialablePhone('12345678901234567890')).toBe(false);
    expect(isDialablePhone('138-1234-1234')).toBe(false);
  });

  test('首尾空白的完整号码仍可拨(trim 后判定)', () => {
    expect(isDialablePhone(' 13812341234 ')).toBe(true);
  });
});
